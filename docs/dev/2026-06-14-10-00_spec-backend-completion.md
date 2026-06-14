# Backend Completion Spec — Password Reset & Admin Announcements

**Date:** 2026-06-14
**Status:** Draft (interview output)
**Author:** Interview session
**Related specs:** `2026-06-08-10-00_spec-membership.md` (US-7, US-14, Journeys 7 & 9), `2026-06-13-18-00_spec-auth-rbac.md` (auth module, deferred list)

---

## Goal

Close the last two unimplemented user stories so the **backend is feature-complete** against the original 19-story membership spec. No web UI in this round (REST only, consistent with the rest of the app).

| Story | Feature | Phase |
|-------|---------|-------|
| **US-14** | Password reset via email (forgot-password) | **Phase 1 — build first** |
| **US-7** | Admin announcements to member groups | Phase 2 |

Out of scope this round (tracked elsewhere): admin-invites-admin, UserAccount backfill, ShedLock scheduler hardening, web/UI frontend.

---

## Current state (context)

- Auth is a stateless **HS256 JWT** module (`auth/`). Accounts (`UserAccountEntity`) have `enabled` and `mustChangePassword` flags; account `enabled` is synced to member status via events.
- Passwords are BCrypt-hashed. First-login forced change is enforced by `MustChangePasswordFilter`.
- Notifications are event-driven: publishers emit domain events, `notification/` listeners send email. The original email table already lists *"Password reset requested → Reset link (24h expiry)"* and *"Admin announcement → custom subject + body"*.
- Flyway is at **V4** (`V4__init_user_account_table.sql`).

---

# Phase 1 — Password Reset (US-14)

## Decisions (from interview)

1. **Eligibility:** any account by email — both `ADMIN` and `MEMBER` roles, including the seeded admin.
2. **Response policy:** **explicit errors** — `404` for unknown email, `403` for disabled/terminated accounts. (See *Security trade-off* below.)
3. **Post-reset:** clears `mustChangePassword`; **existing JWTs remain valid** until natural expiry (stateless model, no revocation).
4. **Token storage:** dedicated DB table (Flyway **V5**), storing a **hash** of the token, not the raw value.

## Data model — `password_reset_token` (Flyway V5)

| Column | Type | Notes |
|--------|------|-------|
| `id` | String (TSID) | PK, follows `BaseEntity`/`IdGenerator` pattern |
| `user_account_id` | String | FK → `user_account.id` |
| `token_hash` | String | SHA-256 of the raw token; raw token only ever in the email link |
| `expires_at` | timestamptz | `createdAt + 24h` |
| `used_at` | timestamptz | nullable; set when consumed (single-use) |
| `created_at` / `updated_at` / `version` | — | from `BaseEntity` |

- Index on `token_hash` (lookup) and `user_account_id`.
- **Single-use:** a token is invalid once `used_at` is set.
- **Supersede:** issuing a new token marks any prior unused, unexpired tokens for that account as used (only the latest link works).

## Endpoints (auth module)

### `POST /api/auth/forgot-password`
Public (no auth). Body: `{ "email": "..." }` → `ForgotPasswordRequest`.

- Account not found → **404** `ResourceNotFoundException`.
- Account disabled / member TERMINATED → **403**.
- Otherwise: generate cryptographically-random raw token, store its hash + 24h expiry, supersede prior tokens, publish `PasswordResetRequestedEvent`, return **200** `{ "message": "Reset link sent" }`.

### `POST /api/auth/reset-password`
Public (no auth). Body: `{ "token": "...", "newPassword": "..." }` → `ResetPasswordRequest`.

- Token unknown / expired / already used → **400** (generic "invalid or expired token").
- New password fails policy → **400** (reuse existing password-validation rules from change-password flow).
- Account became disabled/terminated since request → **403**.
- Success → BCrypt-hash and store new password, set `mustChangePassword = false`, set token `used_at = now`, return **200** `{ "message": "Password updated" }`.

## Event & email

