package com.qlsc.qlsc_booking_service.repo.custom;

import com.qlsc.qlsc_booking_service.entity.BlackFridaySale;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class BlackFridaySaleCustomRepo {
    private final JdbcTemplate jdbcTemplate;

//    public void insertInBatch(List<BlackFridaySale> entities) {
//        String sql = "INSERT INTO booking (user_id, msg) VALUES (?, ?)";
//
//        jdbcTemplate.batchUpdate(sql,
//                entities,
//                500, // Kích thước lô (Batch size)
//                (PreparedStatement ps, BlackFridaySale entity) -> {
//                    ps.setLong(1, entity.getUserId());
//                    ps.setString(2, entity.getMsg());
//                });
//    }



    public int insertInBatch(List<BlackFridaySale> entities) {
        String sql = "INSERT INTO black_friday_sale (user_id, msg) VALUES (?, ?)";

        // Hứng mảng 2 chiều trả về từ Spring JDBC
        int[][] updateCounts = jdbcTemplate.batchUpdate(sql,
                entities,
                500, // Kích thước lô (Batch size)
                (PreparedStatement ps, BlackFridaySale entity) -> {
                    ps.setLong(1, entity.getUserId());
                    ps.setString(2, entity.getMsg());
                });

        int successCount = 0;

        for (int[] batchResult : updateCounts) {
            for (int count : batchResult) {
                if (count > 0 || count == Statement.SUCCESS_NO_INFO) {
                    successCount++; // Tăng biến đếm lên 1 cho mỗi bản ghi thành công
                }
            }
        }

        return successCount;
    }
}
