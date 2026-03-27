package com.qlsc.qlsc_booking_service.service;

import com.qlsc.qlsc_booking_service.entity.OrderUser;
import com.qlsc.qlsc_booking_service.producer.BookingProducer;
import com.qlsc.qlsc_booking_service.repo.custom.OrderUserCustomRepo;
import com.qlsc.qlsc_booking_service.repo.jpa.OrderUserRepo;
import com.qlsc.qlsc_booking_service.request.FlashSaleRequest;
import com.qlsc.qlsc_booking_service.request.OrderRequest;
import com.qlsc.qlsc_common.constant.KafkaConstant;
import com.qlsc.qlsc_common.constant.RedisConstant;
import com.qlsc.qlsc_common.response.ApiResponse;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FlashSaleService {
    StringRedisTemplate stringRedisTemplate;
    DefaultRedisScript<Long> deductStockScript;
    BookingProducer bookingProducer;
    OrderUserCustomRepo orderUserCustomRepo;
    RedissonClient redissonClient;
    OrderUserRepo orderUserRepo;
    Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public FlashSaleService(StringRedisTemplate stringRedisTemplate, BookingProducer bookingProducer,
                            OrderUserCustomRepo orderUserCustomRepo, RedissonClient redissonClient,
                            OrderUserRepo orderUserRepo) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.bookingProducer = bookingProducer;
        this.orderUserCustomRepo = orderUserCustomRepo;
        this.redissonClient = redissonClient;
        this.orderUserRepo = orderUserRepo;
        this.deductStockScript = new DefaultRedisScript<>();
        this.deductStockScript.setScriptText(
                "local stock = tonumber(redis.call('get', KEYS[1])) " +
                        "if (stock and stock > 0) then " +
                        "   redis.call('decr', KEYS[1]) " + // Lập tức trừ đi 1
                        "   return 1 " +
                        "else " +
                        "   return 0 " +
                        "end"
        );
        this.deductStockScript.setResultType(Long.class);
    }
    public ApiResponse<?> buyFlashSale(FlashSaleRequest request) {
        ApiResponse<String> response = new ApiResponse<>();
        String key = RedisConstant.KEY_TOTAL_PRODUCT_FLASH_SALE;
        String topic = KafkaConstant.TOPIC_FLASH_SALE;

        Long result = stringRedisTemplate.execute(deductStockScript, Collections.singletonList(key));
        Long userId = request.getUserId();

        if (result == 1) {
            bookingProducer.sendEvent(topic, userId);
            return response.setMessageSuccess("Tạo đơn hàng thành công");
        } else {
            return response.setMessageFailed("Tạo đơn hàng thất bại vì số lượng không đủ");
        }

    }

    @Transactional
    public void processOrderBatch(List<OrderUser> lstOrderUser) {
        List<OrderUser> lst = orderUserCustomRepo.batchInsertAndReturnSafe(lstOrderUser);

        if (CollectionUtils.isEmpty(lst)) return;

        RBlockingQueue<Long> blockingQueue = redissonClient.getBlockingQueue(RedisConstant.KEY_QUEUE_FLASH_SALE);
        RDelayedQueue<Long> delayedQueue = redissonClient.getDelayedQueue(blockingQueue);

        for (OrderUser order : lst) {

            Long orderIdStr = order.getId();

            // 3. Cài 500 quả bom hẹn giờ ĐỘC LẬP cho 500 cái ID này
            delayedQueue.offer(orderIdStr, 5, TimeUnit.MINUTES);
        }

        log.info("size queue = {}", delayedQueue.size());



    }


    @Transactional
    public ApiResponse<?> acceptOrder(OrderRequest request) {
        ApiResponse<String> response = new ApiResponse<>();
        Long orderId = request.getOrderId();
        OrderUser o = orderUserRepo.findById(orderId).orElse(null);

        if (o == null) return response.setMessageFailed("Không tồn tại đơn hàng với id = " + orderId);

        o.setStatus(OrderUser.STATUS_SUCCESS);
        orderUserRepo.save(o);
        return response.setMessageSuccess("Thanh toán đơn hàng thành công");


    }

    @Transactional
    public void checkCancelOrder(Long orderId) {
        OrderUser o = orderUserRepo.findById(orderId).orElse(null);
        if (o == null || o.getStatus() == OrderUser.STATUS_SUCCESS) return;
        o.setStatus(OrderUser.STATUS_FAIL);
        orderUserRepo.save(o);

        String key = RedisConstant.KEY_TOTAL_PRODUCT_FLASH_SALE;
        stringRedisTemplate.opsForValue().increment(key);
    }
}
