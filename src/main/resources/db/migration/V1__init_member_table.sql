CREATE TABLE members (
    id          VARCHAR(36)  NOT NULL PRIMARY KEY,
    membership_number VARCHAR(20) NOT NULL UNIQUE,
    first_name  VARCHAR(100) NOT NULL,
    last_name   VARCHAR(100) NOT NULL,
    date_of_birth DATE       NOT NULL,
    email       VARCHAR(255) NOT NULL UNIQUE,
    phone       VARCHAR(50),
    address     VARCHAR(500),
    join_date   DATE         NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version     BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_members_status ON members (status);
CREATE INDEX idx_members_email ON members (email);
CREATE INDEX idx_members_membership_number ON members (membership_number);
CREATE INDEX idx_members_last_name ON members (last_name);
