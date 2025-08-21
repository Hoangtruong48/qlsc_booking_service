package com.qlsc.qlsc_booking_service.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@NoArgsConstructor
public class CourtScheduleDTO {
    Integer courtId;
    Integer courtNumber;
    Integer openingTime;
    Integer closingTime;
    List<Integer> slots;
}
