package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.entity.Voucher;
import com.sky.entity.VoucherOrder;
import com.sky.exception.VoucherBusinessException;
import com.sky.mapper.VoucherMapper;
import com.sky.mapper.VoucherOrderMapper;
import com.sky.service.VoucherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;

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
        SECKILL_SCRIPT.setScriptText(
                "local stock = redis.call('get', KEYS[1]) " +
                "if stock == false then stock = ARGV[1] end " +
                "if tonumber(stock) <= 0 then return 0 end " +
                "redis.call('decr', KEYS[1]) " +
                "return 1");
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
        if (!tryLock(lockKey)) {
            throw new VoucherBusinessException(MessageConstant.SECKILL_SYSTEM_BUSY);
        }

        String stockKey = STOCK_KEY_PREFIX + voucherId;
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
                    Collections.singletonList(stockKey),
                    String.valueOf(voucher.getStock()));
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
                    .createTime(LocalDateTime.now())
                    .build();
            voucherOrderMapper.insert(voucherOrder);

            log.info("用户{}秒杀优惠券{}成功，订单id={}", userId, voucherId, voucherOrder.getId());
            return voucherOrder.getId();
        } catch (Exception e) {
            // 补偿：Redis 已扣库存但 DB 落库失败，把 Redis 库存加回去，保持两边一致
            if (stockDeducted) {
                stringRedisTemplate.opsForValue().increment(stockKey);
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
}
