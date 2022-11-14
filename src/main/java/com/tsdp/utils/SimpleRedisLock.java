package com.tsdp.utils;

import cn.hutool.core.util.BooleanUtil;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock{

    private String name;

    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    private StringRedisTemplate stringRedisTemplate;
    private final String LOCK_PREFIX="lock:";

    @Override
    public boolean tryLock(long time) {
        long value = Thread.currentThread().getId();
        String valueOf = String.valueOf(value);
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(LOCK_PREFIX + name, valueOf, time, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }

    @Override
    public boolean unlock() {
        Boolean success = stringRedisTemplate.delete(LOCK_PREFIX + name);
        return BooleanUtil.isTrue(success);
    }
}
