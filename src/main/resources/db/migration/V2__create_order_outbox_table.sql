CREATE TABLE order_outbox (
    id           UUID                     DEFAULT gen_random_uuid() PRIMARY KEY,
    order_id     UUID                     NOT NULL,
    event_type   VARCHAR(100)             NOT NULL,
    payload      TEXT                     NOT NULL,
    published    BOOLEAN                  NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    published_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_order_outbox_published ON order_outbox (published) WHERE published = FALSE;
