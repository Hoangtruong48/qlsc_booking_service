package com.qlsc.qlsc_booking_service.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.qlsc.qlsc_booking_service.entity.Booking;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingRequest {
    Long userId;
    @JsonProperty(Booking.BookingConstant.COURT_ID)
    Long courtId;
    @JsonProperty(Booking.BookingConstant.COURT_NUMBER)
    Integer courtNumber;
    @JsonProperty(Booking.BookingConstant.BOOKING_DATE)
    Long bookingDate;
    @JsonProperty(Booking.BookingConstant.START_TIME)
    Integer startTime;
    @JsonProperty(Booking.BookingConstant.END_TIME)
    Integer endTime;
    @JsonProperty(Booking.BookingConstant.STATUS)
    Integer status;
    @JsonProperty(Booking.BookingConstant.PRICE)
    Double price;

    public String validate() {
        if (userId == null) {
            return "User không được để trống";
        }
        if (courtId == null) {
            return "Số sân không được để trống";
        }
        return null;
    }
}
