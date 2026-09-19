package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginByPwdDTO;
import com.sky.dto.UserLoginDTO;
import com.sky.dto.UserRegisterDTO;
import com.sky.dto.UserResetPwdDTO;
import com.sky.entity.AddressBook;
import com.sky.entity.User;
import com.sky.exception.AccountNotFoundException;
import com.sky.exception.BaseException;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.AddressBookMapper;
import com.sky.mapper.UserMapper;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    public static final String WX_LOGIN = "https://api.weixin.qq.com/sns/jscode2session";

    @Autowired
    private WeChatProperties weChatProperties;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AddressBookMapper addressBookMapper;

    /**
     * 微信登录
     *
     * @param userLoginDTO
     * @return
     */
    @Override
    public User wxLogin(UserLoginDTO userLoginDTO) {
        //调用微信接口服务，获得当前微信用户的openid
        String openid = getOpenId(userLoginDTO.getCode());

        //判断openid是否为空，如果为空则表示登录失败，抛出业务异常
        if ((openid == null)) {
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }

        //判断当前用户是否为新用户
        User user = userMapper.getByOpenId(openid);

        //如果是新用户，自动完成注册
        if (user == null) {
            user = User.builder()
                    .openid(openid)
                    .createTime(LocalDateTime.now())
                    .build();
            userMapper.insert(user);
        }


        //返回这个用户对象
        return user;


    }

    /**
     * 账号密码登录（网页端点餐用）
     *
     * @param userLoginByPwdDTO
     * @return
     */
    @Override
    public User loginByPassword(UserLoginByPwdDTO userLoginByPwdDTO) {
        String username = userLoginByPwdDTO.getUsername();
        String password = userLoginByPwdDTO.getPassword();

        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }

        User user = userMapper.getByUsername(username);

        //账号不存在，或者这个账号没设过密码（微信用户），都按登录失败处理
        if (user == null || user.getPassword() == null) {
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }

        //和员工登录一样，比对 MD5
        String md5 = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!md5.equals(user.getPassword())) {
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }

        return user;
    }

    /**
     * 顾客注册。会顺手建一条默认收货地址 ——
     * 下单接口强制要 addressBookId，新用户没地址就下不了单。
     *
     * @param userRegisterDTO
     * @return
     */
    @Override
    @Transactional
    public User register(UserRegisterDTO userRegisterDTO) {
        String username = StringUtils.hasText(userRegisterDTO.getUsername())
                ? userRegisterDTO.getUsername().trim() : "";
        String password = userRegisterDTO.getPassword();
        String phone = userRegisterDTO.getPhone();
        String address = StringUtils.hasText(userRegisterDTO.getAddress())
                ? userRegisterDTO.getAddress().trim() : "";

        if (username.length() < 3 || !StringUtils.hasText(password) || password.length() < 6) {
            throw new BaseException("账号至少 3 位，密码至少 6 位");
        }
        if (!StringUtils.hasText(phone) || !phone.matches("1[3-9]\\d{9}")) {
            throw new BaseException("手机号格式不对");
        }
        if (!StringUtils.hasText(address)) {
            throw new BaseException("收货地址不能为空");
        }

        //先查一次是为了给一句人话提示；数据库上的唯一索引才是最后的兜底
        if (userMapper.getByUsername(username) != null) {
            throw new BaseException("这个账号已经被注册了");
        }

        User user = User.builder()
                .username(username)
                .password(DigestUtils.md5DigestAsHex(password.getBytes()))
                .name(userRegisterDTO.getName())
                .phone(phone)
                .createTime(LocalDateTime.now())
                .build();
        userMapper.insert(user);

        AddressBook addressBook = AddressBook.builder()
                .userId(user.getId())
                .consignee(StringUtils.hasText(userRegisterDTO.getName())
                        ? userRegisterDTO.getName() : username)
                .phone(phone)
                .sex("0")
                .detail(address)
                .label("1")
                .isDefault(1)
                .build();
        addressBookMapper.insert(addressBook);

        log.info("新顾客注册：{}（用户 id {}）", username, user.getId());
        return user;
    }

    /**
     * 重置密码。这个环境没有短信服务，改成用注册时留的手机号后四位核对身份。
     *
     * @param userResetPwdDTO
     */
    @Override
    public void resetPassword(UserResetPwdDTO userResetPwdDTO) {
        String username = StringUtils.hasText(userResetPwdDTO.getUsername())
                ? userResetPwdDTO.getUsername().trim() : "";
        String suffix = StringUtils.hasText(userResetPwdDTO.getPhoneSuffix())
                ? userResetPwdDTO.getPhoneSuffix().trim() : "";
        String newPassword = userResetPwdDTO.getNewPassword();

        if (!StringUtils.hasText(username) || !StringUtils.hasText(suffix)
                || !StringUtils.hasText(newPassword)) {
            throw new BaseException("请把信息填完整");
        }
        if (newPassword.length() < 6) {
            throw new BaseException("新密码至少 6 位");
        }

        User user = userMapper.getByUsername(username);
        if (user == null) {
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        String phone = user.getPhone();
        //对不上就统一说"不对"，别告诉对方错的是账号还是手机号
        if (phone == null || phone.length() < 4 || !phone.endsWith(suffix)) {
            throw new BaseException("账号或手机号后四位不对");
        }

        User update = User.builder()
                .id(user.getId())
                .password(DigestUtils.md5DigestAsHex(newPassword.getBytes()))
                .build();
        userMapper.updatePassword(update);

        log.info("顾客重置密码：{}（用户 id {}）", username, user.getId());
    }

    private String getOpenId(String code){
        //调用微信接口服务，获得当前微信用户的openid
        Map<String, String> map = new HashMap<>();
        map.put("appid", weChatProperties.getAppid());
        map.put("secret", weChatProperties.getSecret());
        map.put("js_code", code);
        map.put("grant_type", "authorization_code");
        String json = HttpClientUtil.doGet(WX_LOGIN, map);

        JSONObject jsonObject = JSON.parseObject(json);
        String openid = jsonObject.getString("openid");
        return openid;
    }
}
