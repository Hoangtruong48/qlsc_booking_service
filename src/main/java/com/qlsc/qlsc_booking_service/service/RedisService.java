package com.qlsc.qlsc_booking_service.service;

import com.qlsc.qlsc_booking_service.request.ValueRedisRequest;
import com.qlsc.qlsc_common.response.ApiResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class RedisService {
    StringRedisTemplate stringRedisTemplate;

    public ApiResponse<?> addValueToRedis(ValueRedisRequest<?> request) {
        ApiResponse<String> response = new ApiResponse<>();
        Object rawValue = request.getValue();
        if (rawValue instanceof Integer) {
            stringRedisTemplate.opsForValue().set(request.getKey(), String.valueOf(request.getValue()));
        }

        return response.setMessageSuccess("Thành công");
    }


    public ApiResponse<Object> getValueFromRedis(String key, String hashKey) {
        ApiResponse<Object> response = new ApiResponse<>();

        try {
            // 1. Kiểm tra xem Client muốn lấy từ Hash hay lấy String cơ bản
            if (hashKey != null && !hashKey.trim().isEmpty()) {

                // Trường hợp 1: Lấy dữ liệu từ Hash (opsForHash)
                Object hashValue = stringRedisTemplate.opsForHash().get(key, hashKey);

                if (hashValue == null) {
                    return response.setMessageFailed("Không tìm thấy dữ liệu cho hashKey: " + hashKey);
                }

                // Giả định ApiResponse của bạn có hàm setData()
                response.setData(hashValue);

            } else {

                // Trường hợp 2: Lấy dữ liệu String độc lập (opsForValue)
                String value = stringRedisTemplate.opsForValue().get(key);

                if (value == null) {
                    return response.setMessageFailed("Không tìm thấy dữ liệu cho key: " + key);
                }

                response.setData(value);
            }

            return response.setMessageSuccess("Lấy dữ liệu thành công");

        } catch (Exception e) {
            // Bắt lỗi đề phòng Redis server bị sập hoặc mất kết nối
            return response.setMessageFailed("Lỗi khi đọc Redis: " + e.getMessage());
        }
    }
}
