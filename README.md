# Booking Service

Microservice for managing location reservations for events in the Planify platform. Provides REST and gRPC APIs secured with Keycloak authentication and publishes events via Kafka.

## Technologies

### Backend Framework & Language
- **Java 21** - Programming language
- **Spring Boot 3.5.7** - Application framework
- **Spring Security** - Security and authentication
- **Spring Data JPA** - Database access
- **Hibernate** - ORM framework
- **Lombok** - Boilerplate code reduction

### Database
- **PostgreSQL** - Database
- **Flyway** - Database migrations
- **HikariCP** - Connection pooling

### Security & Authentication
- **Keycloak** - OAuth2/OIDC authentication and authorization
- **Spring OAuth2 Resource Server** - JWT validation

### Messaging System
- **Apache Kafka** - Event streaming platform
- **Spring Kafka** - Kafka integration

### gRPC Communication
- **gRPC 1.66.0** - RPC framework
- **Protobuf 3.25.5** - Serialization format
- **grpc-spring-boot-starter** - Spring Boot gRPC integration

### Monitoring & Health
- **Spring Boot Actuator** - Health checks and metrics
- **Micrometer Prometheus** - Metrics export
- **Resilience4j** - Circuit breakers, retry, rate limiting, bulkheads

### Containerization
- **Docker** - Application containerization
- **Kubernetes/Helm** - Orchestration (Helm charts included)

## System Integrations

- **Keycloak**: OAuth2/OIDC authentication and authorization. All endpoints require a valid JWT Bearer token.
- **Kafka**: Publishes domain events for booking creation and cancellation consumed by other services (topics: `booking-created`, `booking.events`).
- **PostgreSQL**: Stores all location and booking data via Hibernate/JPA with Flyway migrations in the `booking` schema.
- **gRPC**: Primary interface for booking operations (create, cancel, get booking) called by other microservices.

## API Endpoints

All REST endpoints require `Authorization: Bearer <JWT_TOKEN>` header.

### REST API

#### Locations (/api/locations)

**GET** `/api/locations` — List all active locations
```bash
curl http://localhost:8086/api/locations
```

**GET** `/api/locations/{id}` — Get location details by ID
```bash
curl http://localhost:8086/api/locations/550e8400-e29b-41d4-a716-446655440000
```

#### Availability (/api/booking)

**GET** `/api/booking/{locationId}/availability?start={epochMillis}&end={epochMillis}` — Check location availability in time window
```bash
curl "http://localhost:8086/api/booking/550e8400-e29b-41d4-a716-446655440000/availability?start=1735036800000&end=1735040400000"
```

Response:
```json
{
  "available": true,
  "conflictingBookingIds": []
}
```

### gRPC API

gRPC definition: [src/main/proto/booking.proto](src/main/proto/booking.proto)

Service: `BookingService` (port 9095)

**CheckAvailability** — Check if location is available in time window

Request:
```protobuf
CheckAvailabilityRequest {
  string location_id = 1;
  int64 start_epoch_millis = 2;
  int64 end_epoch_millis = 3;
}
```

Response:
```protobuf
CheckAvailabilityResponse {
  bool available = 1;
  repeated string conflicting_booking_ids = 2;
}
```

**CreateBooking** — Create new booking for a location

Request:
```protobuf
CreateBookingRequest {
  string location_id = 1;
  string event_id = 2;
  string organization_id = 3;  // UUID
  int64 start_epoch_millis = 4;
  int64 end_epoch_millis = 5;
  string currency = 6;  // e.g. "EUR"
}
```

Response:
```protobuf
CreateBookingResponse {
  string booking_id = 1;
  string status = 2;
  bool available = 3;
  repeated string conflicts = 4;
  int32 total_amount_cents = 5;
}
```

**GetBooking** — Retrieve booking details by ID

Request:
```protobuf
GetBookingRequest {
  string booking_id = 1;
}
```

Response:
```protobuf
GetBookingResponse {
  string booking_id = 1;
  string location_id = 2;
  string event_id = 3;
  string organization_id = 4;
  int64 start_epoch_millis = 5;
  int64 end_epoch_millis = 6;
  string status = 7;
  int32 total_amount_cents = 8;
  string currency = 9;
}
```

**CancelBooking** — Cancel an existing booking

Request:
```protobuf
CancelBookingRequest {
  string booking_id = 1;
}
```

Response:
```protobuf
CancelBookingResponse {
  string status = 1;
}
```

## Database Structure

The service uses PostgreSQL with the following core entities in the `booking` schema:

### Locations

Venue locations available for booking. Contains:

- `id` (UUID, PK)
- `name` (TEXT) - Location name
- `address` (TEXT) - Physical address
- `capacity` (INT) - Maximum capacity
- `price_per_hour_cents` (INT) - Hourly rental price in cents
- `active` (BOOLEAN) - Whether location accepts bookings

### Bookings

Reservation records linking locations to events. Contains:

- `id` (UUID, PK)
- `location_id` (UUID, FK to locations)
- `event_id` (UUID) - Reference to event in Event Service
- `organization_id` (UUID) - Organization making the booking
- `start_time` (TIMESTAMP) - Booking start time
- `end_time` (TIMESTAMP) - Booking end time
- `status` (TEXT) - Booking status: `PENDING_PAYMENT`, `CONFIRMED`, `CANCELLED`, `FAILED`
- `total_amount_cents` (INT) - Total cost in cents
- `currency` (VARCHAR) - Currency code (e.g. EUR)
- `payment_intent_id` (TEXT) - External payment reference
- `created_at` (TIMESTAMP) - Creation timestamp
- `updated_at` (TIMESTAMP) - Last update timestamp

