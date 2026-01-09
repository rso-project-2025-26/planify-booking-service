package com.planify.booking_service.service;

import com.planify.booking_service.domain.Booking;
import com.planify.booking_service.domain.BookingStatus;
import com.planify.booking_service.domain.Location;
import com.planify.booking_service.messaging.BookingEventProducer;
import com.planify.booking_service.repository.BookingRepository;
import com.planify.booking_service.repository.LocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingDomainService Tests")
class BookingDomainServiceTest {

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private AvailabilityService availabilityService;

    @Mock
    private BookingEventProducer eventProducer;

    @InjectMocks
    private BookingDomainService bookingDomainService;

    private UUID testLocationId;
    private UUID testEventId;
    private UUID testOrganizationId;
    private Location testLocation;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @BeforeEach
    void setUp() {
        testLocationId = UUID.randomUUID();
        testEventId = UUID.randomUUID();
        testOrganizationId = UUID.randomUUID();
        startTime = LocalDateTime.now().plusDays(1);
        endTime = startTime.plusHours(2);

        testLocation = Location.builder()
            .id(testLocationId)
            .name("Test Location")
            .address("123 Test Street")
            .capacity(100)
            .pricePerHourCents(5000) // 50 EUR per hour
            .active(true)
            .build();
    }

    @Test
    @DisplayName("Should check availability successfully")
    void testIsAvailable_Success() {
        // Given
        when(availabilityService.isAvailable(testLocationId, startTime, endTime))
            .thenReturn(true);

        // When
        boolean result = bookingDomainService.isAvailable(testLocationId, startTime, endTime);

        // Then
        assertThat(result).isTrue();
        verify(availabilityService).isAvailable(testLocationId, startTime, endTime);
    }

    @Test
    @DisplayName("Should return false when location is not available")
    void testIsAvailable_NotAvailable() {
        // Given
        when(availabilityService.isAvailable(testLocationId, startTime, endTime))
            .thenReturn(false);

        // When
        boolean result = bookingDomainService.isAvailable(testLocationId, startTime, endTime);

        // Then
        assertThat(result).isFalse();
        verify(availabilityService).isAvailable(testLocationId, startTime, endTime);
    }

