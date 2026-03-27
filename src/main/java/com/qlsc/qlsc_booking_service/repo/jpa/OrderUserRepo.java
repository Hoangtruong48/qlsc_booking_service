package com.qlsc.qlsc_booking_service.repo.jpa;

import com.qlsc.qlsc_booking_service.entity.OrderUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderUserRepo extends JpaRepository<OrderUser, Long> {
}
