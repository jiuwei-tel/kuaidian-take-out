package com.sky.controller.admin;

import com.sky.dto.VoucherPageQueryDTO;
import com.sky.entity.Voucher;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.VoucherService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端优惠券管理。
 * 类名和用户端的 VoucherController 一样，所以这里显式指定 bean 名，不然 Spring 会撞名。
 */
@RestController("adminVoucherController")
@RequestMapping("/admin/voucher")
@Slf4j
@Api(tags = "B端-优惠券管理接口")
public class VoucherController {

    @Autowired
    private VoucherService voucherService;

    /**
     * 分页查询
     */
    @GetMapping("/page")
    @ApiOperation("分页查询优惠券")
    public Result<PageResult> page(VoucherPageQueryDTO queryDTO) {
        log.info("优惠券分页查询：{}", queryDTO);
        return Result.success(voucherService.pageQuery(queryDTO));
    }

    /**
     * 新增优惠券
     */
    @PostMapping
    @ApiOperation("新增优惠券")
    public Result<String> add(@RequestBody Voucher voucher) {
        log.info("新增优惠券：{}", voucher);
        voucherService.addVoucher(voucher);
        return Result.success();
    }

    /**
     * 修改优惠券
     */
    @PutMapping
    @ApiOperation("修改优惠券")
    public Result<String> update(@RequestBody Voucher voucher) {
        log.info("修改优惠券：{}", voucher);
        voucherService.updateVoucher(voucher);
        return Result.success();
    }

    /**
     * 删除优惠券
     */
    @DeleteMapping
    @ApiOperation("删除优惠券")
    public Result<String> delete(@RequestParam Long id) {
        log.info("删除优惠券：{}", id);
        voucherService.deleteVoucher(id);
        return Result.success();
    }

    /**
     * 上架 / 下架
     */
    @PostMapping("/status/{status}")
    @ApiOperation("优惠券上架下架")
    public Result<String> setStatus(@PathVariable Integer status, @RequestParam Long id) {
        log.info("优惠券{}状态改为：{}", id, status);
        voucherService.setStatus(id, status);
        return Result.success();
    }
}
