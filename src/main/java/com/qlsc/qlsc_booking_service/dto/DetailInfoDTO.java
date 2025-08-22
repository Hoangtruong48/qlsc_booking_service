package com.qlsc.qlsc_booking_service.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@NoArgsConstructor
public class DetailInfoDTO {
    int startTime;
    int endTime;
    List<Integer> listCourtNumber;
}
