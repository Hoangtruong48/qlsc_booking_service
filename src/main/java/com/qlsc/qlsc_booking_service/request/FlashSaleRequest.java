package com.qlsc.qlsc_booking_service.request;

import lombok.Data;

import java.util.concurrent.ThreadLocalRandom;

@Data
public class FlashSaleRequest {
    Long userId;

    public void setValueUserId() {
        this.userId = ThreadLocalRandom.current().nextLong(1, 10000L);
    }
}
