-- Tenancy, people and the shared message/file plumbing every feature builds on.

CREATE TABLE organizations (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    phone       VARCHAR(40),
    country     VARCHAR(2)   NOT NULL DEFAULT 'AL',
    locale      VARCHAR(5)   NOT NULL DEFAULT 'sq',
    timezone    VARCHAR(64)  NOT NULL DEFAULT 'Europe/Tirane',
    currency    VARCHAR(3)   NOT NULL DEFAULT 'EUR',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE users (
    id               BIGSERIAL PRIMARY KEY,
    organization_id  BIGINT       NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name             VARCHAR(255) NOT NULL,
    email            VARCHAR(255) NOT NULL UNIQUE,
    password_hash    VARCHAR(255) NOT NULL,
    role             VARCHAR(32)  NOT NULL,
    locale           VARCHAR(5)   NOT NULL DEFAULT 'sq',
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    last_login_at    TIMESTAMPTZ
);
CREATE INDEX users_organization_idx ON users (organization_id);

CREATE TABLE invitations (
    id                BIGSERIAL PRIMARY KEY,
    organization_id   BIGINT       NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    token             VARCHAR(64)  NOT NULL UNIQUE,
    role              VARCHAR(32)  NOT NULL,
    name              VARCHAR(255),
    invited_by        BIGINT       NOT NULL,
    expires_at        TIMESTAMPTZ  NOT NULL,
    accepted_at       TIMESTAMPTZ,
    accepted_user_id  BIGINT,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE outbound_messages (
    id                   BIGSERIAL PRIMARY KEY,
    organization_id      BIGINT REFERENCES organizations(id) ON DELETE CASCADE,
    channel              VARCHAR(16)  NOT NULL,
    recipient            VARCHAR(64)  NOT NULL,
    recipient_name       VARCHAR(255),
    template_key         VARCHAR(64)  NOT NULL,
    locale               VARCHAR(5)   NOT NULL,
    body                 TEXT         NOT NULL,
    link                 TEXT,
    params_json          TEXT,
    status               VARCHAR(16)  NOT NULL,
    provider             VARCHAR(32),
    provider_message_id  VARCHAR(128),
    error                TEXT,
    related_type         VARCHAR(64),
    related_id           BIGINT,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    sent_at              TIMESTAMPTZ,
    delivered_at         TIMESTAMPTZ,
    read_at              TIMESTAMPTZ
);
CREATE INDEX outbound_messages_org_idx ON outbound_messages (organization_id, created_at DESC);
CREATE INDEX outbound_messages_recipient_idx ON outbound_messages (recipient, created_at DESC);
CREATE INDEX outbound_messages_related_idx ON outbound_messages (related_type, related_id);
CREATE INDEX outbound_messages_provider_idx ON outbound_messages (provider_message_id);

CREATE TABLE inbound_messages (
    id                   BIGSERIAL PRIMARY KEY,
    organization_id      BIGINT REFERENCES organizations(id) ON DELETE CASCADE,
    from_phone           VARCHAR(64)  NOT NULL,
    body                 TEXT,
    kind                 VARCHAR(16)  NOT NULL,
    media_id             VARCHAR(128),
    media_type           VARCHAR(64),
    provider_message_id  VARCHAR(128) UNIQUE,
    handled              BOOLEAN      NOT NULL DEFAULT FALSE,
    handled_by           VARCHAR(64),
    related_type         VARCHAR(64),
    related_id           BIGINT,
    received_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX inbound_messages_org_idx ON inbound_messages (organization_id, received_at DESC);

CREATE TABLE stored_files (
    id               VARCHAR(36)  PRIMARY KEY,
    organization_id  BIGINT       NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    original_name    VARCHAR(255),
    content_type     VARCHAR(100) NOT NULL,
    size_bytes       BIGINT       NOT NULL,
    storage_path     VARCHAR(255) NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Spring Session JDBC: sessions survive restarts, so a phone stays signed in.
CREATE TABLE spring_session (
    primary_id             CHAR(36)     NOT NULL,
    session_id             CHAR(36)     NOT NULL,
    creation_time          BIGINT       NOT NULL,
    last_access_time       BIGINT       NOT NULL,
    max_inactive_interval  INT          NOT NULL,
    expiry_time            BIGINT       NOT NULL,
    principal_name         VARCHAR(100),
    CONSTRAINT spring_session_pk PRIMARY KEY (primary_id)
);
CREATE UNIQUE INDEX spring_session_ix1 ON spring_session (session_id);
CREATE INDEX spring_session_ix2 ON spring_session (expiry_time);
CREATE INDEX spring_session_ix3 ON spring_session (principal_name);

CREATE TABLE spring_session_attributes (
    session_primary_id  CHAR(36)     NOT NULL,
    attribute_name      VARCHAR(200) NOT NULL,
    attribute_bytes     BYTEA        NOT NULL,
    CONSTRAINT spring_session_attributes_pk PRIMARY KEY (session_primary_id, attribute_name),
    CONSTRAINT spring_session_attributes_fk FOREIGN KEY (session_primary_id)
        REFERENCES spring_session (primary_id) ON DELETE CASCADE
);
