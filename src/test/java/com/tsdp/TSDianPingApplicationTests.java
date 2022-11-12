package com.tsdp;

import com.tsdp.entity.Shop;
import com.tsdp.service.impl.ShopServiceImpl;
import com.tsdp.utils.CatchClient;
import com.tsdp.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.*;

import static com.tsdp.utils.RedisConstants.CACHE_SHOP_KEY;

@SpringBootTest
class TSDianPingApplicationTests {

    @Autowired
    private RedisIdWorker redisIdWorker;
    @Autowired
    private ShopServiceImpl shopService;
    @Autowired
    private CatchClient catchClient;

    private ExecutorService es=Executors.newFixedThreadPool(300);

    @Test
    void saveTest() {
        Shop shop = shopService.getById(1L);
        //catchClient.set(CACHE_SHOP_KEY+1L,shop,10L, TimeUnit.SECONDS);
        //catchClient.addLock(LOCK_SHOP_KEY+1L);
        //catchClient.onLock(LOCK_SHOP_KEY);
        catchClient.setWithLogicExpired(CACHE_SHOP_KEY + 1L, shop, 10L, TimeUnit.SECONDS);
    }

    @Test
    void testIdWorker() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(300);

        Runnable task = () -> {
            for (int i = 0; i < 100; i++) {
                long id = redisIdWorker.nextId("order");
                System.out.println("id = " + id);
            }
            latch.countDown();
        };
        long begin = System.currentTimeMillis();
        for (int i = 0; i < 300; i++) {
            es.submit(task);
        }
        latch.await();
        long end = System.currentTimeMillis();
        System.out.println("time = " + (end - begin));
    }

}
