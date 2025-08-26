package com.qlsc.qlsc_booking_service.controller;

import com.qlsc.qlsc_booking_service.BookingService;
import com.qlsc.qlsc_booking_service.entity.Booking;
import com.qlsc.qlsc_booking_service.request.BookingRequest;
import com.qlsc.qlsc_common.response.ApiResponse;
import com.qlsc.qlsc_common.util.StringQlscUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/booking")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BookingController {
    Logger LOG = LoggerFactory.getLogger(this.getClass());
    BookingService bookingService;
    @GetMapping("/get-time-badminton")
    public ApiResponse<?> getBadmintonFree(@RequestParam List<Integer> ids, @RequestParam Long bookingDate) {
        LOG.info("Start get time badminton  ids = {}", ids);
        ApiResponse<?> response = bookingService.getBadmintonFree(ids, bookingDate);
        LOG.info("End get tine badminton");
        return response;
    }

    @PostMapping("/create-booking")
    public ApiResponse<?> createBooking(@RequestBody BookingRequest request) {
        String keyLog = StringQlscUtils.generate(6);
        LOG.info("{} Start create booking badminton  req = {}", keyLog, request);
        ApiResponse<?> response = bookingService.createBooking(keyLog, request);
        LOG.info("{} End create booking badminton  req = {}", keyLog, request);
        return response;
    }


}
