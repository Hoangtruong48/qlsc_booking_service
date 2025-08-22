package com.qlsc.qlsc_booking_service.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@NoArgsConstructor
public class CourtScheduleDTO {
    Integer courtId;
    Integer openingTime;
    Integer closingTime;
    List<InfoSlot> infoSlots = new ArrayList<>();
    @Data
    public static class InfoSlot {
        Integer courtNumber;
        List<Integer> slots;
    }
}
