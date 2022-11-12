package com.tsdp.interceptor;

import com.tsdp.dto.UserDTO;
import com.tsdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 登录拦截器 判断是否需要拦截
 */
@Slf4j
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //判断是否需要拦截（Localhost是否存在用户）
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            //拦截
            response.setStatus(401);
            return false;
        }
        return true;
    }
}
