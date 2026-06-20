-- Distributed lock table for ShedLock, so each @Scheduled job runs on exactly
-- one instance per fire when the app is deployed on more than one node.
CREATE TABLE shedlock (
    name       VARCHAR(64)  NOT NULL PRIMARY KEY,
    lock_until TIMESTAMP    NOT NULL,
    locked_at  TIMESTAMP    NOT NULL,
    locked_by  VARCHAR(255) NOT NULL
);
