package com.tsdp.service;

import com.tsdp.dto.Result;
import com.tsdp.entity.ShopType;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 范大晨
 * @since 2022-11-9
 */
public interface IShopTypeService extends IService<ShopType> {
    Result selectList();
}
