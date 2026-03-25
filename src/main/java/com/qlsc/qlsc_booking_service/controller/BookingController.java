package com.qlsc.qlsc_booking_service.controller;

import com.htruong48.common_log.annotaion.TraceName;
import com.qlsc.qlsc_booking_service.service.BookingService;
import com.qlsc.qlsc_common.response.ApiResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RequestMapping("/booking")
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Log4j2
public class BookingController {
    Logger LOG = LoggerFactory.getLogger(this.getClass());
    BookingService bookingService;
    @GetMapping("/get-time-badminton")
    @TraceName("get-free-badminton")
    public ApiResponse<?> getBadmintonFree(@RequestParam List<Integer> ids, @RequestParam Long bookingDate) {
        LOG.info("Start get time badminton  ids = {}", ids);
        ApiResponse<?> response = bookingService.getBadmintonFree(ids, bookingDate);
        LOG.info("End get tine badminton");
        return response;
    }

    @PostMapping("cusor-paging-with-multi-thread-get-data")
    public ApiResponse<?> cusorPagingWithMultiThreadGetData() {
        return bookingService.cusorPagingWithMultiThreadGetData();
    }

    @PostMapping("stream-data-excel")
    public ApiResponse<?> streamDataAlibabaExcel() {
        return bookingService.streamDataAlibabaExcel();
    }



//    @PostMapping("/create-booking")
//    public ApiResponse<?> createBooking(@RequestBody BookingRequest request) {
//        String keyLog = StringQlscUtils.generate(6);
//        LOG.info("{} Start create booking badminton  req = {}", keyLog, request);
//        ApiResponse<?> response = bookingService.createBooking(keyLog, request);
//        LOG.info("{} End create booking badminton  req = {}", keyLog, request);
//        return response;
//    }


}
