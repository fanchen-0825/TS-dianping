package com.tsdp.service;

import com.tsdp.dto.Result;
import com.tsdp.entity.Shop;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 范大晨
 * @since 2022-11-9
 */
public interface IShopService extends IService<Shop> {
    Result selectShopById(Long id);

    Result update(Shop shop);
}
