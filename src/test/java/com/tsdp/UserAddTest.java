package com.tsdp;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tsdp.dto.UserDTO;
import com.tsdp.entity.User;
import com.tsdp.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.tsdp.utils.RedisConstants.LOGIN_USER_KEY;
import static com.tsdp.utils.RedisConstants.LOGIN_USER_TTL;
import static com.tsdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

@SpringBootTest
public class UserAddTest {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void add() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader("E:\\workspace5\\TS-dianping\\src\\test\\java\\com\\tsdp\\phone.txt"));
        BufferedWriter writer = new BufferedWriter(new FileWriter("E:\\workspace5\\TS-dianping\\src\\test\\java\\com\\tsdp\\token.txt"));
        for (int i = 0; i < 1000; i++) {
            String phone;
            while ((phone = reader.readLine()) != null) {
                LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(phone != null, User::getPhone, phone);
                User user = userMapper.selectOne(wrapper);
                // 存用户信息到redis
                UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
                Map<String, Object> userDtoMap = BeanUtil.beanToMap(userDTO,new HashMap<>(),
                        CopyOptions.create().setIgnoreNullValue(true)
                                .setFieldValueEditor((fieldName,fieldValue)->fieldValue.toString()));
                String token = UUID.randomUUID(true).toString();
                String key=LOGIN_USER_KEY + token;
                writer.write(token);
                writer.newLine();
                writer.flush();
                stringRedisTemplate.opsForHash().putAll(key, userDtoMap);
                stringRedisTemplate.expire(key,LOGIN_USER_TTL, TimeUnit.SECONDS);
            }
        }
        writer.close();
        reader.close();
    }
}
