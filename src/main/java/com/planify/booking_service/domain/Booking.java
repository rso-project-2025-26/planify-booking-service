package com.planify.booking_service.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bookings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @Column(name = "total_amount_cents", nullable = false)
    private Integer totalAmountCents;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "payment_intent_id")
    private String paymentIntentId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
