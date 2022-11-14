package com.tsdp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.tsdp.dto.Result;
import com.tsdp.dto.UserDTO;
import com.tsdp.entity.SeckillVoucher;
import com.tsdp.entity.VoucherOrder;
import com.tsdp.mapper.SeckillVoucherMapper;
import com.tsdp.mapper.VoucherMapper;
import com.tsdp.mapper.VoucherOrderMapper;
import com.tsdp.service.ISeckillVoucherService;
import com.tsdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tsdp.utils.RedisIdWorker;
import com.tsdp.utils.UserHolder;
import org.springframework.aop.framework.AopContext;
import org.springframework.aop.framework.AopProxy;
import org.springframework.aop.framework.autoproxy.AutoProxyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 范大晨
 * @since 2022-11-9
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Autowired
    private ISeckillVoucherService seckillVoucherService;

    @Autowired
    private RedisIdWorker redisIdWorker;

    @Override
    public Result seckillVoucher(Long id) {
        // 根据id查询秒杀券
        SeckillVoucher seckillVoucher = seckillVoucherService.getById(id);
        // 不存在 返回错误
        if (seckillVoucher == null) {
            return Result.fail("不要用你的技术挑战我的底线！！！");
        }
        // 存在
        // 判断秒杀是否开始
        if (seckillVoucher.getBeginTime().isAfter(LocalDateTime.now())) {
            // 未开始 返回错误
            return Result.fail("秒杀未开始");
        }
        // 开始
        // 判断秒杀是否结束
        if (!seckillVoucher.getEndTime().isAfter(LocalDateTime.now())) {
            // 已结束 返回错误
            return Result.fail("秒杀已结束");
        }
        // 未结束
        // 判断库存
        if (seckillVoucher.getStock() < 1) {
            // 库存小于1 返回错误
            return Result.fail("库存不足");
        }

        Long userId = UserHolder.getUser().getId();
        synchronized (userId.toString().intern()) {
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(seckillVoucher);
        }
    }

    @Transactional
    public Result createVoucherOrder(SeckillVoucher seckillVoucher) {
        //实现一人一单
        Long userId = UserHolder.getUser().getId();


        LambdaQueryWrapper<VoucherOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(VoucherOrder::getUserId, userId)
                .eq(VoucherOrder::getVoucherId, seckillVoucher.getVoucherId());
        int count = count(queryWrapper);
        if (count > 0) {
            return Result.fail("用户已购买 不可重复购买");
        }

        // 库存充足扣减库存
//        boolean success = seckillVoucherService.update()
//                .setSql("stock=stock-1")
//                .eq("voucher_id", seckillVoucher.getVoucherId())
//                .gt("stock",0).update();

        LambdaUpdateWrapper<SeckillVoucher> wrapper = new LambdaUpdateWrapper<>();
        Integer stock = seckillVoucher.getStock();
        wrapper.setSql("stock=stock-1")
                .eq(SeckillVoucher::getVoucherId, seckillVoucher.getVoucherId())
                .gt(SeckillVoucher::getStock, 0);
        boolean success = seckillVoucherService.update(wrapper);


        if (!success) {
            return Result.fail("库存不足");
        }
        // 生成订单
        VoucherOrder voucherOrder = new VoucherOrder();
        long nextId = redisIdWorker.nextId("order");
        voucherOrder.setId(nextId);
        voucherOrder.setVoucherId(seckillVoucher.getVoucherId());
        voucherOrder.setUserId(userId);
        boolean save = save(voucherOrder);
        // 返回成功订单id
        if (!save) {
            return Result.fail("抢购失败");
        }
        return Result.ok(nextId);

    }
}
