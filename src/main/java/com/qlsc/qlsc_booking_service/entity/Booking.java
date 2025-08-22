package com.qlsc.qlsc_booking_service.entity;

import com.qlsc.qlsc_common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.FieldDefaults;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = Booking.BookingConstant.TABLE_NAME, schema = Booking.BookingConstant.SCHEMA)
@Entity
public class Booking extends BaseEntity {
    @Id
    Integer id;
    @Column(name = BookingConstant.USER_ID)
    Long userId;
    @Column(name = BookingConstant.COURT_ID)
    Long courtId;
    @Column(name = BookingConstant.COURT_NUMBER)
    Integer courtNumber;
    @Column(name = BookingConstant.BOOKING_DATE)
    Long bookingDate;
    @Column(name = BookingConstant.START_TIME)
    Integer startTime;
    @Column(name = BookingConstant.END_TIME)
    Integer endTime;
    @Column(name = BookingConstant.STATUS)
    Integer status;
    @Column(name = BookingConstant.PRICE)
    Double price;

    public static class BookingConstant {
        public static final String TABLE_NAME = "booking";
        public static final String SCHEMA = "badminton";
        public static final String USER_ID = "user_id";
        public static final String COURT_ID = "court_id";
        public static final String COURT_NUMBER = "court_number";
        public static final String BOOKING_DATE = "booking_date";
        public static final String START_TIME = "start_time";
        public static final String END_TIME = "end_time";
        public static final String STATUS = "status";
        public static final String PRICE = "price";

    }
}
