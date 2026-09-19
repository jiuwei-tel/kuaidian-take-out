package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 网页端账号密码登录
 */
@Data
public class UserLoginByPwdDTO implements Serializable {

    private String username;

    private String password;

}
