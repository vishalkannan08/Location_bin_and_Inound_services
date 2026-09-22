-- WMS Increment 1 :: location-bin-service schema
-- This service owns ONLY these two tables. warehouse_id is a logical reference
-- to warehouse-service and deliberately has NO foreign key: cross-service DB
-- coupling is prohibited by the architecture rules.

CREATE TABLE location (
    id            UUID         PRIMARY KEY,
    warehouse_id  UUID         NOT NULL,
    location_code VARCHAR(50)  NOT NULL,
    name          VARCHAR(150) NOT NULL,
    type          VARCHAR(30)  NOT NULL,
    status        VARCHAR(20)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_location_warehouse_code UNIQUE (warehouse_id, location_code)
);

CREATE INDEX idx_location_warehouse_id ON location (warehouse_id);
CREATE INDEX idx_location_status       ON location (status);

CREATE TABLE bin (
    id          UUID        PRIMARY KEY,
    location_id UUID        NOT NULL,
    bin_code    VARCHAR(50) NOT NULL,
    capacity    INTEGER     NOT NULL,
    status      VARCHAR(20) NOT NULL,
    version     BIGINT      NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_bin_location      FOREIGN KEY (location_id) REFERENCES location (id),
    CONSTRAINT uk_bin_location_code UNIQUE (location_id, bin_code),
    CONSTRAINT ck_bin_capacity      CHECK (capacity > 0)
);

CREATE INDEX idx_bin_location_id ON bin (location_id);
CREATE INDEX idx_bin_status      ON bin (status);
