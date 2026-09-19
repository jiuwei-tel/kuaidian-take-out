package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 网页端顾客注册
 */
@Data
public class UserRegisterDTO implements Serializable {

    private String username;

    private String password;

    //收货人
    private String name;

    private String phone;

    //收货地址，注册时顺手建成默认地址，不然新用户下不了单
    private String address;

}
