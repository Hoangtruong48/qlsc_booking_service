package com.qlsc.qlsc_booking_service.repo.jpa;

import com.qlsc.qlsc_booking_service.dto.DataBlackFridayDTO;
import com.qlsc.qlsc_booking_service.entity.BlackFridaySale;
import feign.Param;
import jakarta.persistence.QueryHint;
import jakarta.validation.constraints.AssertTrue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Stream;

@Repository
public interface BlackFridaySaleRepo extends JpaRepository<BlackFridaySale, Long> {

    @Query(value = "SELECT new com.qlsc.qlsc_booking_service.dto.DataBlackFridayDTO(MIN(id), MAX(id)) FROM BlackFridaySale")
    DataBlackFridayDTO getMinMaxId();

    @Query(value = "SELECT * FROM black_friday_sale WHERE id >= :minId AND id < :maxId ", nativeQuery = true)
    List<BlackFridaySale> findAllByCusorId(@Param("minId") Long minId, @Param("maxId")Long maxId);


    @QueryHints(value = {
            // Báo cho JDBC Driver: Mỗi lần hút qua mạng chỉ lấy 1000 dòng để tiết kiệm RAM
            @QueryHint(name = org.hibernate.annotations.QueryHints.FETCH_SIZE, value = "1000"),
            // Báo cho Hibernate: Không cần theo dõi sự thay đổi (Dirty Checking) của các object này
            @QueryHint(name = org.hibernate.annotations.QueryHints.READ_ONLY, value = "true")
    })
    @Query("SELECT b FROM BlackFridaySale b")
    Stream<BlackFridaySale> streamAllData();
}
