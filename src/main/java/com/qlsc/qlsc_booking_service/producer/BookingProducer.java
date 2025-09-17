package com.qlsc.qlsc_booking_service.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class BookingProducer {

    Logger LOG = LoggerFactory.getLogger(this.getClass());
    KafkaTemplate<String, String> kafkaTemplate;
    ObjectMapper objectMapper = new ObjectMapper();

    /**
     * @param topic : topic nhận message
     * @param event : message gửi đi
     */
    public void sendEvent(String topic, Object event) {
        LOG.info(" ---> Sending message to topic {}", topic);
        try {
            String payLoad = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, payLoad);
            LOG.info("---> Sent message to topic {} successfully", topic);
        } catch (Exception ex) {
            LOG.error("Send message failed topic = {}, command = {}", topic, event);
        }
    }
}
