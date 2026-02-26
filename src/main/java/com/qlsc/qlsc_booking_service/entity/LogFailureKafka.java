package com.qlsc.qlsc_booking_service.entity;

import com.qlsc.qlsc_common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "log_failure_kafka", schema = Booking.BookingConstant.SCHEMA)
@Entity
public class LogFailureKafka extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "topic")
    String topic;

    @Column(name = "message_failure", columnDefinition = "TEXT")
    String messageFailure;
}
