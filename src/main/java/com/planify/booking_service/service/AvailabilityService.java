package com.planify.booking_service.service;

import com.planify.booking_service.domain.BookingStatus;
import com.planify.booking_service.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final BookingRepository bookingRepository;

    public List<UUID> findConflicts(UUID locationId, LocalDateTime start, LocalDateTime end) {
        var statuses = EnumSet.of(BookingStatus.PENDING_PAYMENT, BookingStatus.CONFIRMED);
        return bookingRepository.findConflictingBookings(locationId, start, end, statuses);
    }

    public boolean isAvailable(UUID locationId, LocalDateTime start, LocalDateTime end) {
        return findConflicts(locationId, start, end).isEmpty();
    }
}
