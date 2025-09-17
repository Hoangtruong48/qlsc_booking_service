package com.qlsc.qlsc_booking_service.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qlsc.qlsc_booking_service.service.BookingService;
import com.qlsc.qlsc_common.constant.KafkaConstant;
import com.qlsc.qlsc_common.event.TestEvent;
import com.qlsc.qlsc_common.saga.CreateBookingCommand;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BookingConsumer {
    Logger LOG = LoggerFactory.getLogger(this.getClass());
    BookingService bookingService;
    ObjectMapper objectMapper = new ObjectMapper();
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
        } catch (Exception ex){
            LOG.error("Error parsing CreateBookingCommand", ex);
        }
    }
}
