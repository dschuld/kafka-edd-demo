CREATE TABLE IF NOT EXISTS orders (
    id          UUID                     DEFAULT gen_random_uuid() PRIMARY KEY,
    customer_id VARCHAR(255)             NOT NULL,
    product     VARCHAR(255)             NOT NULL,
    quantity    INT                      NOT NULL,
    status      VARCHAR(50)              NOT NULL DEFAULT 'PENDING',
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS order_outbox (
    id           UUID                     DEFAULT gen_random_uuid() PRIMARY KEY,
    order_id     UUID                     NOT NULL,
    event_type   VARCHAR(100)             NOT NULL,
    payload      TEXT                     NOT NULL,
    published    BOOLEAN                  NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    published_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_order_outbox_published ON order_outbox (published) WHERE published = FALSE;

CREATE TABLE IF NOT EXISTS orders_processed (
    id         UUID         DEFAULT gen_random_uuid() PRIMARY KEY,
    message_id VARCHAR(255) NOT NULL,
    result     VARCHAR(255) NOT NULL,
    CONSTRAINT uq_orders_processed_message_id UNIQUE (message_id)
);

-- Inventory deduplication
CREATE TABLE IF NOT EXISTS inventory_processed (
    id         UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    message_id VARCHAR(255) NOT NULL,
    result     VARCHAR(255) NOT NULL,
    CONSTRAINT uq_inventory_processed_message_id UNIQUE (message_id)
);

-- Inventory outbox
CREATE TABLE IF NOT EXISTS inventory_outbox (
    id           UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    order_id     UUID NOT NULL,
    event_type   VARCHAR(100) NOT NULL,
    payload      TEXT NOT NULL,
    published    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    published_at TIMESTAMP WITH TIME ZONE
);
CREATE INDEX IF NOT EXISTS idx_inventory_outbox_published ON inventory_outbox (published) WHERE published = FALSE;

-- Payment outbox
CREATE TABLE IF NOT EXISTS payment_outbox (
    id           UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    order_id     UUID NOT NULL,
    event_type   VARCHAR(100) NOT NULL,
    payload      TEXT NOT NULL,
    published    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    published_at TIMESTAMP WITH TIME ZONE
);
CREATE INDEX IF NOT EXISTS idx_payment_outbox_published ON payment_outbox (published) WHERE published = FALSE;

-- Order consumer deduplication
CREATE TABLE IF NOT EXISTS order_events_processed (
    id         UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    message_id VARCHAR(255) NOT NULL,
    result     VARCHAR(255) NOT NULL,
    CONSTRAINT uq_order_events_processed_message_id UNIQUE (message_id)
);
