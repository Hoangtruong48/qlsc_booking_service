package com.qlsc.qlsc_booking_service.controller;

import com.qlsc.qlsc_booking_service.BookingService;
import com.qlsc.qlsc_common.response.ApiResponse;
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
    @GetMapping("/get-badminton-free")
    public ApiResponse<?> getBadmintonFree(@RequestParam List<Integer> ids, @RequestParam Long bookingDate) {
        LOG.info("Start get badminton free ids = {}", ids);
        ApiResponse<?> response = bookingService.getBadmintonFree(ids, bookingDate);
        LOG.info("End get badminton free");
        return response;
    }

}
