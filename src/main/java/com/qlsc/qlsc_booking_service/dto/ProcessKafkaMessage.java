package com.qlsc.qlsc_booking_service.dto;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProcessKafkaMessage<T> {
    public static final int SUCCESS = HttpStatus.OK.value();
    public static final int FAILURE = HttpStatus.BAD_REQUEST.value();

    T dataSuccess;
    int status;
    String messageFailure;


}
