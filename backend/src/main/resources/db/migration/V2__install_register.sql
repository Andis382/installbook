-- The installer's register: customers, the units fitted for them, visits, booking requests
-- and the reminders sent for each service cycle.

CREATE TABLE installer_settings (
    organization_id                  BIGINT      PRIMARY KEY REFERENCES organizations(id) ON DELETE CASCADE,
    default_warranty_months          INT         NOT NULL DEFAULT 24,
    default_service_interval_months  INT         NOT NULL DEFAULT 12,
    reminder_lead_days               INT         NOT NULL DEFAULT 30,
    typical_service_price_cents      INT         NOT NULL DEFAULT 5000,
    public_phone                     VARCHAR(20),
    -- the daily reminder run and the Monday digest each happen once per local day
    last_reminder_run_on             DATE,
    last_digest_on                   DATE
);

CREATE TABLE customers (
    id                   BIGSERIAL    PRIMARY KEY,
    organization_id      BIGINT       NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name                 VARCHAR(160) NOT NULL,
    phone                VARCHAR(20)  NOT NULL,
    locale               VARCHAR(5)   NOT NULL DEFAULT 'sq',
    whatsapp_opt_in      BOOLEAN      NOT NULL DEFAULT FALSE,
    whatsapp_opt_in_at   TIMESTAMPTZ,
    whatsapp_opt_out_at  TIMESTAMPTZ,
    notes                TEXT,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT customers_phone_per_org UNIQUE (organization_id, phone)
);
CREATE INDEX customers_org_name_idx ON customers (organization_id, lower(name));

CREATE TABLE units (
    id                       BIGSERIAL    PRIMARY KEY,
    organization_id          BIGINT       NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    customer_id              BIGINT       NOT NULL REFERENCES customers(id),
    type                     VARCHAR(24)  NOT NULL,
    brand                    VARCHAR(80)  NOT NULL,
    model                    VARCHAR(120),
    serial_number            VARCHAR(80),
    plate_photo_id           VARCHAR(36)  REFERENCES stored_files(id) ON DELETE SET NULL,
    installed_on             DATE         NOT NULL,
    address                  VARCHAR(255) NOT NULL,
    latitude                 DOUBLE PRECISION,
    longitude                DOUBLE PRECISION,
    warranty_months          INT          NOT NULL,
    warranty_until           DATE         NOT NULL,
    service_interval_months  INT          NOT NULL,
    last_service_on          DATE,
    next_service_due         DATE         NOT NULL,
    card_token               VARCHAR(32)  NOT NULL UNIQUE,
    status                   VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    removed_on               DATE,
    notes                    TEXT,
    installed_by             BIGINT       REFERENCES users(id) ON DELETE SET NULL,
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX units_org_due_idx ON units (organization_id, status, next_service_due);
CREATE INDEX units_customer_idx ON units (customer_id);
CREATE INDEX units_org_serial_idx ON units (organization_id, upper(serial_number));
CREATE INDEX units_org_installed_idx ON units (organization_id, installed_on);

CREATE TABLE service_visits (
    id                  BIGSERIAL    PRIMARY KEY,
    organization_id     BIGINT       NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    unit_id             BIGINT       NOT NULL REFERENCES units(id) ON DELETE CASCADE,
    visited_on          DATE         NOT NULL,
    kind                VARCHAR(24)  NOT NULL,
    price_cents         INT,
    parts               TEXT,
    notes               TEXT,
    performed_by        BIGINT       REFERENCES users(id) ON DELETE SET NULL,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX service_visits_unit_idx ON service_visits (unit_id, visited_on DESC);

CREATE TABLE reminders (
    id               BIGSERIAL    PRIMARY KEY,
    organization_id  BIGINT       NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    unit_id          BIGINT       NOT NULL REFERENCES units(id) ON DELETE CASCADE,
    -- the cycle this reminder belongs to: the unit's next-service date when it went out
    due_on           DATE         NOT NULL,
    trigger_kind     VARCHAR(12)  NOT NULL,
    outcome          VARCHAR(16)  NOT NULL,
    sent_at          TIMESTAMPTZ,
    message_id       BIGINT       REFERENCES outbound_messages(id) ON DELETE SET NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT reminders_one_per_cycle UNIQUE (unit_id, due_on)
);
CREATE INDEX reminders_org_sent_idx ON reminders (organization_id, sent_at);

CREATE TABLE booking_requests (
    id                BIGSERIAL    PRIMARY KEY,
    organization_id   BIGINT       NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    unit_id           BIGINT       NOT NULL REFERENCES units(id) ON DELETE CASCADE,
    reminder_id       BIGINT       REFERENCES reminders(id) ON DELETE SET NULL,
    source            VARCHAR(12)  NOT NULL,
    preferred_date    DATE,
    preferred_period  VARCHAR(12),
    note              TEXT,
    status            VARCHAR(12)  NOT NULL DEFAULT 'NEW',
    scheduled_at      TIMESTAMPTZ,
    decided_at        TIMESTAMPTZ,
    visit_id          BIGINT       REFERENCES service_visits(id) ON DELETE SET NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX booking_requests_org_status_idx ON booking_requests (organization_id, status, created_at DESC);
CREATE INDEX booking_requests_unit_idx ON booking_requests (unit_id);
