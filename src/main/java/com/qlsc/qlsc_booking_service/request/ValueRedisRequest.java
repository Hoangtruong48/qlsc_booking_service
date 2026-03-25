package com.qlsc.qlsc_booking_service.request;

import lombok.Data;

@Data
public class ValueRedisRequest<T> {
    T value;
    String key;
    String hashKey;
    Integer type;

}
