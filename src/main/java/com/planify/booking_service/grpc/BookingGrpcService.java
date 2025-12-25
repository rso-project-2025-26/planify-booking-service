package com.planify.booking_service.grpc;

import com.planify.booking_service.domain.Booking;
import com.planify.booking_service.repository.BookingRepository;
import com.planify.booking_service.service.AvailabilityService;
import com.planify.booking_service.service.BookingDomainService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.UUID;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class BookingGrpcService extends BookingServiceGrpc.BookingServiceImplBase {

    private final BookingDomainService bookingService;
    private final AvailabilityService availabilityService;
    private final BookingRepository bookingRepository;

    @Override
    public void checkAvailability(CheckAvailabilityRequest request, StreamObserver<CheckAvailabilityResponse> responseObserver) {
        LocalDateTime start = LocalDateTime.ofInstant(Instant.ofEpochMilli(request.getStartEpochMillis()), ZoneOffset.UTC);
        LocalDateTime end = LocalDateTime.ofInstant(Instant.ofEpochMilli(request.getEndEpochMillis()), ZoneOffset.UTC);
        var locationId = UUID.fromString(request.getLocationId());
        var conflicts = availabilityService.findConflicts(locationId, start, end);
        if (conflicts.isEmpty()) {
            log.info("Location {} is available between {} and {}", locationId, start, end);
        } else {
            log.info("Location {} is not available between {} and {}", locationId, start, end);
        }
        var resp = CheckAvailabilityResponse.newBuilder()
                .setAvailable(conflicts.isEmpty())
                .addAllConflictingBookingIds(conflicts.stream().map(UUID::toString).toList())
                .build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
        log.info("Availability check completed");
    }

    @Override
    public void createBooking(CreateBookingRequest request, StreamObserver<CreateBookingResponse> responseObserver) {
        try {
            LocalDateTime start = LocalDateTime.ofInstant(Instant.ofEpochMilli(request.getStartEpochMillis()), ZoneOffset.UTC);
            LocalDateTime end = LocalDateTime.ofInstant(Instant.ofEpochMilli(request.getEndEpochMillis()), ZoneOffset.UTC);
            var result = bookingService.createBooking(BookingDomainService.CreateBookingCommand.builder()
                    .locationId(UUID.fromString(request.getLocationId()))
                    .eventId(UUID.fromString(request.getEventId()))
                    .organizationId(UUID.fromString(request.getOrganizationId()))
                    .start(start)
                    .end(end)
                    .currency(request.getCurrency())
                    .build());

            var resp = CreateBookingResponse.newBuilder()
                    .setBookingId(result.getBookingId() == null ? "" : result.getBookingId().toString())
                    .setStatus(result.getStatus().name())
                    .setAvailable(result.isAvailable())
                    .addAllConflicts(result.getConflicts().stream().map(UUID::toString).toList())
                    .setTotalAmountCents(result.getTotalAmountCents())
                    .build();
            responseObserver.onNext(resp);
            responseObserver.onCompleted();
            log.info("Booking created");
        } catch (Exception e) {
            log.error("Error creating booking", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void cancelBooking(CancelBookingRequest request, StreamObserver<CancelBookingResponse> responseObserver) {
        Booking booking = bookingService.cancelBooking(UUID.fromString(request.getBookingId()));
        var resp = CancelBookingResponse.newBuilder()
                .setStatus(booking.getStatus().name())
                .build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
        log.info("Booking cancelled");
    }

    @Override
    public void getBooking(GetBookingRequest request, StreamObserver<GetBookingResponse> responseObserver) {
        try {
            Booking booking = bookingRepository.findById(UUID.fromString(request.getBookingId()))
                    .orElseThrow(() -> new IllegalArgumentException("Rezervacija ne obstaja"));
            var resp = GetBookingResponse.newBuilder()
                    .setBookingId(booking.getId().toString())
                    .setLocationId(booking.getLocation().toString())
                    .setEventId(booking.getEventId().toString())
                    .setOrganizationId(booking.getOrganizationId().toString())
                    .setStartEpochMillis(booking.getStartTime().atOffset(ZoneOffset.UTC).toInstant().toEpochMilli())
                    .setEndEpochMillis(booking.getEndTime().atOffset(ZoneOffset.UTC).toInstant().toEpochMilli())
                    .setStatus(booking.getStatus().name())
                    .setTotalAmountCents(booking.getTotalAmountCents())
                    .setCurrency(booking.getCurrency())
                    .build();
            responseObserver.onNext(resp);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }
}
