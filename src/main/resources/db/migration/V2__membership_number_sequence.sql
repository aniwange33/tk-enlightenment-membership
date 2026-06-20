CREATE TABLE membership_number_sequence (
    year        INTEGER NOT NULL PRIMARY KEY,
    next_value  INTEGER NOT NULL DEFAULT 1
);
