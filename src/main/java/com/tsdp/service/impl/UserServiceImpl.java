package com.tsdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tsdp.dto.LoginFormDTO;
import com.tsdp.dto.Result;
import com.tsdp.dto.UserDTO;
import com.tsdp.entity.User;
import com.tsdp.mapper.UserMapper;
import com.tsdp.service.IUserService;
import com.tsdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.tsdp.utils.RedisConstants.*;
import static com.tsdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 范大晨
 * @since 2022-11-9
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        //校验手机号
        boolean invalid = RegexUtils.isPhoneInvalid(phone);
        //错误 返回错误
        if (!invalid) {
            return Result.fail("请输入正确的手机号");
        }
        //正确 生成验证码
        String code = RandomUtil.randomNumbers(6);
        //  存入redis
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);
        //发送验证码
        log.info("验证码发送成功，验证码为:{}", code);
        //返回ok
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //校验手机号
        String phone = loginForm.getPhone();
        boolean invalid = RegexUtils.isPhoneInvalid(phone);
        //错误 返回错误
        if (!invalid) {
            return Result.fail("请输入正确手机号码");
        }
        //正确 校验验证码
        String code = loginForm.getCode();
        //  从redis获得验证码
        String codeInRedis = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        if (codeInRedis == null || !codeInRedis.equals(code)) {
            return Result.fail("验证码错误");
        }

        //验证码正确 查询手机号是否存在数据库（是否注册）
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(phone != null, User::getPhone, phone);
        User user = userMapper.selectOne(wrapper);
        //不存在 注册
        if (user == null) {
            user = createUserWithPhone(phone);
        }
        // 存用户信息到redis
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> userDtoMap = BeanUtil.beanToMap(userDTO,new HashMap<>(),
                CopyOptions.create().setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName,fieldValue)->fieldValue.toString()));
        String token = UUID.randomUUID(true).toString();
        String key=LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(key, userDtoMap);
        stringRedisTemplate.expire(key,LOGIN_USER_TTL,TimeUnit.SECONDS);

        // 返回token令牌给前端
        //log.info("token==>{}",token);
        return Result.ok(token);
    }

    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        userMapper.insert(user);
        return user;
    }

}
