package com.planify.booking_service.service;

import com.planify.booking_service.domain.*;
import com.planify.booking_service.messaging.BookingEventProducer;
import com.planify.booking_service.repository.BookingRepository;
import com.planify.booking_service.repository.LocationRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingDomainService {

    private final LocationRepository locationRepository;
    private final BookingRepository bookingRepository;
    private final AvailabilityService availabilityService;
    private final BookingEventProducer eventProducer;

    @Value
    @Builder
    public static class CreateBookingCommand {
        UUID locationId;
        UUID eventId;
        UUID organizationId;
        LocalDateTime start;
        LocalDateTime end;
        String currency;
    }

    @Value
    @Builder
    public static class CreateBookingResult {
        UUID bookingId;
        BookingStatus status;
        boolean available;
        List<UUID> conflicts;
        int totalAmountCents;
    }

    @Transactional(readOnly = true)
    public boolean isAvailable(UUID locationId, LocalDateTime start, LocalDateTime end) {
        log.info("Checking availability for location {} between {} and {}", locationId, start, end);
        return availabilityService.isAvailable(locationId, start, end);
    }

    @Transactional
    public CreateBookingResult createBooking(CreateBookingCommand cmd) {
        log.info("Creating booking for location {} between {} and {}", cmd.getLocationId(), cmd.getStart(), cmd.getEnd());
        var conflicts = availabilityService.findConflicts(cmd.getLocationId(), cmd.getStart(), cmd.getEnd());
        if (!conflicts.isEmpty()) {
            log.info("Booking conflicts with existing bookings: {}", conflicts);
            return CreateBookingResult.builder()
                .bookingId(null)
                .status(BookingStatus.FAILED)
                .available(false)
                .conflicts(conflicts)
                .totalAmountCents(0)
                .build();
        }

        Location location = locationRepository.findById(cmd.getLocationId())
            .orElseThrow(() -> {
                log.error("Location {} not found", cmd.getLocationId());
                return new NoSuchElementException("Lokacija ne obstaja");
            });

        int hours = (int) Math.max(1, Math.ceil((double) Duration.between(cmd.getStart(), cmd.getEnd()).toMinutes() / 60.0));
        int price = location.getPricePerHourCents() * hours;

        var now = LocalDateTime.now();
        Booking booking = Booking.builder()
            .location(location)
            .eventId(cmd.getEventId())
            .organizationId(cmd.getOrganizationId())
            .startTime(cmd.getStart())
            .endTime(cmd.getEnd())
            .status(BookingStatus.PENDING_PAYMENT)
            .totalAmountCents(price)
            .currency(cmd.getCurrency())
            .createdAt(now)
            .updatedAt(now)
            .build();
        booking = bookingRepository.save(booking);

        // Kafka dogodki
        eventProducer.publishBookingCreated(Map.of(
            "bookingId", booking.getId(),
            "status", booking.getStatus().name(),
            "locationId", booking.getLocation(),
            "start", booking.getStartTime().toString(),
            "end", booking.getEndTime().toString(),
            "totalAmountCents", booking.getTotalAmountCents(),
            "currency", booking.getCurrency()
        ));

        return CreateBookingResult.builder()
            .bookingId(booking.getId())
            .status(booking.getStatus())
            .available(true)
            .conflicts(List.of())
            .totalAmountCents(price)
            .build();
    }

    @Transactional
    public Booking cancelBooking(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> {
                log.error("Booking {} not found", bookingId);
                return new NoSuchElementException("Rezervacija ne obstaja");
            });
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setUpdatedAt(LocalDateTime.now());
        bookingRepository.save(booking);
        eventProducer.publishBookingEvent(Map.of(
            "bookingId", booking.getId(),
            "status", booking.getStatus().name(),
            "type", "booking_cancelled"
        ));
        return booking;
    }
}
