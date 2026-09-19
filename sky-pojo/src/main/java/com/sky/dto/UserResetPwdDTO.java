package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 网页端重置密码
 * 这个环境没有短信服务，所以不做验证码，改成用手机号后四位核对身份
 */
@Data
public class UserResetPwdDTO implements Serializable {

    private String username;

    //注册时留的手机号后四位
    private String phoneSuffix;

    private String newPassword;

}
