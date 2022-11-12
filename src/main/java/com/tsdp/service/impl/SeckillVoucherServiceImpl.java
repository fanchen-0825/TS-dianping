package com.tsdp.service.impl;

import com.tsdp.entity.SeckillVoucher;
import com.tsdp.mapper.SeckillVoucherMapper;
import com.tsdp.service.ISeckillVoucherService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 秒杀优惠券表，与优惠券是一对一关系 服务实现类
 * </p>
 *
 * @author 范大晨
 * @since 2022-11-9
 */
@Service
public class SeckillVoucherServiceImpl extends ServiceImpl<SeckillVoucherMapper, SeckillVoucher> implements ISeckillVoucherService {

}
