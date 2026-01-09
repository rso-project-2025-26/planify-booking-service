package com.planify.booking_service.controller;

import com.planify.booking_service.service.AvailabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Availability", description = "Location availability checking endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    @Operation(
        summary = "Check location availability",
        description = "Checks if a location is available for booking in the given time window. Returns availability status and conflicting booking IDs. Times are in UTC epoch milliseconds."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Availability checked successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid parameters", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
    })
    @GetMapping("/{locationId}/availability")
    public ResponseEntity<CheckAvailabilityResponseDto> checkAvailability(
            @Parameter(required = true)
            @PathVariable("locationId") UUID locationId,
            @Parameter(required = true, example = "1735036800000")
            @RequestParam("start") long startEpochMillis,
            @Parameter(required = true, example = "1735040400000")
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

    @Schema(description = "Response containing availability status and conflicting booking IDs")
    @Data
    @AllArgsConstructor
    public static class CheckAvailabilityResponseDto {
        @Schema(description = "Whether the location is available", example = "true")
        private boolean available;
        
        @Schema(description = "List of conflicting booking IDs", example = "[]")
        private List<UUID> conflictingBookingIds;
    }
}
