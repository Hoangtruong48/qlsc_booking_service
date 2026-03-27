package com.qlsc.qlsc_booking_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "order_user", schema = Booking.BookingConstant.SCHEMA)
@Entity
public class OrderUser {
    public static int STATUS_FAIL = -1;
    public static int STATUS_INIT = 0;
    public static int STATUS_SUCCESS = 1;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "user_id")
    Long userId;

    @Column(name = "status")
    Integer status;

}
