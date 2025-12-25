package com.planify.booking_service.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class BookingEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.booking-created}")
    private String bookingCreatedTopic;

    @Value("${kafka.topics.booking-events}")
    private String bookingEventsTopic;

    public void publishBookingCreated(Map<String, Object> payload) {
        kafkaTemplate.send(bookingCreatedTopic, payload);
    }

    public void publishBookingEvent(Map<String, Object> payload) {
        kafkaTemplate.send(bookingEventsTopic, payload);
    }
}
