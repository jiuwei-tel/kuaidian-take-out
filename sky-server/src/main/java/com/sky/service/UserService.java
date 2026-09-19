package com.sky.service;

import com.sky.dto.UserLoginByPwdDTO;
import com.sky.dto.UserLoginDTO;
import com.sky.dto.UserRegisterDTO;
import com.sky.dto.UserResetPwdDTO;
import com.sky.entity.User;
import org.springframework.stereotype.Service;


public interface UserService {

    /**
     * 微信登录
     * @param userLoginDTO
     * @return
     */
    User wxLogin(UserLoginDTO userLoginDTO);

    /**
     * 账号密码登录（网页端点餐用）
     * @param userLoginByPwdDTO
     * @return
     */
    User loginByPassword(UserLoginByPwdDTO userLoginByPwdDTO);

    /**
     * 顾客注册，会顺手建一条默认收货地址
     * @param userRegisterDTO
     * @return
     */
    User register(UserRegisterDTO userRegisterDTO);

    /**
     * 重置密码（用手机号后四位核对）
     * @param userResetPwdDTO
     */
    void resetPassword(UserResetPwdDTO userResetPwdDTO);

}
