package com.tsdp.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tsdp.dto.Result;
import com.tsdp.entity.Shop;
import com.tsdp.mapper.ShopMapper;
import com.tsdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tsdp.utils.CatchClient;
import com.tsdp.utils.RedisData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static com.tsdp.utils.RedisConstants.*;

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
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    //private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    @Autowired
    private ShopMapper shopMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private CatchClient catchClient;

    @Override
    public Result selectShopById(Long id) {

        //解决缓存穿透
        //Shop shop = selectCachePenetration(id);
        //Shop shop = catchClient.selectCachePenetration(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);


        //互斥锁解决缓存击穿
        //Shop shop = selectCacheBreakdown(id);
        //Shop shop = catchClient.selectCacheBreakdown(CACHE_SHOP_KEY, id, Shop.class, this::getById, 20L, TimeUnit.MINUTES);
        //Shop shop = catchClient.selectCacheBreakdown(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);
        //逻辑过期解决缓存击穿
        //Shop shop = selectCacheBreakdownUseLogicExpired(id);
        Shop shop = catchClient.selectCacheBreakdown(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);
        if (shop == null) {
            return Result.fail("店铺不存在");
        }
        return Result.ok(shop);
    }

    // 解决缓存穿透
//    private Shop selectCachePenetration(Long id) {
//        /**
//         * 缓存穿透：查询redis为空 从而查询数据库也为空 每次访问都成功
//         * 本项目使用缓存空对象解决缓存穿透
//         */
//
//        //从redis查询
//        String shopJsonInRedis = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
//        //log.info(shopJsonInRedis);
//        //查询到 返回信息
//        /**
//         * StrUtil.isNotBlank(null) // false
//         * StrUtil.isNotBlank("") // false
//         * StrUtil.isNotBlank(" \t\n") // false
//         * StrUtil.isNotBlank("abc") // true
//         */
//        //这里进行判空 结果为false有三种情况
//        if (StrUtil.isNotBlank(shopJsonInRedis)) {
//            Shop shop = JSONUtil.toBean(shopJsonInRedis, Shop.class);
//            log.info("redis命中==>{}", shop);
//            return shop;
//        }
//        //未查询到 判断是否为空对象
//        if (shopJsonInRedis != null) {
//            return null;
//        }
//
//        //查询数据库
//        Shop shop = shopMapper.selectById(id);
//        //数据库未查询到 返回错误信息 写空对象到redis 设置短期TTL
//        if (shop == null) {
//            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
//            return null;
//        }
//        //查询到 存到redis 返回信息
//        String shopJson = JSONUtil.toJsonStr(shop);
//        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, shopJson);
//        stringRedisTemplate.expire(CACHE_SHOP_KEY + id, CACHE_SHOP_TTL, TimeUnit.MINUTES);
//        log.info("redis未命中 shop==>{}", shop);
//        return shop;
//    }

//    //互斥锁解决缓存击穿
//    private Shop selectCacheBreakdown(Long id) {
//
//        /**
//         * 缓存击穿
//         * 热键（访问量很大的数据）或者重构复杂的数据失效
//         * 本方法采用互斥锁解决（利用redis的键存在特性）
//         */
//
//        //从redis查询
//        String shopJsonInRedis = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
//        //log.info(shopJsonInRedis);
//        //查询到 返回信息
//        /**
//         * StrUtil.isNotBlank(null) // false
//         * StrUtil.isNotBlank("") // false
//         * StrUtil.isNotBlank(" \t\n") // false
//         * StrUtil.isNotBlank("abc") // true
//         */
//        //这里进行判空 结果为false有三种情况
//        if (StrUtil.isNotBlank(shopJsonInRedis)) {
//            Shop shop = JSONUtil.toBean(shopJsonInRedis, Shop.class);
//            log.info("redis命中");
//            return shop;
//        }
//        //未查询到 判断是否为空对象
//        if (shopJsonInRedis != null) {
//            log.info("redis命中空对象");
//            return null;
//        }
//
//        //  尝试获得互斥锁
//        log.info("redis未命中");
//        String key = LOCK_SHOP_KEY + id;
//        Shop shop = null;
//        try {
//            boolean lock = addLock(key);
//
//            // 无法获得 等待一段时间重试
//            if (!lock) {
//                Thread.sleep(50);
//                return selectCacheBreakdown(id);
//            }
//
//            //  可以获得 查询数据库进行重构
//            //查询数据库
//            Thread.sleep(200);
//            shop = shopMapper.selectById(id);
//            //数据库未查询到 返回错误信息 写空对象到redis 设置短期TTL
//            if (shop == null) {
//                stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
//                return null;
//            }
//            //查询到 存到redis 返回信息
//            String shopJson = JSONUtil.toJsonStr(shop);
//            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, shopJson);
//            stringRedisTemplate.expire(CACHE_SHOP_KEY + id, CACHE_SHOP_TTL, TimeUnit.MINUTES);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        } finally {
//            //  释放锁
//            boolean onLock = onLock(key);
//            return shop;
//        }
//    }

