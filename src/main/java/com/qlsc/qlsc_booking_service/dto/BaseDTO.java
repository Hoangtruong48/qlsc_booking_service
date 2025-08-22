package com.qlsc.qlsc_booking_service.dto;

public interface BaseDTO<RES, DATA> {
    RES convertRawDataToDTO(DATA rawData);
}
