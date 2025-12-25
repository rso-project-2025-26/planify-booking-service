### Planify Booking Service

Booking microservice for managing location reservations for events at Planify.

Key features:
- gRPC API for checking availability, creating bookings, and cancelling bookings
- Kafka events: `booking-created`, `booking.events`, `booking.metrics`
- Flyway migrations with seed/mock data for locations

Tech stack:
- Java, Spring Boot, gRPC, PostgreSQL, Kafka, Flyway

How to run locally:
1. Start Postgres and Kafka from the shared stack at `planify/infrastructure/docker-compose.yaml`.
2. Build and run:
   - Build: `mvn clean package`
   - Run: `mvn spring-boot:run`

Environment variables:
- `SPRING_DATASOURCE_URL` (default: `jdbc:postgresql://localhost:5432/planify`)
- `SPRING_DATASOURCE_USERNAME` (default: `planify`)
- `SPRING_DATASOURCE_PASSWORD` (default: `planify`)
- `SPRING_KAFKA_BOOTSTRAP_SERVERS` (default: `localhost:9092`)

Ports:
- HTTP (Actuator/health and REST): `8086`
- gRPC: `9095`

gRPC definition: `src/main/proto/booking.proto`

HTTP REST endpoints (implemented):
- `GET /api/locations` — list active locations
- `GET /api/locations/{id}` — get location by ID
- `GET /api/booking/{locationId}/availability?start={epochMillis}&end={epochMillis}` — check availability window for a location

Example requests

gRPC (pseudo):
```
CheckAvailabilityRequest {
  location_id: 1,
  start_epoch_millis: 1735036800000,
  end_epoch_millis: 1735040400000
}

CreateBookingRequest {
  location_id: 1,
  event_id: 42,
  organization_id: "550e8400-e29b-41d4-a716-446655440000",
  start_epoch_millis: 1735036800000,
  end_epoch_millis: 1735040400000,
  currency: "EUR",
  addon_quantities: { 1: 1, 3: 2 }
}
```

REST (curl):
```
curl -s "http://localhost:8084/api/booking/00000000-0000-0000-0000-000000000001/availability?start=1735036800000&end=1735040400000"

curl -s http://localhost:8084/api/locations
```

Notes:
- New bookings start in status `PENDING_PAYMENT`. Payment processing is not implemented in this service.
- On successful creation, an event is published to `booking-created` and a metric/event to Kafka (see topics below).
- Booking statuses: `PENDING_PAYMENT`, `CONFIRMED`, `CANCELLED`, `FAILED`.

Kafka topics (producer only):
- `kafka.topics.booking-created` — emitted on successful booking creation
- `kafka.topics.booking-events` — emitted on significant state changes (e.g., cancellation)

Additional notes:
- Booking creation and cancellation are available via gRPC, not REST.
- Availability treats both `PENDING_PAYMENT` and `CONFIRMED` bookings as blocking for the requested time window.
