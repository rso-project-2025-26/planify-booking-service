package com.planify.booking_service.service;

import com.planify.booking_service.domain.BookingStatus;
import com.planify.booking_service.repository.BookingRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final BookingRepository bookingRepository;

    @Retry(name = "availabilityService")
    @CircuitBreaker(name = "availabilityService", fallbackMethod = "findConflictsFallback")
    public List<UUID> findConflicts(UUID locationId, LocalDateTime start, LocalDateTime end) {
        var statuses = EnumSet.of(BookingStatus.PENDING_PAYMENT, BookingStatus.CONFIRMED);
        return bookingRepository.findConflictingBookings(locationId, start, end, statuses);
    }

    private List<UUID> findConflictsFallback(UUID locationId, LocalDateTime start, LocalDateTime end, Exception ex) {
        log.error("Availability check failed for location {}. Cannot verify availability. Error: {}", 
            locationId, ex.getMessage());
        // Zavrnemo, če ne moremo preveriti dostopnosti lokacije
        // Preprečimo dvojno rezervacijo
        throw new RuntimeException("Booking system temporarily unavailable. Please try again later.", ex);
    }

    @Retry(name = "availabilityService")
    @CircuitBreaker(name = "availabilityService", fallbackMethod = "isAvailableFallback")
    public boolean isAvailable(UUID locationId, LocalDateTime start, LocalDateTime end) {
        return findConflicts(locationId, start, end).isEmpty();
    }

    private boolean isAvailableFallback(UUID locationId, LocalDateTime start, LocalDateTime end, Exception ex) {
        log.error("Availability check failed for location {}. Cannot verify availability. Error: {}", 
            locationId, ex.getMessage());
        // Predpostavimo, da lokacija ni na voljo
        throw new RuntimeException("Booking system temporarily unavailable. Please try again later.", ex);
    }
}
