package com.planify.booking_service.messaging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingEventProducer Unit Tests")
class BookingEventProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private BookingEventProducer bookingEventProducer;

    private static final String BOOKING_CREATED_TOPIC = "booking-created-topic";
    private static final String BOOKING_EVENTS_TOPIC = "booking-events-topic";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(bookingEventProducer, "bookingCreatedTopic", BOOKING_CREATED_TOPIC);
        ReflectionTestUtils.setField(bookingEventProducer, "bookingEventsTopic", BOOKING_EVENTS_TOPIC);
    }

    @Test
    @DisplayName("Should send message to booking-created topic")
    void testPublishBookingCreated_SendsToCorrectTopic() {
        // Given
        UUID bookingId = UUID.randomUUID();
        Map<String, Object> payload = new HashMap<>();
        payload.put("bookingId", bookingId);
        payload.put("status", "PENDING_PAYMENT");

        // When
        bookingEventProducer.publishBookingCreated(payload);

        // Then
        verify(kafkaTemplate).send(eq(BOOKING_CREATED_TOPIC), eq(payload));
    }

    @Test
    @DisplayName("Should send message to booking-events topic")
    void testPublishBookingEvent_SendsToCorrectTopic() {
        // Given
        UUID bookingId = UUID.randomUUID();
        Map<String, Object> payload = new HashMap<>();
        payload.put("bookingId", bookingId);
        payload.put("status", "CANCELLED");
        payload.put("type", "booking_cancelled");

        // When
        bookingEventProducer.publishBookingEvent(payload);

        // Then
        verify(kafkaTemplate).send(eq(BOOKING_EVENTS_TOPIC), eq(payload));
    }

    @Test
    @DisplayName("Should pass correct payload to Kafka template for booking created")
    void testPublishBookingCreated_CorrectPayload() {
        // Given
        UUID bookingId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        Map<String, Object> payload = new HashMap<>();
        payload.put("bookingId", bookingId);
        payload.put("status", "CONFIRMED");
        payload.put("locationId", locationId);
        payload.put("totalAmountCents", 15000);
        payload.put("currency", "USD");

        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);

        // When
        bookingEventProducer.publishBookingCreated(payload);

        // Then
        verify(kafkaTemplate).send(eq(BOOKING_CREATED_TOPIC), payloadCaptor.capture());
        
        Map<String, Object> capturedPayload = payloadCaptor.getValue();
        assertThat(capturedPayload).isEqualTo(payload);
        assertThat(capturedPayload).containsEntry("bookingId", bookingId);
        assertThat(capturedPayload).containsEntry("status", "CONFIRMED");
        assertThat(capturedPayload).containsEntry("locationId", locationId);
        assertThat(capturedPayload).containsEntry("totalAmountCents", 15000);
        assertThat(capturedPayload).containsEntry("currency", "USD");
    }

    @Test
    @DisplayName("Should pass correct payload to Kafka template for booking event")
    void testPublishBookingEvent_CorrectPayload() {
        // Given
        UUID bookingId = UUID.randomUUID();
        Map<String, Object> payload = new HashMap<>();
        payload.put("bookingId", bookingId);
        payload.put("status", "CANCELLED");
        payload.put("type", "booking_cancelled");
        payload.put("timestamp", System.currentTimeMillis());

        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);

        // When
        bookingEventProducer.publishBookingEvent(payload);

        // Then
        verify(kafkaTemplate).send(eq(BOOKING_EVENTS_TOPIC), payloadCaptor.capture());
        
        Map<String, Object> capturedPayload = payloadCaptor.getValue();
        assertThat(capturedPayload).isEqualTo(payload);
        assertThat(capturedPayload).containsEntry("bookingId", bookingId);
        assertThat(capturedPayload).containsEntry("status", "CANCELLED");
        assertThat(capturedPayload).containsEntry("type", "booking_cancelled");
        assertThat(capturedPayload).containsKey("timestamp");
    }

    @Test
    @DisplayName("Should handle empty payload")
    void testPublishBookingCreated_EmptyPayload() {
        // Given
        Map<String, Object> emptyPayload = new HashMap<>();

        // When
        bookingEventProducer.publishBookingCreated(emptyPayload);

        // Then
        verify(kafkaTemplate).send(eq(BOOKING_CREATED_TOPIC), eq(emptyPayload));
    }

    @Test
    @DisplayName("Should handle null values in payload")
    void testPublishBookingEvent_NullValues() {
        // Given
        Map<String, Object> payload = new HashMap<>();
        payload.put("bookingId", UUID.randomUUID());
        payload.put("status", null);
        payload.put("type", "booking_event");

        // When
        bookingEventProducer.publishBookingEvent(payload);

        // Then
        verify(kafkaTemplate).send(eq(BOOKING_EVENTS_TOPIC), eq(payload));
    }

    @Test
    @DisplayName("Should call Kafka template exactly once per publish")
    void testPublishBookingCreated_SingleInvocation() {
        // Given
        Map<String, Object> payload = Map.of("bookingId", UUID.randomUUID());

        // When
        bookingEventProducer.publishBookingCreated(payload);

        // Then
        verify(kafkaTemplate, times(1)).send(anyString(), any());
    }

    @Test
    @DisplayName("Should handle multiple consecutive publishes")
    void testPublishBookingCreated_MultiplePublishes() {
        // Given
        Map<String, Object> payload1 = Map.of("bookingId", UUID.randomUUID());
        Map<String, Object> payload2 = Map.of("bookingId", UUID.randomUUID());
        Map<String, Object> payload3 = Map.of("bookingId", UUID.randomUUID());

        // When
        bookingEventProducer.publishBookingCreated(payload1);
        bookingEventProducer.publishBookingCreated(payload2);
        bookingEventProducer.publishBookingCreated(payload3);

        // Then
        verify(kafkaTemplate, times(3)).send(eq(BOOKING_CREATED_TOPIC), any());
    }

    @Test
    @DisplayName("Should handle payload with nested objects")
    void testPublishBookingEvent_NestedObjects() {
        // Given
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "api");
        metadata.put("version", "1.0");

        Map<String, Object> payload = new HashMap<>();
        payload.put("bookingId", UUID.randomUUID());
        payload.put("metadata", metadata);
        payload.put("tags", new String[]{"important", "priority"});

        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);

        // When
        bookingEventProducer.publishBookingEvent(payload);

        // Then
        verify(kafkaTemplate).send(eq(BOOKING_EVENTS_TOPIC), payloadCaptor.capture());
        
        Map<String, Object> capturedPayload = payloadCaptor.getValue();
        assertThat(capturedPayload).containsKey("metadata");
        assertThat(capturedPayload).containsKey("tags");
    }

    @Test
    @DisplayName("Should not modify the original payload")
    void testPublishBookingCreated_DoesNotModifyPayload() {
        // Given
        UUID bookingId = UUID.randomUUID();
        Map<String, Object> originalPayload = new HashMap<>();
        originalPayload.put("bookingId", bookingId);
        originalPayload.put("status", "PENDING_PAYMENT");
        
        Map<String, Object> payloadCopy = new HashMap<>(originalPayload);

        // When
        bookingEventProducer.publishBookingCreated(originalPayload);

        // Then
        assertThat(originalPayload).isEqualTo(payloadCopy);
    }
}
