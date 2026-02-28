CREATE TABLE orders (
    id          UUID                     DEFAULT gen_random_uuid() PRIMARY KEY,
    customer_id VARCHAR(255)             NOT NULL,
    product     VARCHAR(255)             NOT NULL,
    quantity    INT                      NOT NULL,
    status      VARCHAR(50)              NOT NULL DEFAULT 'PENDING',
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