**Relationships**: All entities use UUIDs and enforce referential integrity via foreign keys. Audit fields (`created_at`, `updated_at`) track changes. Database schema is versioned via Flyway migrations in `src/main/resources/db/migration/`.

## Installation and Setup

### Prerequisites

- Java 21 or newer
- Maven 3.6+
- Docker and Docker Compose
- Git

### Infrastructure Setup

This service requires PostgreSQL, Kafka, and Keycloak to run. These dependencies are provided via Docker containers in the main Planify repository.

Clone and setup the infrastructure:

```bash
# Clone the main Planify repository
git clone https://github.com/rso-project-2025-26/planify.git
cd planify

# Follow the setup instructions in the main repository README
# This will start all required infrastructure services (PostgreSQL, Kafka, Keycloak)
```

Refer to the main Planify repository (https://github.com/rso-project-2025-26/planify) documentation for detailed infrastructure setup instructions.

### Configuration

The application uses a single `application.yaml` configuration file located in `src/main/resources/`.

Important environment variables:

```
SERVER_PORT=8086
DB_URL=jdbc:postgresql://localhost:5432/planify
DB_USERNAME=planify
DB_PASSWORD=planify
DB_SCHEMA=booking
KEYCLOAK_ISSUER_URI=http://localhost:9080/realms/planify
KEYCLOAK_JWK_SET_URI=http://localhost:9080/realms/planify/protocol/openid-connect/certs
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KAFKA_TOPIC_BOOKING_CREATED=booking-created
KAFKA_TOPIC_BOOKING_EVENTS=booking.events
```

### Local Run

```bash
# Build project
mvn clean package

# Run application
mvn spring-boot:run
```

### Using Makefile

```bash
# Build project
make build

# Docker build
make docker-build

# Docker run
make docker-run

# Tests
make test
```

### Docker Run

```bash
# Build Docker image
docker build -t planify/booking-service:0.0.1 .

# Run container
docker run -p 8086:8086 -p 9095:9095 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/planify \
  -e KEYCLOAK_ISSUER_URI=http://host.docker.internal:9080/realms/planify \
  -e KAFKA_BOOTSTRAP_SERVERS=host.docker.internal:9092 \
  planify/booking-service:0.0.1
```

### Kubernetes/Helm Deployment

```bash
# Install with Helm
helm install booking-service ./helm/booking

# Install with specific environment values
helm install booking-service ./helm/booking -f ./helm/booking/values-dev.yaml

# Upgrade
helm upgrade booking-service ./helm/booking

# Uninstall
helm uninstall booking-service
```

### Flyway Migrations

Migrations are located in `src/main/resources/db/migration/`:

- `V1__init.sql` - Initial schema and seed data for locations

Manual migration run:

```bash
mvn flyway:migrate
```

## Health Check & Monitoring

### Actuator Endpoints

- **GET** `/actuator/health` — Health check endpoint
- **GET** `/actuator/health/liveness` — Liveness probe
- **GET** `/actuator/health/readiness` — Readiness probe
- **GET** `/actuator/prometheus` — Prometheus metrics
- **GET** `/actuator/info` — Application information
- **GET** `/actuator/metrics` — Application metrics

## Kafka Events

The service publishes the following events to Kafka:

### Booking Creation Events

**booking-created** — Published when a booking is successfully created

Contains:
- `bookingId` - UUID of the created booking
- `locationId` - UUID of the booked location
- `eventId` - UUID of the associated event
- `organizationId` - UUID of the organization
- `startTime` - Booking start time (ISO 8601)
- `endTime` - Booking end time (ISO 8601)
- `totalAmountCents` - Total cost in cents
- `currency` - Currency code (e.g., EUR)
- `status` - Booking status (typically PENDING_PAYMENT)
- `timestamp` - Event timestamp

Example:
```json
{
  "bookingId": "550e8400-e29b-41d4-a716-446655440000",
  "locationId": "660e8400-e29b-41d4-a716-446655440001",
  "eventId": "770e8400-e29b-41d4-a716-446655440002",
  "organizationId": "880e8400-e29b-41d4-a716-446655440003",
  "startTime": "2024-12-24T10:00:00Z",
  "endTime": "2024-12-24T14:00:00Z",
  "totalAmountCents": 48000,
  "currency": "EUR",
  "status": "PENDING_PAYMENT"
}
```

### Booking Status Events

**booking.events** — Published on significant booking status changes (cancellation, confirmation, failure)

Contains similar structure to booking-created with updated status field.

## Resilience4j

The service implements:

- **Circuit Breakers** - Prevention of cascading failures for:
  - `availabilityService`
  - `bookingCreation`
  - `bookingCancellation`
  - `eventManagerService`
- **Retry** - Automatic retry of failed calls
- **Rate Limiting** - Request rate limiting
- **Bulkheads** - Resource isolation

Configuration is managed via `application.yaml` with health indicators exposed through Actuator.

## Testing

```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=AvailabilityServiceTest

# Run with coverage report
mvn test jacoco:report
```

Tests are located in `src/test/java/com/planify/booking_service/` and include:

- `AvailabilityServiceTest` - Availability checking logic
- `BookingDomainServiceTest` - Booking creation and management
- `BookingEventProducerTest` - Kafka event publishing
