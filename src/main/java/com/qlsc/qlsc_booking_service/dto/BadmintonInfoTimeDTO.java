package com.qlsc.qlsc_booking_service.dto;

import com.qlsc.qlsc_common.util.AppUtils;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BadmintonInfoTimeDTO {
    Integer courtId;
    Integer courtNumber;
    Integer openingTime;
    Integer closingTime;

}
