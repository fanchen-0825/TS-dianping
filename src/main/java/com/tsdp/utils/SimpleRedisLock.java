package com.tsdp.utils;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock {

    private String name;

    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    private StringRedisTemplate stringRedisTemplate;
    private static final String LOCK_PREFIX = "lock:";
    private static final String ID_PREFIX = UUID.randomUUID() + "-";

    private static final DefaultRedisScript<Long> redisScript;

    static {
        redisScript=new DefaultRedisScript<>();
        redisScript.setLocation(new ClassPathResource("lock.lua"));
        redisScript.setResultType(Long.class);
    }

    @Override
    public boolean tryLock(long time) {

        String value = ID_PREFIX + Thread.currentThread().getId();
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(LOCK_PREFIX + name, value, time, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }

    @Override
    public void unlock() {

//        String value = ID_PREFIX + Thread.currentThread().getId();
//        String s = stringRedisTemplate.opsForValue().get(LOCK_PREFIX + name);
//        if (value.equals(s)) {
//            Boolean success = stringRedisTemplate.delete(LOCK_PREFIX + name);
//        }
        stringRedisTemplate.execute(redisScript,
                Collections.singletonList(LOCK_PREFIX + name),
                ID_PREFIX + Thread.currentThread().getId());
    }
}
