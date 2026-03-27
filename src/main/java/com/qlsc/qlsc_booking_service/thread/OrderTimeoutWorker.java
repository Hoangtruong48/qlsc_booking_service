package com.qlsc.qlsc_booking_service.thread;

import com.qlsc.qlsc_booking_service.service.FlashSaleService;
import com.qlsc.qlsc_common.constant.RedisConstant;
import jakarta.annotation.PreDestroy;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
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
    public void run(ApplicationArguments args) {
        log.info("OrderTimeoutWorker is running...");

        executorService.submit(() -> {
            RBlockingQueue<Long> queue = redissonClient.getBlockingQueue(RedisConstant.KEY_QUEUE_FLASH_SALE);
            RDelayedQueue<Long> delayedQueue = redissonClient.getDelayedQueue(queue);

            log.info(" Bắt đầu đứng canh gác hòm thư: {}", RedisConstant.KEY_QUEUE_FLASH_SALE);

            while (!Thread.currentThread().isInterrupted()) {
                Long orderId = null; // Khai báo ở ngoài để catch có thể log ra được
                try {
                    // Code sẽ block (dừng lại) ở dòng này cho đến khi có đơn rớt xuống
                    orderId = queue.take();

                    // NẾU REDISSON NHẢ ĐỒ, DÒNG LOG NÀY SẼ HIỆN LÊN
                    log.info(" [BING!] Bắt được đơn hàng hết hạn: ID = {}", orderId);

                    flashSaleService.checkCancelOrder(orderId);

                    log.info(" Xử lý hủy đơn ID = {} thành công!", orderId);

                } catch (InterruptedException exception) {
                    log.warn(" Thread interrupted from redisson queue.");
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    // [TỬ HUYỆT ĐƯỢC BẢO VỆ]
                    // Nếu lỗi SQL hay bất cứ lỗi gì, nó sẽ chạy vào đây, in lỗi ra,
                    // nhưng vòng lặp while vẫn tiếp tục chạy để chộp đơn hàng tiếp theo!
                    log.error(" Lỗi nghiêm trọng khi xử lý đơn hàng ID = {}", orderId, e);
                }
            }
        });
    }

    @PreDestroy
    public void destroy() {
        log.info("🧹 Đóng luồng canh gác Redisson...");
        executorService.shutdown();
    }
}