package com.qlsc.qlsc_booking_service.controller;

import com.qlsc.qlsc_booking_service.request.ValueRedisRequest;
import com.qlsc.qlsc_booking_service.service.RedisService;
import com.qlsc.qlsc_common.response.ApiResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/redis")
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Log4j2
public class RedisController {

    RedisService redisService;
    @PostMapping("")
    public ApiResponse<?> addValueToRedis(@RequestBody ValueRedisRequest<?> request) {
        return redisService.addValueToRedis(request);
    }

    @GetMapping("/redis/get")
    public ApiResponse<?> getRedisValue(
            @RequestParam String key,
            @RequestParam(required = false) String hashKey) { // hashKey có thể bỏ trống
        return redisService.getValueFromRedis(key, hashKey);
    }
}
