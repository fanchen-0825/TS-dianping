package com.tsdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tsdp.dto.LoginFormDTO;
import com.tsdp.dto.Result;
import com.tsdp.entity.User;

import javax.servlet.http.HttpSession;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 范大晨
 * @since 2022-11-9
 */
public interface IUserService extends IService<User> {

    /**
     * 发送验证码
     * @param phone 手机号
     * @param session session
     * @return 返回统一结果
     */
    Result sendCode(String phone, HttpSession session);

    /**
     * 登录
     * @param loginForm 前端返回的登陆相关数据
     * @param session session
     * @return 返回统一结果
     */
    Result login(LoginFormDTO loginForm, HttpSession session);
}
