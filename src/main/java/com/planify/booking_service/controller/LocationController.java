package com.planify.booking_service.controller;

import com.planify.booking_service.domain.Location;
import com.planify.booking_service.repository.LocationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
@Tag(name = "Locations", description = "Location management endpoints for venue booking")
@SecurityRequirement(name = "bearer-jwt")
public class LocationController {

    private final LocationRepository locationRepository;

    @Operation(
        summary = "Get all active locations",
        description = "Returns a list of all active locations available for booking. Locations are sorted by name in ascending order."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved list of locations",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Location.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token", content = @Content),
    })
    @GetMapping
    public ResponseEntity<List<Location>> getAllActive() {
        return ResponseEntity.ok(locationRepository.findByActiveTrueOrderByNameAsc());
    }

    @Operation(
        summary = "Get location by ID",
        description = "Returns detailed information about a specific location identified by its UUID."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved location",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Location.class))),
        @ApiResponse(responseCode = "404", description = "Location not found", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token", content = @Content),
    })
    @GetMapping("/{id}")
    public ResponseEntity<Location> getById(
            @Parameter(required = true)
            @PathVariable UUID id) {
        return locationRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
