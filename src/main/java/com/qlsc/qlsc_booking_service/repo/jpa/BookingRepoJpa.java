package com.qlsc.qlsc_booking_service.repo.jpa;

import com.qlsc.qlsc_booking_service.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepoJpa extends JpaRepository<Booking, Long> {
    @Query(value = """
            SELECT b.court_id, b.court_number, b.start_time, b.end_time
            FROM badminton.booking b
            WHERE b.court_id IN (:courtIds)
              and b.booking_date = :bookingDate
              and b.status > 0;
            """, nativeQuery = true)
    List<Object[]> findAllCourtAvailable(@Param("courtIds") List<Integer> courtIds, @Param("bookingDate") Long bookingDate);

    @Query(value = """
    select exists (
        select 1
        from badminton.booking b
        where b.court_id = :courtId
          and b.court_number = :courtNumber
          and b.booking_date = :bookingDate
          and b.status > 0
          and (b.start_time, b.end_time) overlaps (:newStartTime, :newEndTime)
    )
    """, nativeQuery = true)
    boolean isBookingConflicted(@Param("courtId") Long courtId,
                                @Param("courtNumber") Integer courtNumber,
                                @Param("bookingDate") Long bookingDate,
                                @Param("newStartTime") Integer newStartTime,
                                @Param("newEndTime") Integer newEndTime);

}
