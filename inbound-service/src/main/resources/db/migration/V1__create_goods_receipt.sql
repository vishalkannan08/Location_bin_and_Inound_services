-- WMS Increment 1 :: inbound-service schema
-- Owns goods_receipt and goods_receipt_line ONLY.
--
-- warehouse_id is a logical reference to warehouse-service and has NO foreign key.
-- sku_id is a SKU *code* (e.g. 'SKU-1001'), not an id: the requirement doc defines
-- no product table in this increment. Revisit at the architecture review.

CREATE SEQUENCE goods_receipt_number_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE goods_receipt (
    id             UUID         PRIMARY KEY,
    receipt_number VARCHAR(30)  NOT NULL,
    warehouse_id   UUID         NOT NULL,
    reference_type VARCHAR(30)  NOT NULL,
    reference_id   VARCHAR(100) NOT NULL,
    status         VARCHAR(30)  NOT NULL,
    received_at    TIMESTAMPTZ,
    version        BIGINT       NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_goods_receipt_number UNIQUE (receipt_number)
);

CREATE INDEX idx_goods_receipt_warehouse ON goods_receipt (warehouse_id);
CREATE INDEX idx_goods_receipt_status    ON goods_receipt (status);
CREATE INDEX idx_goods_receipt_reference ON goods_receipt (reference_type, reference_id);

CREATE TABLE goods_receipt_line (
    id                UUID           PRIMARY KEY,
    goods_receipt_id  UUID           NOT NULL,
    line_number       INTEGER        NOT NULL,
    sku_id            VARCHAR(60)    NOT NULL,
    expected_quantity NUMERIC(15, 3) NOT NULL,
    received_quantity NUMERIC(15, 3) NOT NULL DEFAULT 0,
    put_away_quantity NUMERIC(15, 3) NOT NULL DEFAULT 0,
    uom               VARCHAR(10)    NOT NULL,
    batch_number      VARCHAR(60),
    serial_number     VARCHAR(60),
    status            VARCHAR(30)    NOT NULL,
    version           BIGINT         NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ    NOT NULL,
    updated_at        TIMESTAMPTZ    NOT NULL,
    CONSTRAINT fk_line_goods_receipt FOREIGN KEY (goods_receipt_id)
        REFERENCES goods_receipt (id) ON DELETE CASCADE,
    CONSTRAINT uk_line_receipt_number UNIQUE (goods_receipt_id, line_number),

    -- Section 9: "Receipt and Put-Away quantities cannot be negative"
    CONSTRAINT ck_line_expected_non_negative  CHECK (expected_quantity >= 0),
    CONSTRAINT ck_line_received_non_negative  CHECK (received_quantity >= 0),
    CONSTRAINT ck_line_put_away_non_negative  CHECK (put_away_quantity >= 0),

    -- Section 9: "Put-Away quantity cannot exceed eligible received quantity".
    -- Enforced at the database, not only in Java, because putaway-service will
    -- eventually drive this concurrently.
    CONSTRAINT ck_line_put_away_within_received CHECK (put_away_quantity <= received_quantity)
);

CREATE INDEX idx_line_goods_receipt ON goods_receipt_line (goods_receipt_id);
CREATE INDEX idx_line_sku           ON goods_receipt_line (sku_id);
CREATE INDEX idx_line_status        ON goods_receipt_line (status);
