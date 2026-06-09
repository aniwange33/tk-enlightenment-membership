CREATE TABLE dues_records (
    id          VARCHAR(36)  NOT NULL PRIMARY KEY,
    member_id   VARCHAR(36)  NOT NULL,
    year        INTEGER      NOT NULL,
    paid        BOOLEAN      NOT NULL DEFAULT FALSE,
    paid_date   DATE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version     BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_dues_member_year UNIQUE (member_id, year),
    CONSTRAINT fk_dues_member FOREIGN KEY (member_id) REFERENCES members (id)
);

CREATE INDEX idx_dues_member_id ON dues_records (member_id);
CREATE INDEX idx_dues_year ON dues_records (year);
CREATE INDEX idx_dues_paid ON dues_records (paid);
