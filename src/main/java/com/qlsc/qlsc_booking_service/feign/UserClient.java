package com.qlsc.qlsc_booking_service.feign;


import com.qlsc.qlsc_common.response.ApiResponse;
import com.qlsc.qlsc_common.user.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(
        name = "user-service",
        url = "http://localhost:8081")
public interface UserClient {
    @GetMapping("/auth")
    ApiResponse<UserDTO> getInfoTimeCourtDetail(
            @RequestParam("id") Long id
    );
}
