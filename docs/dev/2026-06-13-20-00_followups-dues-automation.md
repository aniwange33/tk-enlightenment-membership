# Dues Automation — Deferred Follow-ups

**Date:** 2026-06-13
**Branch context:** `feature/dues-automation`
**Status:** ✅ RESOLVED 2026-06-14 on `feature/scheduler-hardening` — both items below are implemented. See "Resolution" at the bottom.

These two items surfaced during the dues implementation review. They were intentionally left out of the `feature/dues-automation` change because neither is a correctness bug at the current (single-instance) deployment scale. Captured here so they are not lost.

---

## 1. Scheduled jobs are not safe under multi-instance deployment

**Severity:** Medium (only once deployed on >1 instance)

`DuesScheduler` uses plain `@Scheduled` cron jobs (`generateDuesForNewYear`, `sendDuesReminders`, `inactivateUnpaidMembers`). With more than one application instance, every node fires each job independently.

- **Inactivation** is idempotent today — the `member.status() == ACTIVE` guard in `DuesService.inactivateUnpaidMembers` means a second run is a no-op. ✅
- **Reminders** are **not** idempotent — each instance would re-publish `DuesReminderEvent`, so members receive duplicate reminder emails.
- **Generation** is idempotent via `createDuesRecord` (unique `(member_id, year)` + existence check), so duplicates are harmless. ✅

**Suggested approach:** introduce distributed locking (e.g. [ShedLock](https://github.com/lukas-krecan/ShedLock) with a JDBC lock provider, reusing the existing PostgreSQL datasource) and wrap the three scheduled methods so exactly one instance runs each job per fire. Alternatively, designate a single "scheduler" instance via profile/config.

**Trigger to act:** before the app is run on more than one instance.

---

## 2. Batch jobs run as a single large transaction

**Severity:** Low (fine at current member volume)

`DuesService.sendDuesReminders`, `inactivateUnpaidMembers`, and `generateDuesForYear` are covered by the class-level `@Transactional` and iterate over the full member/dues set within one transaction. Per-record `try/catch` gives resilience against individual failures, but the whole batch commits as one unit and holds a transaction open for the duration.

**Suggested approach (when member count grows):**
- Process in pages/chunks with a new transaction per chunk (e.g. `Pageable` over members, or `REQUIRES_NEW` per record/batch), so a long job does not hold one transaction open and partial progress is durable.
- Consider Spring Batch or a simple chunked loop if volume justifies it.

**Trigger to act:** when the active-member count is large enough that a single-transaction sweep becomes a latency/locking concern.

---

## Resolution (2026-06-14)

Both items implemented on `feature/scheduler-hardening` before going multi-instance:

1. **ShedLock** added (`shedlock-spring` + `shedlock-provider-jdbc-template` 6.10.0). `SchedulerLockConfig` enables it with a JDBC `LockProvider` over the existing Postgres datasource using `usingDbTime()`; Flyway `V6` creates the `shedlock` table. All three `@Scheduled` methods carry `@SchedulerLock` (`lockAtMostFor=PT1H`, `lockAtLeastFor=PT5M`) so exactly one instance runs each job per fire.
2. **Chunked batch transactions** — `DuesService` batch methods are now `@Transactional(NOT_SUPPORTED)` orchestrators that page the source (active members / unpaid records, `dues.batch.chunk-size` default 500) and hand each page to `DuesChunkService`, whose methods run one transaction per chunk. A failed chunk is logged and the sweep continues (partial progress durable at chunk granularity).

Also folded in here: the deferred code-review item — `ALL_MEMBERS` announcements now use a `findByStatusNot(TERMINATED)` query instead of loading the full roster and filtering in memory.

## Out of scope here, tracked elsewhere

- **New-year dues generation** — implemented on `feature/dues-automation` (the original deferred gap from the auth/RBAC spec).
- Other deferred features (password reset, admin announcements, admin-invites-admin, web UI) are tracked in `2026-06-13-18-00_spec-auth-rbac.md`.
