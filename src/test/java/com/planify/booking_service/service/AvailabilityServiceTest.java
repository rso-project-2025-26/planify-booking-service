package com.planify.booking_service.service;

import com.planify.booking_service.domain.BookingStatus;
import com.planify.booking_service.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AvailabilityService Tests")
class AvailabilityServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private AvailabilityService availabilityService;

    private UUID testLocationId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @BeforeEach
    void setUp() {
        testLocationId = UUID.randomUUID();
        startTime = LocalDateTime.now().plusDays(1);
        endTime = startTime.plusHours(2);
    }

    @Test
    @DisplayName("Should find no conflicts when location is available")
    void testFindConflicts_NoConflicts() {
        // Given
        when(bookingRepository.findConflictingBookings(
            eq(testLocationId),
            eq(startTime),
            eq(endTime),
            any()
        )).thenReturn(Collections.emptyList());

        // When
        List<UUID> conflicts = availabilityService.findConflicts(testLocationId, startTime, endTime);

        // Then
        assertThat(conflicts).isEmpty();
        verify(bookingRepository).findConflictingBookings(
            eq(testLocationId),
            eq(startTime),
            eq(endTime),
            argThat(statuses -> 
                statuses.contains(BookingStatus.PENDING_PAYMENT) && 
                statuses.contains(BookingStatus.CONFIRMED))
        );
    }

    @Test
    @DisplayName("Should find conflicts when bookings exist")
    void testFindConflicts_WithConflicts() {
        // Given
        UUID conflict1 = UUID.randomUUID();
        UUID conflict2 = UUID.randomUUID();
        UUID conflict3 = UUID.randomUUID();
        List<UUID> expectedConflicts = Arrays.asList(conflict1, conflict2, conflict3);

        when(bookingRepository.findConflictingBookings(
            eq(testLocationId),
            eq(startTime),
            eq(endTime),
            any()
        )).thenReturn(expectedConflicts);

        // When
        List<UUID> conflicts = availabilityService.findConflicts(testLocationId, startTime, endTime);

        // Then
        assertThat(conflicts).hasSize(3);
        assertThat(conflicts).containsExactlyInAnyOrder(conflict1, conflict2, conflict3);
    }

    @Test
    @DisplayName("Should only check PENDING_PAYMENT and CONFIRMED statuses")
    void testFindConflicts_OnlyChecksRelevantStatuses() {
        // Given
        when(bookingRepository.findConflictingBookings(
            eq(testLocationId),
            eq(startTime),
            eq(endTime),
            any()
        )).thenReturn(Collections.emptyList());

        // When
        availabilityService.findConflicts(testLocationId, startTime, endTime);

        // Then
        verify(bookingRepository).findConflictingBookings(
            eq(testLocationId),
            eq(startTime),
            eq(endTime),
            argThat(statuses -> {
                // Verify only PENDING_PAYMENT and CONFIRMED are checked
                return statuses.size() == 2 &&
                       statuses.contains(BookingStatus.PENDING_PAYMENT) &&
                       statuses.contains(BookingStatus.CONFIRMED) &&
                       !statuses.contains(BookingStatus.CANCELLED) &&
                       !statuses.contains(BookingStatus.FAILED);
            })
        );
    }

    @Test
    @DisplayName("Should throw exception when database is unavailable")
    void testFindConflicts_DatabaseUnavailable() {
        // Given
        when(bookingRepository.findConflictingBookings(
            eq(testLocationId),
            eq(startTime),
            eq(endTime),
            any()
        )).thenThrow(new RuntimeException("Database connection failed"));

        // When / Then
        assertThatThrownBy(() -> 
            availabilityService.findConflicts(testLocationId, startTime, endTime)
        )
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Database connection failed");
    }

    @Test
    @DisplayName("Should return true when location is available")
    void testIsAvailable_Available() {
        // Given
        when(bookingRepository.findConflictingBookings(
            eq(testLocationId),
            eq(startTime),
            eq(endTime),
            any()
        )).thenReturn(Collections.emptyList());

        // When
        boolean available = availabilityService.isAvailable(testLocationId, startTime, endTime);

        // Then
        assertThat(available).isTrue();
    }

    @Test
    @DisplayName("Should return false when location has conflicts")
    void testIsAvailable_NotAvailable() {
        // Given
        UUID conflictId = UUID.randomUUID();
        when(bookingRepository.findConflictingBookings(
            eq(testLocationId),
            eq(startTime),
            eq(endTime),
            any()
        )).thenReturn(Collections.singletonList(conflictId));

        // When
        boolean available = availabilityService.isAvailable(testLocationId, startTime, endTime);

        // Then
        assertThat(available).isFalse();
    }

    @Test
    @DisplayName("Should throw exception when isAvailable check fails")
    void testIsAvailable_CheckFails() {
        // Given
        when(bookingRepository.findConflictingBookings(
            eq(testLocationId),
            eq(startTime),
            eq(endTime),
            any()
        )).thenThrow(new RuntimeException("Database error"));

        // When / Then
        assertThatThrownBy(() -> 
            availabilityService.isAvailable(testLocationId, startTime, endTime)
        )
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Database error");
    }

    @Test
    @DisplayName("Should handle multiple conflicts correctly")
    void testFindConflicts_MultipleConflicts() {
        // Given
        List<UUID> largeConflictList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            largeConflictList.add(UUID.randomUUID());
        }

        when(bookingRepository.findConflictingBookings(
            eq(testLocationId),
            eq(startTime),
            eq(endTime),
            any()
        )).thenReturn(largeConflictList);

        // When
        List<UUID> conflicts = availabilityService.findConflicts(testLocationId, startTime, endTime);

        // Then
        assertThat(conflicts).hasSize(10);
        assertThat(conflicts).containsExactlyInAnyOrderElementsOf(largeConflictList);
    }

    @Test
    @DisplayName("Should handle edge case with same start and end time")
    void testFindConflicts_SameStartAndEndTime() {
        // Given
        LocalDateTime sameTime = LocalDateTime.now().plusDays(1);
        when(bookingRepository.findConflictingBookings(
            eq(testLocationId),
            eq(sameTime),
            eq(sameTime),
            any()
        )).thenReturn(Collections.emptyList());

        // When
        List<UUID> conflicts = availabilityService.findConflicts(testLocationId, sameTime, sameTime);

        // Then
        assertThat(conflicts).isEmpty();
        verify(bookingRepository).findConflictingBookings(
            eq(testLocationId),
            eq(sameTime),
            eq(sameTime),
            any()
        );
    }

    @Test
    @DisplayName("Should handle very long time range")
    void testFindConflicts_LongTimeRange() {
        // Given
        LocalDateTime veryLongEnd = startTime.plusDays(365); // 1 year
        UUID conflictId = UUID.randomUUID();
        when(bookingRepository.findConflictingBookings(
            eq(testLocationId),
            eq(startTime),
            eq(veryLongEnd),
            any()
        )).thenReturn(Collections.singletonList(conflictId));

        // When
        List<UUID> conflicts = availabilityService.findConflicts(testLocationId, startTime, veryLongEnd);

        // Then
        assertThat(conflicts).hasSize(1);
        assertThat(conflicts).containsExactly(conflictId);
    }

    @Test
    @DisplayName("Should propagate repository exception")
    void testFindConflicts_PropagatesException() {
        // Given
        RuntimeException dbException = new RuntimeException("Connection timeout");
        when(bookingRepository.findConflictingBookings(
            eq(testLocationId),
            eq(startTime),
            eq(endTime),
            any()
        )).thenThrow(dbException);

        // When / Then
        assertThatThrownBy(() -> 
            availabilityService.findConflicts(testLocationId, startTime, endTime)
        )
        .isInstanceOf(RuntimeException.class)
        .isSameAs(dbException);
    }
}