- New event **`PasswordResetRequestedEvent`** in `auth/domain/events/` — carries account id, email, raw token (or fully-built reset link).
- `notification/` listener (`@ApplicationModuleListener`) sends the **password-reset email** with a link of the form `{frontend-base}/reset-password?token=<raw>`. Reset-link base URL added to `NotificationProperties`.
- Raw token is **never** persisted or logged; only the hash is stored.

## Edge cases

- Expired token (>24h) → 400.
- Reused / already-consumed token → 400.
- Multiple requests in a row → only the newest token is valid (supersede rule).
- Concurrent reset attempts with the same token → optimistic `@Version` + `used_at` check makes the second a 400.
- Reset does **not** change member status; a TERMINATED member can't even request one (blocked at forgot-password with 403).
- Clock is injected (`ClockConfig`) so expiry is testable.

## Security trade-off (explicit decision)

Returning **404/403** instead of a uniform "if an account exists…" response **allows account enumeration** — an attacker can discover which emails are registered and which are disabled/terminated. This was a deliberate choice for clearer UX. Mitigations to consider later: rate-limiting forgot-password by IP/email, CAPTCHA, or switching to a uniform response. Recorded here so it remains a conscious decision.

---

# Phase 2 — Admin Announcements (US-7)

## Behavior (from spec Journey 7)

Admin sends an email blast to a recipient group; the response reports how many emails were sent.

### `POST /api/announcements` — ADMIN only
Body: `{ "subject": "...", "body": "...", "recipientGroup": "ACTIVE_MEMBERS" | "ALL_MEMBERS" }`.

- Resolve recipients via `MemberAPI` (`findActiveMembers` vs all members).
- Dispatch email to each recipient through `NotificationAPI` / an announcement event.
- Return **200** `{ "sentCount": N }`.

## Open questions (resolve at Phase 2 design time)

- **Sync vs async send:** inline (simple, but request blocks on N emails) vs publish one `AnnouncementRequestedEvent` and fan out asynchronously. Recommendation: async via event listener, return count of *targeted* recipients.
- **Per-recipient failure handling:** best-effort with per-recipient try/catch (consistent with batch dues jobs) and logged failures.
- **Persistence/audit:** fire-and-forget vs storing an `announcement` record for history. Spec doesn't require persistence; default fire-and-forget unless an audit trail is wanted.

---

## User Stories

- **US-1**: As a Member or Admin, I open the **Forgot Password** entry point and submit my email so that I can start recovering access to my account.
- **US-2**: As a Member or Admin, when I request a reset for an unknown email I receive a 404, and for a disabled/terminated account a 403, so that I get direct feedback about why recovery can't proceed.
- **US-3**: As a Member or Admin with a valid account, I receive an email containing a 24-hour reset link so that I can securely set a new password.
- **US-4**: As a Member or Admin, I submit my reset token and a new password so that my password is updated and I can log in again.
- **US-5**: As a Member or Admin, after a successful reset my forced-password-change flag is cleared so that I am not prompted to change it again on next login.
- **US-6**: As the System, I store only a hash of the reset token with a 24-hour expiry and single-use semantics so that leaked database rows cannot be replayed into account takeover.
- **US-7**: As the System, I supersede a user's prior unused reset tokens when a new one is issued so that only the most recent link is valid.
- **US-8**: As an Admin, I navigate to the announcement entry point so that I can compose a club-wide message.
- **US-9**: As an Admin, I send an announcement with a subject and body to All Active Members or All Members so that I can communicate club news.
- **US-10**: As an Admin, I see a count of how many recipients the announcement was sent to so that I can confirm delivery scope.

---

## Build order

1. **Phase 1 (US-14):** Flyway V5 → token entity/repository → `AccountService` reset methods → `PasswordResetRequestedEvent` + notification listener → `AuthController` two endpoints → tests (request, reset, expiry, reuse, supersede, disabled/terminated, enumeration responses).
2. **Phase 2 (US-7):** announcement DTO + controller → recipient resolution via `MemberAPI` → async dispatch event/listener → tests. Resolve the three open questions first.
