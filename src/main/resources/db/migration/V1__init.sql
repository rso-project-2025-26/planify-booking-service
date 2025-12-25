-- Locations
CREATE TABLE IF NOT EXISTS booking.locations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    address TEXT NOT NULL,
    capacity INT NOT NULL,
    price_per_hour_cents INT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

-- Bookings
CREATE TABLE IF NOT EXISTS booking.bookings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    location_id UUID NOT NULL REFERENCES booking.locations(id),
    event_id UUID,
    organization_id UUID NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    status TEXT NOT NULL,
    total_amount_cents INT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    payment_intent_id TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_bookings_location_time
    ON booking.bookings(location_id, start_time, end_time);

-- Mock data
INSERT INTO booking.locations (name, address, capacity, price_per_hour_cents, active) VALUES
    ('City Conference Hall', 'Cankarjeva 1, Ljubljana', 200, 12000, TRUE),
    ('Riverside Venue', 'Breg 5, Maribor', 120, 9000, TRUE),
    ('Tech Hub Auditorium', 'Tržaška 25, Ljubljana', 300, 15000, TRUE)
ON CONFLICT DO NOTHING;
