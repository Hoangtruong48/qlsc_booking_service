package com.qlsc.qlsc_booking_service.repo.custom;

import com.qlsc.qlsc_booking_service.entity.OrderUser;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

@Repository
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class OrderUserCustomRepo {
    JdbcTemplate jdbcTemplate;

    private static final int BATCH_SIZE = 1000;

    public List<OrderUser> batchInsertAndReturnSafe(List<OrderUser> orderUsers) {
        if (CollectionUtils.isEmpty(orderUsers)) {
            return new ArrayList<>();
        }
        List<OrderUser> allSavedOrders = new ArrayList<>();

        for (int i = 0; i < orderUsers.size(); i += BATCH_SIZE) {

            // Tính toán điểm cắt (không để vượt quá size thực tế của mảng)
            int end = Math.min(orderUsers.size(), i + BATCH_SIZE);
            List<OrderUser> chunk = orderUsers.subList(i, end); // Cắt ra 1 lô 500 cái

            // Gọi hàm thực thi riêng cho lô này
            List<OrderUser> savedChunk = executeInsertChunk(chunk);
            allSavedOrders.addAll(savedChunk);
        }

        return allSavedOrders;
    }

    // Tách riêng logic build SQL ra một hàm private cho sạch code
    private List<OrderUser> executeInsertChunk(List<OrderUser> chunkToSave) {
        StringBuilder sql = new StringBuilder("INSERT INTO orders (user_id, status) VALUES ");
        List<Object> params = new ArrayList<>();

        for (int i = 0; i < chunkToSave.size(); i++) {
            OrderUser order = chunkToSave.get(i);
            sql.append("(?, ?)");

            if (i < chunkToSave.size() - 1) {
                sql.append(", ");
            }

            params.add(order.getUserId());
            params.add(order.getStatus());
        }

        sql.append(" RETURNING id, user_id, status");

        return jdbcTemplate.query(
                sql.toString(),

                // [ĐƯA LÊN TRƯỚC] Hàm RowMapper map dữ liệu
                (rs, rowNum) -> {
                    OrderUser savedOrder = new OrderUser();
                    savedOrder.setId(rs.getLong("id"));
                    savedOrder.setUserId(rs.getLong("user_id"));
                    savedOrder.setStatus(rs.getInt("status"));
                    return savedOrder;
                },

                // [ĐẨY XUỐNG CUỐI] Mảng tham số params đóng vai trò là Varargs (Object...)
                params.toArray()
        );
    }

}