    @Test
    @DisplayName("Should create booking successfully when no conflicts exist")
    void testCreateBooking_Success() {
        // Given
        BookingDomainService.CreateBookingCommand command = BookingDomainService.CreateBookingCommand.builder()
            .locationId(testLocationId)
            .eventId(testEventId)
            .organizationId(testOrganizationId)
            .start(startTime)
            .end(endTime)
            .currency("EUR")
            .build();

        when(availabilityService.findConflicts(testLocationId, startTime, endTime))
            .thenReturn(Collections.emptyList());
        when(locationRepository.findById(testLocationId))
            .thenReturn(Optional.of(testLocation));

        UUID bookingId = UUID.randomUUID();
        Booking savedBooking = Booking.builder()
            .id(bookingId)
            .location(testLocation)
            .eventId(testEventId)
            .organizationId(testOrganizationId)
            .startTime(startTime)
            .endTime(endTime)
            .status(BookingStatus.PENDING_PAYMENT)
            .totalAmountCents(10000)
            .currency("EUR")
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        when(bookingRepository.save(any(Booking.class)))
            .thenReturn(savedBooking);

        // When
        BookingDomainService.CreateBookingResult result = bookingDomainService.createBooking(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getBookingId()).isEqualTo(bookingId);
        assertThat(result.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThat(result.isAvailable()).isTrue();
        assertThat(result.getConflicts()).isEmpty();
        assertThat(result.getTotalAmountCents()).isEqualTo(10000);

        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(bookingCaptor.capture());
        
        Booking capturedBooking = bookingCaptor.getValue();
        assertThat(capturedBooking.getLocation()).isEqualTo(testLocation);
        assertThat(capturedBooking.getEventId()).isEqualTo(testEventId);
        assertThat(capturedBooking.getOrganizationId()).isEqualTo(testOrganizationId);
        assertThat(capturedBooking.getStartTime()).isEqualTo(startTime);
        assertThat(capturedBooking.getEndTime()).isEqualTo(endTime);
        assertThat(capturedBooking.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThat(capturedBooking.getCurrency()).isEqualTo("EUR");

        verify(eventProducer).publishBookingCreated(anyMap());
    }

    @Test
    @DisplayName("Should fail to create booking when conflicts exist")
    void testCreateBooking_WithConflicts() {
        // Given
        BookingDomainService.CreateBookingCommand command = BookingDomainService.CreateBookingCommand.builder()
            .locationId(testLocationId)
            .eventId(testEventId)
            .organizationId(testOrganizationId)
            .start(startTime)
            .end(endTime)
            .currency("EUR")
            .build();

        UUID conflictId1 = UUID.randomUUID();
        UUID conflictId2 = UUID.randomUUID();
        List<UUID> conflicts = Arrays.asList(conflictId1, conflictId2);

        when(availabilityService.findConflicts(testLocationId, startTime, endTime))
            .thenReturn(conflicts);

        // When
        BookingDomainService.CreateBookingResult result = bookingDomainService.createBooking(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getBookingId()).isNull();
        assertThat(result.getStatus()).isEqualTo(BookingStatus.FAILED);
        assertThat(result.isAvailable()).isFalse();
        assertThat(result.getConflicts()).containsExactlyInAnyOrder(conflictId1, conflictId2);
        assertThat(result.getTotalAmountCents()).isZero();

        verify(bookingRepository, never()).save(any());
        verify(eventProducer, never()).publishBookingCreated(anyMap());
    }

    @Test
    @DisplayName("Should throw exception when location not found")
    void testCreateBooking_LocationNotFound() {
        // Given
        BookingDomainService.CreateBookingCommand command = BookingDomainService.CreateBookingCommand.builder()
            .locationId(testLocationId)
            .eventId(testEventId)
            .organizationId(testOrganizationId)
            .start(startTime)
            .end(endTime)
            .currency("EUR")
            .build();

        when(availabilityService.findConflicts(testLocationId, startTime, endTime))
            .thenReturn(Collections.emptyList());
        when(locationRepository.findById(testLocationId))
            .thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> bookingDomainService.createBooking(command))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessageContaining("Lokacija ne obstaja");

        verify(bookingRepository, never()).save(any());
        verify(eventProducer, never()).publishBookingCreated(anyMap());
    }

    @Test
    @DisplayName("Should calculate price correctly for fractional hours")
    void testCreateBooking_PriceCalculation_FractionalHours() {
        LocalDateTime end90Min = startTime.plusMinutes(90);
        BookingDomainService.CreateBookingCommand command = BookingDomainService.CreateBookingCommand.builder()
            .locationId(testLocationId)
            .eventId(testEventId)
            .organizationId(testOrganizationId)
            .start(startTime)
            .end(end90Min)
            .currency("EUR")
            .build();

        when(availabilityService.findConflicts(testLocationId, startTime, end90Min))
            .thenReturn(Collections.emptyList());
        when(locationRepository.findById(testLocationId))
            .thenReturn(Optional.of(testLocation));

        UUID bookingId = UUID.randomUUID();
        when(bookingRepository.save(any(Booking.class)))
            .thenAnswer(invocation -> {
                Booking booking = invocation.getArgument(0);
                booking.setId(bookingId);
                return booking;
            });

        // When
        BookingDomainService.CreateBookingResult result = bookingDomainService.createBooking(command);

        assertThat(result.getTotalAmountCents()).isEqualTo(10000);
    }

    @Test
    @DisplayName("Should cancel booking successfully")
    void testCancelBooking_Success() {
        // Given
        UUID bookingId = UUID.randomUUID();
        LocalDateTime originalUpdatedAt = LocalDateTime.now().minusDays(1);
        Booking existingBooking = Booking.builder()
            .id(bookingId)
            .location(testLocation)
            .eventId(testEventId)
            .organizationId(testOrganizationId)
            .startTime(startTime)
            .endTime(endTime)
            .status(BookingStatus.CONFIRMED)
            .totalAmountCents(10000)
            .currency("EUR")
            .createdAt(LocalDateTime.now().minusDays(1))
            .updatedAt(originalUpdatedAt)
            .build();

        when(bookingRepository.findById(bookingId))
            .thenReturn(Optional.of(existingBooking));
        when(bookingRepository.save(any(Booking.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Booking result = bookingDomainService.cancelBooking(bookingId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(result.getUpdatedAt()).isNotNull();
        assertThat(result.getUpdatedAt()).isAfterOrEqualTo(originalUpdatedAt);

        verify(bookingRepository).save(existingBooking);
        verify(eventProducer).publishBookingEvent(anyMap());
    }

    @Test
    @DisplayName("Should throw exception when trying to cancel non-existent booking")
    void testCancelBooking_BookingNotFound() {
        // Given
        UUID bookingId = UUID.randomUUID();
        when(bookingRepository.findById(bookingId))
            .thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> bookingDomainService.cancelBooking(bookingId))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessageContaining("Rezervacija ne obstaja");

        verify(bookingRepository, never()).save(any());
        verify(eventProducer, never()).publishBookingEvent(anyMap());
    }

    @Test
    @DisplayName("Should propagate exception when availability check fails")
    void testIsAvailable_ThrowsExceptionOnFailure() {
        // Given
        when(availabilityService.isAvailable(testLocationId, startTime, endTime))
            .thenThrow(new RuntimeException("Database connection failed"));

        // When / Then
        assertThatThrownBy(() -> 
            bookingDomainService.isAvailable(testLocationId, startTime, endTime)
        )
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Database connection failed");
    }

    @Test
    @DisplayName("Should publish correct event payload when booking is created")
    void testCreateBooking_EventPayloadCorrect() {
        // Given
        BookingDomainService.CreateBookingCommand command = BookingDomainService.CreateBookingCommand.builder()
            .locationId(testLocationId)
            .eventId(testEventId)
            .organizationId(testOrganizationId)
            .start(startTime)
            .end(endTime)
            .currency("USD")
            .build();

        when(availabilityService.findConflicts(testLocationId, startTime, endTime))
            .thenReturn(Collections.emptyList());
        when(locationRepository.findById(testLocationId))
            .thenReturn(Optional.of(testLocation));

        UUID bookingId = UUID.randomUUID();
        when(bookingRepository.save(any(Booking.class)))
            .thenAnswer(invocation -> {
                Booking booking = invocation.getArgument(0);
                booking.setId(bookingId);
                return booking;
            });

        // When
        bookingDomainService.createBooking(command);

        // Then
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(eventProducer).publishBookingCreated(payloadCaptor.capture());

        Map<String, Object> payload = payloadCaptor.getValue();
        assertThat(payload).containsKey("bookingId");
        assertThat(payload).containsEntry("status", BookingStatus.PENDING_PAYMENT.name());
        assertThat(payload).containsEntry("currency", "USD");
        assertThat(payload).containsKey("start");
        assertThat(payload).containsKey("end");
    }

    @Test
    @DisplayName("Should publish correct event payload when booking is cancelled")
    void testCancelBooking_EventPayloadCorrect() {
        // Given
        UUID bookingId = UUID.randomUUID();
        Booking existingBooking = Booking.builder()
            .id(bookingId)
            .location(testLocation)
            .status(BookingStatus.CONFIRMED)
            .build();

        when(bookingRepository.findById(bookingId))
            .thenReturn(Optional.of(existingBooking));
        when(bookingRepository.save(any(Booking.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        bookingDomainService.cancelBooking(bookingId);

        // Then
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(eventProducer).publishBookingEvent(payloadCaptor.capture());

        Map<String, Object> payload = payloadCaptor.getValue();
        assertThat(payload).containsEntry("bookingId", bookingId);
        assertThat(payload).containsEntry("status", BookingStatus.CANCELLED.name());
        assertThat(payload).containsEntry("type", "booking_cancelled");
    }
}
