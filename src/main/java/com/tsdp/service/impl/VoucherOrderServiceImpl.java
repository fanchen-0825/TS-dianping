package com.tsdp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.tsdp.dto.Result;
import com.tsdp.entity.SeckillVoucher;
import com.tsdp.entity.VoucherOrder;
import com.tsdp.mapper.VoucherOrderMapper;
import com.tsdp.service.ISeckillVoucherService;
import com.tsdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tsdp.utils.RedisIdWorker;
import com.tsdp.utils.SimpleRedisLock;
import com.tsdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.concurrent.*;

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
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Autowired
    private ISeckillVoucherService seckillVoucherService;

    @Autowired
    private RedisIdWorker redisIdWorker;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedissonClient redissonClient;
    private static final DefaultRedisScript<Long> redisScript;

    private static BlockingQueue<VoucherOrder> blockingQueue = new ArrayBlockingQueue<VoucherOrder>(1024 * 1024);
    private static ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();

    private IVoucherOrderService proxy;

    //线程任务
    @PostConstruct
    void init() {
        SECKILL_ORDER_EXECUTOR.submit(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        //获取订单信息
                        VoucherOrder order = blockingQueue.take();
                        //处理创建订单
                        doOrder(order);

                    } catch (Exception e) {
                        log.error("订单异常{}", e);
                    }
                }
            }
        });
    }

    private void doOrder(VoucherOrder voucherOrder) {
        //获取分布式锁
        Long userId = voucherOrder.getUserId();
        SimpleRedisLock lock = new SimpleRedisLock("order:" + userId, stringRedisTemplate);
        boolean tryLock = lock.tryLock(10);
        //获取失败 返回错误
        if (!tryLock) {
            log.error("获取锁失败");
            return;
        }
        //获取成功 生成订单
        try {
            //获取代理对象（事务）
            proxy.createVoucherOrder(voucherOrder);
        } catch (IllegalStateException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    static {
        redisScript = new DefaultRedisScript();
        redisScript.setLocation(new ClassPathResource("seckill.lua"));
        redisScript.setResultType(Long.class);
    }


    @Override
    public Result seckillVoucher(Long id) throws InterruptedException {
        Long userId = UserHolder.getUser().getId();
        //执行lua脚本
        Long execute = stringRedisTemplate.execute(redisScript,
                Collections.emptyList(), id.toString(), userId.toString());
        int intValue = execute.intValue();
        //判断结果是否为零
        log.info("excute结果：==>{}", intValue);
        if (intValue != 0) {
            return Result.fail(intValue == 1 ? "库存不足" : "不可重复购买");
        }
        long orderId = redisIdWorker.nextId("order");
        //获取代理对象
        proxy = (IVoucherOrderService) AopContext.currentProxy();
        // 保存到阻塞队列
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setId(new RedisIdWorker(stringRedisTemplate).nextId("order"));
        voucherOrder.setVoucherId(id);
        voucherOrder.setUserId(userId);
        blockingQueue.add(voucherOrder);
        //返回订单id
        return Result.ok(orderId);
    }

    @Transactional
    public void createVoucherOrder(VoucherOrder voucherOrder) {
        //实现一人一单
        Long userId = voucherOrder.getUserId();
        LambdaQueryWrapper<VoucherOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(VoucherOrder::getUserId, userId)
                .eq(VoucherOrder::getVoucherId, voucherOrder.getVoucherId());
        int count = count(queryWrapper);
        if (count > 0) {
            log.error("不可重复购买");
            return;
        }
        // 库存充足扣减库存
        LambdaUpdateWrapper<SeckillVoucher> wrapper = new LambdaUpdateWrapper<>();
        wrapper.setSql("stock=stock-1")
                .eq(SeckillVoucher::getVoucherId, voucherOrder.getVoucherId())
                .gt(SeckillVoucher::getStock, 0);
        boolean success = seckillVoucherService.update(wrapper);
        if (!success) {
            log.error("库存不足");
            return;
        }
        // 生成订单
        save(voucherOrder);
    }

//    @Override
//    public Result seckillVoucher(Long id) throws InterruptedException {
//        // 根据id查询秒杀券
//        SeckillVoucher seckillVoucher = seckillVoucherService.getById(id);
//        // 不存在 返回错误
//        if (seckillVoucher == null) {
//            return Result.fail("不要用你的技术挑战我的底线！！！");
//        }
//        // 存在
//        // 判断秒杀是否开始
//        if (seckillVoucher.getBeginTime().isAfter(LocalDateTime.now())) {
//            // 未开始 返回错误
//            return Result.fail("秒杀未开始");
//        }
//        // 开始
//        // 判断秒杀是否结束
//        if (!seckillVoucher.getEndTime().isAfter(LocalDateTime.now())) {
//            // 已结束 返回错误
//            return Result.fail("秒杀已结束");
//        }
//        // 未结束
//        // 判断库存
//        if (seckillVoucher.getStock() < 1) {
//            // 库存小于1 返回错误
//            return Result.fail("库存不足");
//        }
//
//        //获取分布式锁
//        Long userId = UserHolder.getUser().getId();
//        SimpleRedisLock lock = new SimpleRedisLock("order:" + userId, stringRedisTemplate);
//        boolean tryLock = lock.tryLock(10);
////        RLock lock = redissonClient.getLock("order:" + userId);
////        boolean tryLock = lock.tryLock(1,10, TimeUnit.SECONDS);
//
//        //获取失败 返回错误
//        if (!tryLock) {
//            return Result.fail("只可购买一次");
//        }
//        //获取成功 生成订单
//        try {
//            //获取代理对象（事务）
//            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
//            return proxy.createVoucherOrder(seckillVoucher);
//        } catch (IllegalStateException e) {
//            throw new RuntimeException(e);
//        } finally {
//            lock.unlock();
//        }
//    }

//    @Transactional
//    public Result createVoucherOrder(SeckillVoucher seckillVoucher) {
//        //实现一人一单
//        Long userId = UserHolder.getUser().getId();
//
//
//        LambdaQueryWrapper<VoucherOrder> queryWrapper = new LambdaQueryWrapper<>();
//        queryWrapper.eq(VoucherOrder::getUserId, userId)
//                .eq(VoucherOrder::getVoucherId, seckillVoucher.getVoucherId());
//        int count = count(queryWrapper);
//        if (count > 0) {
//            return Result.fail("用户已购买 不可重复购买");
//        }
//
//        // 库存充足扣减库存
////        boolean success = seckillVoucherService.update()
////                .setSql("stock=stock-1")
////                .eq("voucher_id", seckillVoucher.getVoucherId())
////                .gt("stock",0).update();
//
//        LambdaUpdateWrapper<SeckillVoucher> wrapper = new LambdaUpdateWrapper<>();
//        wrapper.setSql("stock=stock-1")
//                .eq(SeckillVoucher::getVoucherId, seckillVoucher.getVoucherId())
//                .gt(SeckillVoucher::getStock, 0);
//        boolean success = seckillVoucherService.update(wrapper);
//
//
//        if (!success) {
//            return Result.fail("库存不足");
//        }
//        // 生成订单
//        VoucherOrder voucherOrder = new VoucherOrder();
//        long nextId = redisIdWorker.nextId("order");
//        voucherOrder.setId(nextId);
//        voucherOrder.setVoucherId(seckillVoucher.getVoucherId());
//        voucherOrder.setUserId(userId);
//        boolean save = save(voucherOrder);
//        // 返回成功订单id
//        if (!save) {
//            return Result.fail("抢购失败");
//        }
//        return Result.ok(nextId);
//
//    }
}
