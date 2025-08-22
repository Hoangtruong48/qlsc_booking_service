package com.qlsc.qlsc_booking_service.dto;

import com.qlsc.qlsc_common.util.AppUtils;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@NoArgsConstructor
public class ScheduleTimeAvailableDTO /*implements
        BaseDTO<List<ScheduleTimeAvailableDTO>, List<Object[]>> */ {
    Integer courtId;
    Integer courtNumber;
    Integer timeStart;
    Integer timeEnd;

//    @Override
    public static List<ScheduleTimeAvailableDTO> convertRawDataToDTO(List<Object[]> rawData) {
        return rawData.stream()
                .map(x ->
                        new ScheduleTimeAvailableDTO(
                                AppUtils.parseIntNew(x[0]),
                                AppUtils.parseIntNew(x[1]),
                                AppUtils.parseIntNew(x[2]),
                                AppUtils.parseIntNew(x[3])
                        )
                ).collect(Collectors.toList());
    }
}
