package com.qlsc.qlsc_booking_service.thread;

import com.qlsc.qlsc_booking_service.service.FlashSaleService;
import com.qlsc.qlsc_common.constant.RedisConstant;
import jakarta.annotation.PreDestroy;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderTimeoutWorker implements ApplicationRunner {
    RedissonClient redissonClient;
    FlashSaleService flashSaleService;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    public void run(ApplicationArguments args) throws Exception {
        executorService.submit(() -> {
            RBlockingQueue<Long> queue = redissonClient.getBlockingQueue(RedisConstant.KEY_QUEUE_FLASH_SALE);
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Long orderId = queue.take();
                    flashSaleService.checkCancelOrder(orderId);
                } catch (InterruptedException exception) {
                    log.info("Thread interrupted from redisson queue.");
                }
            }
        });
    }

    @PreDestroy
    public void destroy() {
        executorService.shutdown();
    }
}
