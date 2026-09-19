package com.sky.controller.user;

import com.sky.result.Result;
import com.sky.service.VoucherService;
import com.sky.vo.UserVoucherVO;
import com.sky.vo.VoucherVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("userVoucherController")
@RequestMapping("/user/voucher")
@Slf4j
@Api(tags = "C端-优惠券秒杀接口")
public class VoucherController {

    @Autowired
    private VoucherService voucherService;

    /**
     * 秒杀优惠券
     * @param voucherId 优惠券id
     * @return 秒杀订单id
     */
    @PostMapping("/seckill/{id}")
    @ApiOperation("秒杀优惠券")
    public Result<Long> seckillVoucher(@PathVariable("id") Long voucherId) {
        log.info("用户秒杀优惠券:{}", voucherId);
        Long orderId = voucherService.seckillVoucher(voucherId);
        return Result.success(orderId);
    }

    /**
     * 可抢的优惠券列表（领券中心用）
     * @return 券列表，带「当前用户是否已抢过」
     */
    @GetMapping("/list")
    @ApiOperation("查询可抢的优惠券")
    public Result<List<VoucherVO>> list() {
        return Result.success(voucherService.listAvailable());
    }

    /**
     * 我的优惠券
     * @return 我抢到的券，未使用的排在前面
     */
    @GetMapping("/my")
    @ApiOperation("查询我的优惠券")
    public Result<List<UserVoucherVO>> my() {
        return Result.success(voucherService.listMyVouchers());
    }
}
