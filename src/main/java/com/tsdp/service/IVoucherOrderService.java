package com.tsdp.service;

import com.tsdp.dto.Result;
import com.tsdp.entity.SeckillVoucher;
import com.tsdp.entity.VoucherOrder;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 范大晨
 * @since 2022-11-9
 */
public interface IVoucherOrderService extends IService<VoucherOrder> {
    Result seckillVoucher(Long id);

    Result createVoucherOrder(SeckillVoucher seckillVoucher);
}
