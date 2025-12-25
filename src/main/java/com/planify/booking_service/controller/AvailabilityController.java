package com.planify.booking_service.controller;

import com.planify.booking_service.service.AvailabilityService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/booking")
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    @GetMapping("/{locationId}/availability")
    public ResponseEntity<CheckAvailabilityResponseDto> checkAvailability(
            @PathVariable("locationId") UUID locationId,
            @RequestParam("start") long startEpochMillis,
            @RequestParam("end") long endEpochMillis
    ) {
        // Iz ui-a dobimo čase v milisekundah -> pretvorimo jih v LocalDateTime
        long startSeconds = startEpochMillis / 1000L;
        long endSeconds = endEpochMillis / 1000L;
        LocalDateTime start = LocalDateTime.ofEpochSecond(startSeconds, 0, ZoneOffset.UTC);
        LocalDateTime end = LocalDateTime.ofEpochSecond(endSeconds, 0, ZoneOffset.UTC);
        List<UUID> conflicts = availabilityService.findConflicts(locationId, start, end);
        boolean available = conflicts.isEmpty();
        return ResponseEntity.ok(new CheckAvailabilityResponseDto(available, conflicts));
    }

    @Data
    @AllArgsConstructor
    public static class CheckAvailabilityResponseDto {
        private boolean available;
        private List<UUID> conflictingBookingIds;
    }
}
