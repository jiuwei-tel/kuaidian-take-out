package com.sky.controller.user;

import com.sky.constant.JwtClaimsConstant;
import com.sky.dto.UserLoginByPwdDTO;
import com.sky.dto.UserLoginDTO;
import com.sky.dto.UserRegisterDTO;
import com.sky.dto.UserResetPwdDTO;
import com.sky.entity.User;
import com.sky.properties.JwtProperties;
import com.sky.result.Result;
import com.sky.service.UserService;
import com.sky.utils.JwtUtil;
import com.sky.vo.UserLoginVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user/user")
@Api(tags = "C端用户接口")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 微信登录
     * @param userLoginDTO
     * @return
     */
    @PostMapping("/login")
    @ApiOperation("微信登录")
    public Result<UserLoginVO> login(@RequestBody UserLoginDTO userLoginDTO){
        log.info("微信登录：{}",userLoginDTO.getCode());

        //微信登陆
        User user = userService.wxLogin(userLoginDTO);

        //为微信用户生成jwt令牌

        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, user.getId());

        //生成jwt令牌
        String token = JwtUtil.createJWT(jwtProperties.getUserSecretKey(), jwtProperties.getUserTtl(), claims);

        UserLoginVO userLoginVO = UserLoginVO.builder()
                .id(user.getId())
                .openid(user.getOpenid())
                .token(token)
                .build();

        return Result.success(userLoginVO);
    }

    /**
     * 账号密码登录（网页端点餐入口）
     * @param userLoginByPwdDTO
     * @return
     */
    @PostMapping("/loginByPassword")
    @ApiOperation("账号密码登录")
    public Result<UserLoginVO> loginByPassword(@RequestBody UserLoginByPwdDTO userLoginByPwdDTO){
        log.info("账号密码登录：{}",userLoginByPwdDTO.getUsername());

        User user = userService.loginByPassword(userLoginByPwdDTO);

        //为网页端用户生成jwt令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, user.getId());

        String token = JwtUtil.createJWT(jwtProperties.getUserSecretKey(), jwtProperties.getUserTtl(), claims);

        UserLoginVO userLoginVO = UserLoginVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .token(token)
                .build();

        return Result.success(userLoginVO);
    }

    /**
     * 顾客注册（同时会建一条默认收货地址）
     * @param userRegisterDTO
     * @return
     */
    @PostMapping("/register")
    @ApiOperation("顾客注册")
    public Result register(@RequestBody UserRegisterDTO userRegisterDTO){
        log.info("顾客注册：{}",userRegisterDTO.getUsername());

        userService.register(userRegisterDTO);

        //注册完不直接发登录态，让顾客回登录页自己登一次
        return Result.success();
    }

    /**
     * 重置密码（手机号后四位核对）
     * @param userResetPwdDTO
     * @return
     */
    @PostMapping("/resetPassword")
    @ApiOperation("重置密码")
    public Result resetPassword(@RequestBody UserResetPwdDTO userResetPwdDTO){
        log.info("顾客重置密码：{}",userResetPwdDTO.getUsername());

        userService.resetPassword(userResetPwdDTO);

        return Result.success();
    }
}
