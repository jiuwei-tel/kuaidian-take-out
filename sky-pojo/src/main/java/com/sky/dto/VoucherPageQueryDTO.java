package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 管理端优惠券分页查询条件
 */
@Data
public class VoucherPageQueryDTO implements Serializable {

    //券名称，模糊匹配
    private String name;

    //状态：1上架 0下架，不传表示全部
    private Integer status;

    //页码
    private int page;

    //每页记录数
    private int pageSize;
}
