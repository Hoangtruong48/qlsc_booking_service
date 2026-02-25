package com.qlsc.qlsc_booking_service.repo.jpa;

import com.qlsc.qlsc_booking_service.entity.BlackFridaySale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BlackFridaySaleRepo extends JpaRepository<BlackFridaySale, Long> {
}
