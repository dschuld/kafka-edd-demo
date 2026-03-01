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
