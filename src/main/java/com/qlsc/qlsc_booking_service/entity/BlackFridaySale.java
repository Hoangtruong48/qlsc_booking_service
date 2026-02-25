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
@Table(name = "black_friday_sale", schema = Booking.BookingConstant.SCHEMA)
@Entity
public class BlackFridaySale extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "msg")
    String msg;

    @Column(name = "user_id")
    Long userId;
}
