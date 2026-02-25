package com.qlsc.qlsc_booking_service.feign;

import com.qlsc.qlsc_booking_service.dto.BadmintonInfoTimeDTO;
import com.qlsc.qlsc_common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
// đưa cái này vào file yml
@FeignClient(
        name = "court-management-service",
        url = "http://localhost:8082")
public interface BadmintonCourtManagementClient {
    @GetMapping("/badminton-courts/get-info-time-court-detail")
    ApiResponse<List<BadmintonInfoTimeDTO>> getInfoTimeCourtDetail(
            @RequestParam("courtIds") List<Integer> courtIds
    );
}
