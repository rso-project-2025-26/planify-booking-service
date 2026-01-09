package com.planify.booking_service.repository;

import com.planify.booking_service.domain.Booking;
import com.planify.booking_service.domain.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    @Query("select b.id from Booking b where b.location.id = :locationId " +
            "and b.status in :statuses " +
            "and b.startTime < :end and b.endTime > :start")
    List<UUID> findConflictingBookings(@Param("locationId") UUID locationId,
                                       @Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end,
                                       @Param("statuses") Collection<BookingStatus> statuses);
}
