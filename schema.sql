CREATE TABLE IF NOT EXISTS executed_orders
(
    id          SERIAL PRIMARY KEY,
    symbol      VARCHAR(10)    NOT NULL,
    type        VARCHAR(4)     NOT NULL,
    price       DECIMAL(10, 2) NOT NULL,
    quantity    INT            NOT NULL,
    executed_at TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_partial  BOOLEAN        NOT NULL DEFAULT FALSE
);