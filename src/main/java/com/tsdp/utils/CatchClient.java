package com.tsdp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.tsdp.utils.RedisConstants.*;

@Component
@Slf4j
public class CatchClient {
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);
    private final StringRedisTemplate stringRedisTemplate;

    public CatchClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    //带过期时间缓存
    public void set(String key, Object value, Long time, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }

    //逻辑过期缓存
    public void setWithLogicExpired(String key, Object value, Long time, TimeUnit unit) {
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    //解决缓存穿透
    public <R, ID> R selectCachePenetration(String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback,
                                            Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        String shopJsonInRedis = stringRedisTemplate.opsForValue().get(key);
        //查询到 返回信息
        if (StrUtil.isNotBlank(shopJsonInRedis)) {
            R r = JSONUtil.toBean(shopJsonInRedis, type);
            log.info("redis命中==>{}", r);
            return r;
        }
        //未查询到 判断是否为空对象
        if (shopJsonInRedis != null) {
            return null;
        }
        //查询数据库
        R r = dbFallback.apply(id);
        //数据库未查询到 返回错误信息 写空对象到redis 设置短期TTL
        if (r == null) {
            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        //查询到 存到redis 返回信息
        String shopJson = JSONUtil.toJsonStr(r);
        stringRedisTemplate.opsForValue().set(key, shopJson);
        stringRedisTemplate.expire(key, time, unit);
        log.info("redis未命中 shop==>{}", r);
        return r;
    }

    //互斥锁解决缓存击穿
    public <R, ID> R selectCacheBreakdown(String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback,
                                          Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        //从redis查询
        String shopJsonInRedis = stringRedisTemplate.opsForValue().get(key);
        //查询到 返回信息
        //这里进行判空 结果为false有三种情况
        if (StrUtil.isNotBlank(shopJsonInRedis)) {
            R r = JSONUtil.toBean(shopJsonInRedis, type);
            //R r = com.alibaba.fastjson.JSONObject.parseObject(shopJsonInRedis, type);
            log.info("redis命中");
            return r;
        }
        //未查询到 判断是否为空对象
        if (shopJsonInRedis != null) {
            log.info("redis命中空对象");
            return null;
        }

        //  尝试获得互斥锁
        log.info("redis未命中");
        String lockKey = LOCK_SHOP_KEY + id;
        R r = null;
        try {
            boolean lock = addLock(lockKey);
            // 无法获得 等待一段时间重试
            if (!lock) {
                Thread.sleep(50);
                //递归调用
                return selectCacheBreakdown(keyPrefix, id, type, dbFallback, time, unit);
            }

            //  可以获得 查询数据库进行重构
            //查询数据库
            Thread.sleep(200);
            r = dbFallback.apply(id);
            //数据库未查询到 返回错误信息 写空对象到redis 设置短期TTL
            if (r == null) {
                stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }
            //查询到 存到redis 返回信息
            set(key, r, time, unit);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            //  释放锁
            onLock(lockKey);
        }
        return r;
    }

    //逻辑过期解决缓存击穿
    public <R, ID> R selectCacheBreakdownUseLogicExpired(String keyPrefix, ID id, Class<R> type,
                                                         Function<ID, R> dbFallback,
                                                         Long time, TimeUnit unit) {
        //从redis查询
        String key = keyPrefix + id;
        String shopJsonInRedis = stringRedisTemplate.opsForValue().get(key);
        //未命中 返回空
        if (StrUtil.isBlank(shopJsonInRedis)) {
            log.info("redis未命中");
            return null;
        }
        // 命中 判断缓存数据是否逻辑过期
        RedisData redisData = JSONUtil.toBean(shopJsonInRedis, RedisData.class);
        R r = JSONUtil.toBean((JSONObject) redisData.getData(), type);
        LocalDateTime expireTime = redisData.getExpireTime();
        // 未过期 返回数据
        if (expireTime.isAfter(LocalDateTime.now())) {
            log.info("redis命中");
            return r;
        }
        // 若已经过期
        // 尝试获得互斥锁
        String lockKey = LOCK_SHOP_KEY + id;
        boolean lock = addLock(lockKey);
        // 成功获得
        if (lock) {
            // 开启新线程进行数据重构
            // 重构之前 再次进行判断 防止多线程导致重复重构
            String shopJsonInRedisTemp = stringRedisTemplate.opsForValue().get(key);
            RedisData redisDataTemp = JSONUtil.toBean(shopJsonInRedisTemp, RedisData.class);
            R r2 = JSONUtil.toBean((JSONObject) redisDataTemp.getData(), type);
            LocalDateTime expireTimeTemp = redisData.getExpireTime();
            if (expireTimeTemp.isAfter(LocalDateTime.now())) {
                log.info("redis命中");
                return r2;
            }
            try {
                CACHE_REBUILD_EXECUTOR.submit(() -> {
                    R r1 = dbFallback.apply(id);
                    setWithLogicExpired(key, r1, time, unit);
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                // 释放锁
                onLock(lockKey);
            }
        }
        // 返回过期数据
        // 获得失败 返回过期数据
        return r;
    }

    /**
     * 添加锁
     *
     * @param key 利用redis的键存在实现互斥锁 的键值
     * @return 是否上锁成功
     */
    private boolean addLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "yes", LOCK_SHOP_TTL,
                TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    /**
     * 释放锁
     *
     * @param key 利用redis的键存在实现互斥锁 的键值
     * @return 受否释放锁成功
     */
    private boolean onLock(String key) {
        Boolean flag = stringRedisTemplate.delete(key);
        return BooleanUtil.isTrue(flag);
    }


}
