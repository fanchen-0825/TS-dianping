package com.tsdp.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tsdp.dto.Result;
import com.tsdp.entity.ShopType;
import com.tsdp.mapper.ShopTypeMapper;
import com.tsdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.tsdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 范大晨
 * @since 2022-11-9
 */
@Service
@Slf4j
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Autowired
    private ShopTypeMapper shopTypeMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result selectList() {
        //查询redis
        Long size = stringRedisTemplate.opsForList().size(CACHE_SHOPTYPE_KEY);
        List<String> range = stringRedisTemplate.opsForList().range(CACHE_SHOPTYPE_KEY,0,size);

        //存在 返回数据
        if (!range.isEmpty()){
            log.info("redis命中！！");
            List<ShopType> shops =new ArrayList<>();
            for (String s : range) {
                shops.add(JSONUtil.toBean(s,ShopType.class));
            }
            return Result.ok(shops);
        }
        //不存在 查询数据库
        LambdaQueryWrapper<ShopType> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByAsc(ShopType::getSort);
        List<ShopType> shopTypes = shopTypeMapper.selectList(queryWrapper);
        List<String> shopList=new ArrayList<>();
        for (ShopType shopType : shopTypes) {
            shopList.add(JSONUtil.toJsonStr(shopType));
        }
        //不存在 返回错误
        if (shopTypes.isEmpty()){
            return Result.fail("店铺不存在");
        }
        //存在 写入redis 返回数据
        stringRedisTemplate.opsForList().rightPushAll(CACHE_SHOPTYPE_KEY,shopList);
        stringRedisTemplate.expire(CACHE_SHOPTYPE_KEY,CACHE_SHOP_TTL, TimeUnit.MINUTES);
        log.info("redis未命中 已从数据库查询到且添加到redis");
        return Result.ok(shopTypes);
    }
}
