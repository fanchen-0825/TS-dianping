package com.tsdp.interceptor;

import cn.hutool.core.bean.BeanUtil;
import com.tsdp.dto.UserDTO;
import com.tsdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.tsdp.utils.RedisConstants.LOGIN_USER_KEY;
import static com.tsdp.utils.RedisConstants.LOGIN_USER_TTL;

/**
 * 拦截器 为已登录用户重置有效期
 */
@Slf4j
public class RefreshInterceptor implements HandlerInterceptor {

    private StringRedisTemplate stringRedisTemplate;

    public RefreshInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取请求头的token令牌 进行判空
        String token = request.getHeader("authorization");
        if (token == null) {
            return true;
        }
        //log.info("token==>{}",token);
        // 从redis获取用户信息  进行判空
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(LOGIN_USER_KEY + token);
        //log.info("entres==>{}",entries);
        //为空  拦截 返回未登录 状态码401代表未登录
        if (entries.isEmpty()) {
            log.info("未登录");
            return true;
        }

        //不为空 保存用户到LocalThread
        UserDTO userDTO = BeanUtil.fillBeanWithMap(entries, new UserDTO(), false);
        log.info("已登录");
        UserHolder.saveUser(userDTO);

        // 刷新token令牌有效期
        stringRedisTemplate.expire(LOGIN_USER_KEY+token,LOGIN_USER_TTL, TimeUnit.SECONDS);
        //放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.removeUser();
        //log.info("已销毁");
    }
}
