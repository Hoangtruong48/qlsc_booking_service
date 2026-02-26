package com.qlsc.qlsc_booking_service.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qlsc.qlsc_booking_service.dto.ProcessKafkaMessage;
import com.qlsc.qlsc_booking_service.entity.BlackFridaySale;
import com.qlsc.qlsc_booking_service.repo.custom.BlackFridaySaleCustomRepo;
import com.qlsc.qlsc_booking_service.service.BookingService;
import com.qlsc.qlsc_common.constant.KafkaConstant;
import com.qlsc.qlsc_common.event.TestEvent;
import com.qlsc.qlsc_common.saga.BlackFridaySaleCommand;
import com.qlsc.qlsc_common.saga.CreateBookingCommand;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BookingConsumer {
    Logger LOG = LoggerFactory.getLogger(this.getClass());
    BookingService bookingService;
    ObjectMapper objectMapper = new ObjectMapper();
    ExecutorService kafkaWorkerPool;
    BlackFridaySaleCustomRepo blackFridaySaleCustomRepo;
    private final AtomicInteger totalMessageCounter = new AtomicInteger(0);
    private final AtomicLong totalTimeCounter = new AtomicLong(0);
    private static final int TARGET_MESSAGES = 5000;
//    @KafkaListener(topics = KafkaConstant.TOPIC_BOOKING_EVENT,
//            groupId = "booking-group")
//    public void consume(
//            @Payload TestEvent event,
//            @Header("kafka_receivedPartitionId") int partition,
//            @Header("kafka_offset") long offset
//    ) {
//        LOG.info("📩 Received event: {} | partition= {} | offset={}", event, partition, offset);
//    }

    @KafkaListener(topics = KafkaConstant.TOPIC_BOOKING_COMMAND, groupId = KafkaConstant.GROUP_BOOKING)
    public void consumeBookingCommand(String payLoad) {
        try {
            CreateBookingCommand cmd = objectMapper.readValue(payLoad, CreateBookingCommand.class);
            bookingService.createBooking(cmd);
        } catch (Exception ex) {
            LOG.error("Error parsing CreateBookingCommand", ex);
        }
    }

    @KafkaListener(topics = KafkaConstant.TOPIC_BOOKING_MULTI_THREAD, groupId = "test-1", containerFactory = "batchFactory")
    public void listenBatch(List<ConsumerRecord<String, String>> records, Acknowledgment ack) {
        long startTime = System.currentTimeMillis();
        LOG.info("List listener size = {}", records.size());

        // 1. Phân phát việc Parse JSON và chuyển đổi cho các Worker Thread
        List<CompletableFuture<ProcessKafkaMessage<BlackFridaySale>>> futures = records.stream()
                .map(record -> CompletableFuture.supplyAsync(() -> {
                    try {
                        // Tự tay dùng ObjectMapper parse chuỗi JSON thành Object
                        String jsonPayload = record.value();
                        BlackFridaySaleCommand command = objectMapper.readValue(jsonPayload, BlackFridaySaleCommand.class);

//                        LOG.info("Đang xử lý userId: {}", command.getUserId());
                        BlackFridaySale b =  convertCommandToEntity(command);
                        ProcessKafkaMessage<BlackFridaySale> processKafkaMessage = new ProcessKafkaMessage<>();
                        if (b != null) {
                            processKafkaMessage.setStatus(ProcessKafkaMessage.SUCCESS);
                            processKafkaMessage.setDataSuccess(b);
                        } else {
                            processKafkaMessage.setStatus(ProcessKafkaMessage.FAILURE);
                            processKafkaMessage.setMessageFailure(jsonPayload);
                        }
                        return processKafkaMessage;

                    } catch (Exception e) {
                        LOG.error("Lỗi parse JSON tại offset {}. Nội dung: {}", record.offset(), record.value(), e);
                        ProcessKafkaMessage<BlackFridaySale> processKafkaMessage = new ProcessKafkaMessage<>();
                        processKafkaMessage.setStatus(ProcessKafkaMessage.FAILURE);
                        processKafkaMessage.setMessageFailure(record.value());
                        return processKafkaMessage;
//                        return null;
                    }
                }, kafkaWorkerPool))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        List<ProcessKafkaMessage<BlackFridaySale>> lstEntity = futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .toList();

        // 4. Batch Insert xuống DB
        if (!lstEntity.isEmpty()) {

            List<BlackFridaySale> lstInsert = lstEntity.stream()
                    .filter(x -> x.getStatus() == ProcessKafkaMessage.SUCCESS)
                    .map(ProcessKafkaMessage::getDataSuccess)
                    .filter(Objects::nonNull)
                    .toList();

            List<String> lstMessageFailure = lstEntity.stream()
                    .filter(x -> x.getStatus() == ProcessKafkaMessage.FAILURE)
                    .map(ProcessKafkaMessage::getMessageFailure)
                    .filter(Objects::nonNull)
                    .toList();

            try {
                blackFridaySaleCustomRepo.insertBatchSuccessAndFailure
                        (lstInsert, lstMessageFailure, KafkaConstant.TOPIC_BOOKING_MULTI_THREAD);
            } catch (Exception e) {
                LOG.error("Lỗi nghiêm trọng khi insert batch xuống DB!", e);
                throw e;
            }



        } else {
            LOG.warn("Không có bản ghi nào hợp lệ để insert trong lô này.");
        }

        // 5. CỰC KỲ QUAN TRỌNG: Di chuyển ack.acknowledge() XUỐNG DƯỚI CÙNG
        ack.acknowledge();

        long endTime = System.currentTimeMillis();
        long batchDuration = endTime - startTime;

        long accumulatedTime = totalTimeCounter.addAndGet(batchDuration);
        int currentTotalMessages = totalMessageCounter.addAndGet(records.size());

        if (currentTotalMessages >= TARGET_MESSAGES) {
            LOG.info("\n=======================================================");
            LOG.info("------------> KẾT QUẢ BENCHMARK HOÀN TẤT");
            LOG.info("- Tổng số messages đã xử lý : {}", currentTotalMessages);
            LOG.info("- TỔNG THỜI GIAN THỰC THI   : {} ms", accumulatedTime);
            LOG.info("=======================================================\n");

            // (Tùy chọn) Reset lại để nếu bạn bắn tiếp 5000 cái nữa thì nó đo lại từ đầu mà không cần restart app
            totalMessageCounter.set(0);
            totalTimeCounter.set(0);
        }
    }

    private BlackFridaySale convertCommandToEntity(BlackFridaySaleCommand command) {
        BlackFridaySale entity = new BlackFridaySale();
        if (command.getUserId() % 2 != 0) {
            return null;
        }
        entity.setUserId(command.getUserId());
        entity.setMsg(command.getMsg());
        return entity;
    }

    private void insertToDatabase(ConsumerRecord<String, BlackFridaySaleCommand> record) {

    }
}
