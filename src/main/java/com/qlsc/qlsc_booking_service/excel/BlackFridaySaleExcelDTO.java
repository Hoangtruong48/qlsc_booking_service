package com.qlsc.qlsc_booking_service.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BlackFridaySaleExcelDTO {
    @ExcelProperty("Id giao dịch")
    Long id;

    @ExcelProperty("Tin nhắn")
    String msg;

    @ExcelProperty("Id người nhận")
    Long userId;
}
