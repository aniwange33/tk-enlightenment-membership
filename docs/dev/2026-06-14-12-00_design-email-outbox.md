# Design: Email Outbox with `SKIP LOCKED` Workers (Future)

**Date:** 2026-06-14
**Status:** 📐 Proposed — **NOT implemented.** Design-on-record only.
**Related:** `2026-06-13-20-00_followups-dues-automation.md` (ShedLock + chunked batch), `2026-06-14-10-00_spec-backend-completion.md` (announcements US-7)

---

## Why this exists

The current design uses **ShedLock single-leader execution** for the scheduled dues jobs: when a cron fires on N instances, exactly one instance runs the whole sweep and the others stay idle. That is the right call for *correctness* at current scale (cheap, annual, effectively-zero-member jobs) — see the ShedLock follow-up.

It stops being the right call when the goal shifts from **exclusion** to **throughput**: a single instance draining a large fan-out (e.g. the May-1 reminder/inactivation blast to tens of thousands of members, or large admin announcements) within a tight window, while the other instances sit idle. At that point we want instances to **share** the work, not have one own it.

The standard tool for "many workers drain a queue in parallel without stepping on each other" is **`SELECT … FOR UPDATE SKIP LOCKED`** over an **outbox table**. This doc records that design so the trade-off is on record; do not build until the trigger below is hit.

**Trigger to act:** any single fan-out (reminders, inactivation, announcements) grows large enough that one-instance draining risks missing its deadline window or holding resources too long — *and* we are running on more than one instance. Until then, ShedLock single-leader + chunked transactions is sufficient.

---

## Shape of the problem this changes

| Concern | Today (ShedLock single-leader) | With outbox + SKIP LOCKED |
|---|---|---|
| Who runs the sweep | exactly one instance | producer enqueues; **all** instances drain in parallel |
| Primitive | timestamp lease lock (`shedlock` row) | per-message claim via `FOR UPDATE SKIP LOCKED` |
| Goal optimized | correctness (no double-run) | throughput (finish fan-out fast) |
| Delivery semantics | at-least-once (Modulith republish) | at-least-once + explicit retry/dead-letter |
| Failure isolation | chunk granularity | per-message |

Note these are **either/or for a given job**: ShedLock deliberately blocks other instances from entering, so a job can't be both single-leader *and* parallel-drained. Adopting the outbox for a job means dropping `@SchedulerLock` for that job's *dispatch* and instead having the scheduled job act only as a **producer** that enqueues messages.

---

## Proposed design

### 1. Outbox table (new Flyway migration, owned by the `notification` module)

```
email_outbox
------------------------------------------------------------------
id              VARCHAR(36)  PK
recipient       VARCHAR(255) NOT NULL
subject         VARCHAR(255) NOT NULL
body            TEXT         NOT NULL
status          VARCHAR(20)  NOT NULL   -- PENDING | CLAIMED | SENT | FAILED
attempts        INT          NOT NULL DEFAULT 0
max_attempts    INT          NOT NULL DEFAULT 5
available_at    TIMESTAMPTZ  NOT NULL   -- not eligible to claim before this (retry backoff)
claimed_at      TIMESTAMPTZ              -- visibility-timeout anchor
claimed_by      VARCHAR(255)
last_error      TEXT
dedup_key       VARCHAR(255) UNIQUE      -- idempotency: e.g. "reminder:2027:<memberId>"
created_at / updated_at / version
------------------------------------------------------------------
-- partial index for the hot claim query:
CREATE INDEX idx_email_outbox_claimable
  ON email_outbox (available_at)
  WHERE status = 'PENDING';
```

### 2. Producer side (replaces direct fan-out)

- The scheduled reminder/inactivation job (and the announcement endpoint) becomes a **producer**: it inserts one `PENDING` row per recipient with a deterministic `dedup_key`, then returns. This insert is fast and can itself stay chunked.
- The unique `dedup_key` makes enqueue idempotent — re-running the producer (Modulith republish, scheduler retry) cannot create duplicate messages.

### 3. Worker side (the `SKIP LOCKED` part — runs on every instance)

A fixed-rate poller on each instance claims and sends a batch:

```sql
-- claim: atomic, contention-free across instances
UPDATE email_outbox
   SET status = 'CLAIMED', claimed_at = now(), claimed_by = :instance, attempts = attempts + 1
 WHERE id IN (
     SELECT id FROM email_outbox
      WHERE status = 'PENDING' AND available_at <= now()
      ORDER BY available_at
      FOR UPDATE SKIP LOCKED
      LIMIT :batchSize
 )
RETURNING *;
```

- `FOR UPDATE SKIP LOCKED` lets each instance grab a **different** set of rows with no blocking and no double-claim — this is the throughput win over single-leader.
- For each claimed row: send via `EmailService`; on success → `status = SENT`; on failure → `status = PENDING`, `available_at = now() + backoff(attempts)` (exponential), or `status = FAILED` (dead-letter) once `attempts >= max_attempts`.
- **Visibility timeout / crash recovery:** a separate reaper resets rows stuck in `CLAIMED` past a timeout (`claimed_at < now() - lease`) back to `PENDING`, so a crashed worker's claims are re-driven. (The poller itself can stay ShedLock-free; the reaper sweep can be ShedLock-guarded since it's cheap.)

### 4. Concurrency & delivery semantics

- **Mutual exclusion per message:** guaranteed by the row lock taken in the claim `UPDATE … FOR UPDATE SKIP LOCKED` — two instances cannot claim the same row.
- **Still at-least-once:** a worker that sends then crashes before marking `SENT` will have the row re-driven after the visibility timeout → possible duplicate send. Mitigated, not eliminated, by SMTP-level dedup; the `dedup_key` only dedups *enqueue*, not *delivery*. Acceptable for email.
- **Ordering:** not guaranteed across instances; fine for independent emails.

### 5. Relationship to the existing event-driven email

- Today notifications are sent inline in `@ApplicationModuleListener`s. The outbox would sit **behind** `EmailService` for high-volume paths only: listeners enqueue instead of sending. Low-volume transactional emails (welcome, password reset, dues-paid) can keep sending inline — the outbox is reserved for the bulk fan-outs that actually need parallel draining.

---

## Operational concerns (when built)

- **Backpressure / poison messages:** `max_attempts` + `FAILED` dead-letter state; alert/metric on `FAILED` count and on `PENDING` age (oldest `available_at`).
- **Throughput knobs:** `batchSize`, poll interval, worker count per instance.
- **Cleanup:** periodic purge/archive of old `SENT` rows.
- **Metrics:** queue depth, send latency, retry rate, dead-letter rate.

## Testing approach (when built)

- Unit: backoff/attempt/dead-letter state transitions.
- Integration (Testcontainers Postgres): two concurrent claimers never claim the same row (verify with parallel threads); visibility-timeout re-drive; idempotent enqueue via `dedup_key`; dead-letter after `max_attempts`.

## Trade-offs vs today

- **Gains:** parallel draining across instances; per-message failure isolation, retries, and dead-lettering; durable queue decoupled from the request/scheduler.
- **Costs:** new table + migration, producer/worker/reaper code, retry/poison/visibility-timeout logic, and tests — meaningful complexity. Not justified at current (≈0 member) scale, where ShedLock single-leader + chunking is simpler and sufficient.

## Decision

Defer. Keep ShedLock single-leader + chunked transactions until the trigger above is met. Revisit per-job: only the job(s) that actually outgrow single-instance draining need to move to the outbox; the rest stay on the simpler model.