//    //逻辑过期解决缓存击穿
//    private Shop selectCacheBreakdownUseLogicExpired(Long id) {
//
//        /**
//         * 缓存击穿
//         * 热键（访问量很大的数据）或者重构复杂的数据失效
//         * 本方法采用逻辑过期解决   本方法采用逻辑过期解决
//         */
//
//        //从redis查询
//        String shopJsonInRedis = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
//        //log.info(shopJsonInRedis);
//        //未命中 返回空
//        if (StrUtil.isBlank(shopJsonInRedis)) {
//            log.info("redis未命中");
//            return null;
//        }
//
//        // 命中 判断缓存数据是否逻辑过期
//        RedisData redisData = JSONUtil.toBean(shopJsonInRedis, RedisData.class);
//        Shop shop = JSONUtil.toBean((JSONObject) redisData.getData(), Shop.class);
//        LocalDateTime expireTime = redisData.getExpireTime();
//        // 未过期 返回数据
//        if (expireTime.isAfter(LocalDateTime.now())) {
//            log.info("redis命中");
//            return shop;
//        }
//        // 若已经过期
//            // 尝试获得互斥锁
//        String key=LOCK_SHOP_KEY+id;
//        boolean lock = addLock(key);
//        // 成功获得
//        if (lock) {
//            // 开启新线程进行数据重构
//            // 重构之前 再次进行判断 防止多线程导致重复重构
//            String shopJsonInRedisTemp = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
//            RedisData redisDataTemp = JSONUtil.toBean(shopJsonInRedisTemp, RedisData.class);
//            Shop shopTemp = JSONUtil.toBean((JSONObject) redisDataTemp.getData(), Shop.class);
//            LocalDateTime expireTimeTemp = redisData.getExpireTime();
//            if (expireTimeTemp.isAfter(LocalDateTime.now())) {
//                log.info("redis命中");
//                return shopTemp;
//            }
//            try {
//                CACHE_REBUILD_EXECUTOR.submit(()->{
//                    saveShop2Redis(id,20L);
//                });
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            } finally {
//                // 释放锁
//                onLock(key);
//            }
//        }
//            // 返回过期数据
//        // 获得失败 返回过期数据
//        return shop;
//    }
//

    @Override
    @Transactional
    public Result update(Shop shop) {
        //判空
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("店铺id不能为空");
        }
        //更新数据库
        LambdaQueryWrapper<Shop> queryWrapper = new LambdaQueryWrapper<Shop>();
        queryWrapper.eq(Shop::getId, id);
        shopMapper.update(shop, queryWrapper);
        //删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + id);
        return Result.ok();
    }

//    /**
//     * 添加锁
//     *
//     * @param key 利用redis的键存在实现互斥锁 的键值
//     * @return 是否上锁成功
//     * <p>
//     * private boolean addLock(String key) {
//     * Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "yes", LOCK_SHOP_TTL, TimeUnit.SECONDS);
//     * return BooleanUtil.isTrue(flag);
//     * }
//     */

//    /**
//     * 释放锁
//     *
//     * @param key 利用redis的键存在实现互斥锁 的键值
//     * @return 受否释放锁成功
//    private boolean onLock(String key) {
//        Boolean flag = stringRedisTemplate.delete(key);
//        return BooleanUtil.isTrue(flag);
//    }

    //商铺信息的预热 也可作为数据重构使用 达到复用的目的

    public void saveShop2Redis(Long id, Long plusSeconds) {
        Shop shop = shopMapper.selectById(id);
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(plusSeconds));
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisData));
    }
}
