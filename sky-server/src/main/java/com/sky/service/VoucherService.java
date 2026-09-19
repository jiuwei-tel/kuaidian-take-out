package com.sky.service;

import com.sky.dto.VoucherPageQueryDTO;
import com.sky.entity.Voucher;
import com.sky.result.PageResult;
import com.sky.vo.UserVoucherVO;
import com.sky.vo.VoucherVO;

import java.util.List;

public interface VoucherService {

    /**
     * 秒杀优惠券
     * @param voucherId 优惠券id
     * @return 秒杀订单id
     */
    Long seckillVoucher(Long voucherId);

    /**
     * 可抢的优惠券列表（用户端领券中心）
     * @return 券列表，带「当前用户是否已抢过」
     */
    List<VoucherVO> listAvailable();

    /**
     * 当前用户抢到的券
     * @return 我的券，未使用的排前面
     */
    List<UserVoucherVO> listMyVouchers();

    /**
     * 管理端：分页查询
     * @param queryDTO 名称 / 状态 / 页码
     * @return 分页结果
     */
    PageResult pageQuery(VoucherPageQueryDTO queryDTO);

    /**
     * 管理端：新增优惠券
     * @param voucher
     */
    void addVoucher(Voucher voucher);

    /**
     * 管理端：修改优惠券
     * @param voucher
     */
    void updateVoucher(Voucher voucher);

    /**
     * 管理端：删除优惠券
     * @param id
     */
    void deleteVoucher(Long id);

    /**
     * 管理端：上架 / 下架
     * @param id
     * @param status 1上架 0下架
     */
    void setStatus(Long id, Integer status);
}
