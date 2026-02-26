package com.qlsc.qlsc_booking_service.repo.custom;

import com.qlsc.qlsc_booking_service.entity.BlackFridaySale;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class BlackFridaySaleCustomRepo {
    private final JdbcTemplate jdbcTemplate;
    Logger log = LoggerFactory.getLogger(this.getClass());

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

    private int insertInBatchFailure(List<String> lstInsert, String topicBookingMultiThread) {
        String sql = "INSERT INTO log_failure_kafka (topic, message_failure) VALUES (?, ?)";

        // Hứng mảng 2 chiều trả về từ Spring JDBC
        int[][] updateCounts = jdbcTemplate.batchUpdate(sql,
                lstInsert,
                500, // Kích thước lô (Batch size)
                (PreparedStatement ps, String msg) -> {
                    ps.setString(1, topicBookingMultiThread);
                    ps.setString(2, msg);
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

    @Transactional
    public void insertBatchSuccessAndFailure(List<BlackFridaySale> lstInsert, List<String> lstMessageFailure, String topicBookingMultiThread) {
        if (!CollectionUtils.isEmpty(lstInsert)) {
            int totalSuccess = insertInBatch(lstInsert);
            log.info("Đã insert thành công {}/{} bản ghi hợp lệ vào DB.",
                        totalSuccess, lstInsert.size());
        }
        if (!CollectionUtils.isEmpty(lstMessageFailure)) {
            int totalFailure = insertInBatchFailure(lstMessageFailure, topicBookingMultiThread);
            log.info("Đã insert thành công {}/{} bản ghi hợp lệ vào DB.",
                    totalFailure, lstMessageFailure.size());
        }

    }


}
