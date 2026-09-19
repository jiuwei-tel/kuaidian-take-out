package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.VoucherPageQueryDTO;
import com.sky.entity.Voucher;
import com.sky.entity.VoucherOrder;
import com.sky.exception.VoucherBusinessException;
import com.sky.mapper.VoucherMapper;
import com.sky.mapper.VoucherOrderMapper;
import com.sky.result.PageResult;
import com.sky.service.VoucherService;
import com.sky.vo.AdminVoucherVO;
import com.sky.vo.UserVoucherVO;
import com.sky.vo.VoucherVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class VoucherServiceImpl implements VoucherService {

    @Autowired
    private VoucherMapper voucherMapper;

    @Autowired
    private VoucherOrderMapper voucherOrderMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /** Redis 库存 key 前缀 */
    private static final String STOCK_KEY_PREFIX = "seckill:stock:";
    /** Redis 已购用户集合 key 前缀（一人一单）  ← 新增 */
    private static final String USER_SET_KEY_PREFIX = "seckill:users:";
    /** Redis 分布式锁 key 前缀 */
    private static final String LOCK_KEY_PREFIX = "seckill:lock:user:";
    /** 锁过期时间（秒），防止死锁 */
    private static final long LOCK_TIMEOUT_SECONDS = 10;

    /**
     * 秒杀 Lua 脚本：判断库存 -> 扣减库存，原子执行
     * KEYS[1] = 库存 key；ARGV[1] = 初始库存（key 不存在时用）
     * 返回 1 = 扣减成功；0 = 库存不足
     */
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT = new DefaultRedisScript<>();

    static {
        //注意每条语句末尾的空格不能少：这些字符串会拼成一行交给 Lua，
        //少了空格就会出现「endif」这种连在一起的非法写法。
        SECKILL_SCRIPT.setScriptText(
                //已抢过直接返回 -1
                "if redis.call('sismember', KEYS[2], ARGV[2]) == 1 then return -1 end " +
                        //库存 key 不存在时，先把 DB 里的库存写回 Redis。
                        //原来只把 ARGV[1] 赋给 Lua 局部变量、没写回，decr 就作用在一个不存在的 key 上
                        //（Redis 把不存在的 key 当 0），结果库存变成 -1，
                        //只有第一个用户能抢到，之后所有人都提示「已抢完」。
                        "if redis.call('exists', KEYS[1]) == 0 then redis.call('set', KEYS[1], ARGV[1]) end " +
                        "if tonumber(redis.call('get', KEYS[1])) <= 0 then return 0 end " +
                        "redis.call('sadd', KEYS[2], ARGV[2]) " +
                        "redis.call('decr', KEYS[1]) " +
                        "return 1"
        );
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    /**
     * 秒杀优惠券（Redis 优化版：分布式锁 + Lua 原子扣库存 + DB 兜底）
     *
     * @param voucherId 优惠券id
     * @return 秒杀订单id
     */
    @Override
    @Transactional
    public Long seckillVoucher(Long voucherId) {
        Long userId = BaseContext.getCurrentId();

        // 1. 查券 + 校验时间（挡掉不在活动期内的无效请求）
        Voucher voucher = voucherMapper.getById(voucherId);
        if (voucher == null) {
            throw new VoucherBusinessException(MessageConstant.VOUCHER_NOT_FOUND);
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(voucher.getBeginTime()) || now.isAfter(voucher.getEndTime())) {
            throw new VoucherBusinessException(MessageConstant.SECKILL_TIME_ERROR);
        }

        // 2. 分布式锁（SET NX EX，防止同一用户并发重复下单）
        String lockKey = LOCK_KEY_PREFIX + userId;
        // 尝试获取锁（SET NX EX，防止同一用户并发重复下单）
        if (!tryLock(lockKey)) {
            throw new VoucherBusinessException(MessageConstant.SECKILL_SYSTEM_BUSY);
        }

        String stockKey = STOCK_KEY_PREFIX + voucherId;
        String userSetKey = USER_SET_KEY_PREFIX + voucherId;   // ← 新增
        boolean stockDeducted = false;
        try {
            // 3. 一人一单 DB 检查（快速拒绝已抢过的用户）
            int count = voucherOrderMapper.countByVoucherIdAndUserId(voucherId, userId);
            if (count > 0) {
                throw new VoucherBusinessException(MessageConstant.VOUCHER_ALREADY_GOT);
            }

            // 4. Lua 原子扣 Redis 库存（判断+扣减一步完成，挡掉高并发流量，防超卖）
            Long result = stringRedisTemplate.execute(
                    SECKILL_SCRIPT,
                    Arrays.asList(stockKey, userSetKey),
                    String.valueOf(voucher.getStock()),
                    String.valueOf(userId)
            );
            if (result == null || result == 0) {
                throw new VoucherBusinessException(MessageConstant.VOUCHER_STOCK_NOT_ENOUGH);
            }
            stockDeducted = true;

            // 5. DB 乐观锁扣库存 + 插订单（持久化 + 最终兜底）
            int rows = voucherMapper.deductStock(voucherId);
            if (rows == 0) {
                throw new VoucherBusinessException(MessageConstant.VOUCHER_STOCK_NOT_ENOUGH);
            }
            VoucherOrder voucherOrder = VoucherOrder.builder()
                    .voucherId(voucherId)
                    .userId(userId)
                    .status(VoucherOrder.UNUSED)
                    .createTime(LocalDateTime.now())
                    .build();
            voucherOrderMapper.insert(voucherOrder);

            log.info("用户{}秒杀优惠券{}成功，订单id={}", userId, voucherId, voucherOrder.getId());
            return voucherOrder.getId();
        } catch (Exception e) {
            // 补偿：Redis 已扣库存但 DB 落库失败，把 Redis 库存加回去，保持两边一致
            if (stockDeducted) {
                stringRedisTemplate.opsForValue().increment(stockKey);
                stringRedisTemplate.opsForSet().remove(userSetKey, String.valueOf(userId));  // ← 新增
            }
            throw e;
        } finally {
            // 6. 释放锁
            unlock(lockKey);
        }
    }

    /**
     * 加锁：SET key value NX EX timeout
     * @param key 锁key
     * @return true=抢到锁，false=没抢到
     */
    private boolean tryLock(String key) {
        Boolean ok = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, "1", Duration.ofSeconds(LOCK_TIMEOUT_SECONDS));
        return Boolean.TRUE.equals(ok);
    }

    /**
     * 释放锁
     * @param key 锁key
     */
    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }

    /**
     * 可抢的优惠券（领券中心）
     * 一次查出该用户抢过的券id，避免每张券都去查一次库
     */
    @Override
    public List<VoucherVO> listAvailable() {
        Long userId = BaseContext.getCurrentId();
        List<Voucher> vouchers = voucherMapper.listAvailable();
        Set<Long> gotIds = new HashSet<>(voucherOrderMapper.listVoucherIdsByUserId(userId));
        return vouchers.stream()
                .map(v -> VoucherVO.builder()
                        .id(v.getId())
                        .name(v.getName())
                        .value(v.getValue())
                        .minAmount(v.getMinAmount())
                        .stock(v.getStock())
                        .status(v.getStatus())
                        .beginTime(v.getBeginTime())
                        .endTime(v.getEndTime())
                        .got(gotIds.contains(v.getId()))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 我的优惠券，未使用的排前面
     */
    @Override
    public List<UserVoucherVO> listMyVouchers() {
        Long userId = BaseContext.getCurrentId();
        List<UserVoucherVO> list = voucherOrderMapper.listByUserId(userId);
        LocalDateTime now = LocalDateTime.now();
        list.forEach(v -> v.setExpired(v.getEndTime() != null && now.isAfter(v.getEndTime())));
        return list;
    }

    /* ---------------- 管理端 ---------------- */

    /**
     * 分页查询
     */
    @Override
    public PageResult pageQuery(VoucherPageQueryDTO queryDTO) {
        PageHelper.startPage(queryDTO.getPage(), queryDTO.getPageSize());
        Page<AdminVoucherVO> page = voucherMapper.pageQuery(queryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 新增优惠券
     */
    @Override
    public void addVoucher(Voucher voucher) {
        //不传状态就默认上架
        if (voucher.getStatus() == null) {
            voucher.setStatus(1);
        }
        LocalDateTime now = LocalDateTime.now();
        voucher.setCreateTime(now);
        voucher.setUpdateTime(now);
        voucherMapper.insert(voucher);
    }

    /**
     * 修改优惠券
     */
    @Override
    public void updateVoucher(Voucher voucher) {
        voucherMapper.update(voucher);
        //库存可能被改了，Redis 里那份缓存就过时了
        clearStockCache(voucher.getId());
    }

    /**
     * 删除优惠券
     */
    @Override
    public void deleteVoucher(Long id) {
        voucherMapper.deleteById(id);
        clearStockCache(id);
    }

    /**
     * 上架 / 下架
     */
    @Override
    public void setStatus(Long id, Integer status) {
        voucherMapper.updateStatus(id, status);
        clearStockCache(id);
    }

    /**
     * 清掉 Redis 里的库存缓存。
     * 秒杀脚本只在 key 不存在时才会拿 DB 的库存做初始值，
     * 所以管理端一改库存，就必须把这个 key 删掉，不然用的还是改之前那个数字。
     * 「已购用户集合」不清 —— 用户确实抢过了，清掉就等于允许他再抢一次。
     */
    private void clearStockCache(Long voucherId) {
        if (voucherId != null) {
            stringRedisTemplate.delete(STOCK_KEY_PREFIX + voucherId);
        }
    }
}
