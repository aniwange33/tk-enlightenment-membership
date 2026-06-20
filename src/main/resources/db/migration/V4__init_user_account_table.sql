CREATE TABLE user_accounts (
    id                   VARCHAR(36)  NOT NULL PRIMARY KEY,
    email                VARCHAR(255) NOT NULL UNIQUE,
    password_hash        VARCHAR(255) NOT NULL,
    role                 VARCHAR(20)  NOT NULL,
    member_id            VARCHAR(36),
    enabled              BOOLEAN      NOT NULL DEFAULT TRUE,
    must_change_password BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version              BIGINT       NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX idx_user_accounts_email ON user_accounts (email);
CREATE INDEX idx_user_accounts_member_id ON user_accounts (member_id);
CREATE INDEX idx_user_accounts_role ON user_accounts (role);
