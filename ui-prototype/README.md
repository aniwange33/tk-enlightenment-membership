# Taraku Members UI — Design Prototype (“Luminary”)

A standalone, front-end-only design prototype for the Taraku Enlightenment Club members' app.
This first slice covers the **login + first-run flow**. No backend required — the logic is mocked
but mirrors the real REST contracts so it ports cleanly.

## Aesthetic — “Luminary”
Light emerging from a dark field: deep ink background with a breathing amber glow, warm gold accents,
an ember call-to-action, and a fine grain overlay.

- **Display:** Fraunces (high-contrast serif)
- **Body:** Hanken Grotesk
- **Credentials / code:** Spline Sans Mono
- Fully responsive, keyboard-accessible, and `prefers-reduced-motion` aware.

## View it
```bash
cd ui-prototype
python3 -m http.server 5500
# open http://localhost:5500
```
(Or just open `index.html` directly in a browser.)

## Screens / states
Routed by hash so each state is shareable: `#login`, `#change`, `#forgot`, `#reset`, `#done`.

| State | What it shows |
|-------|---------------|
| `#login`  | Email + password, forgot link |
| `#change` | Forced first-login password change (with strength meter) |
| `#forgot` | Request a reset link |
| `#reset`  | Set a new password (token read from `?token=` in the URL) |
| `#done`   | Signed-in confirmation stub |

## Demo credentials (mock)
| Input | Result |
|-------|--------|
| any valid email + 8+ char password | signs in → `#done` |
| `first@taraku.org` | triggers the **forced first-login** change flow |
| `admin@taraku.org` | “Welcome, Secretary” (admin copy) |
| `nobody@taraku.org` | 401 invalid credentials |
| forgot: `unknown@taraku.org` | 404 (no account) |
| forgot: `left@taraku.org` | 403 (disabled account) |
| `?token=expired` | reset shows 400 invalid/expired token |

## How it maps to the API (porting plan)
The mock functions in `app.js` (`mockLogin`, `mockForgot`, `mockReset`) are the only seams. Replace
each with a `fetch`:

| Mock | Real endpoint | Response |
|------|---------------|----------|
| `mockLogin`   | `POST /api/auth/login` | `{ token, mustChangePassword }` |
| change submit | `POST /api/auth/change-password` (Bearer token) | `{ token, mustChangePassword }` |
| `mockForgot`  | `POST /api/auth/forgot-password` | `{ message }` · 404 unknown · 403 disabled |
| `mockReset`   | `POST /api/auth/reset-password` | `{ message }` · 400 invalid/expired |

Notes for wiring:
- Auth is **stateless JWT** — store the returned `token` and send it as `Authorization: Bearer <token>`.
- On login, if `mustChangePassword` is true, route to `#change` before the portal (the backend's
  `MustChangePasswordFilter` enforces this server-side too).
- Client validation mirrors the server: email format + password `min 8` (matches the DTO `@Size`).
- Error copy mirrors the backend's `ProblemDetail` responses.

## Member portal — `member.html`
The authenticated member's view, in the same Luminary system (reuses `styles.css` tokens/primitives,
adds `member.css` + `member.js`).

- **Membership credential card** — club name, member name (Fraunces), membership number (mono), status badge, “member since”.
- **My profile** — read-only details (membership no., email, date of birth, joined) plus editable **phone & address** with an inline Edit → Save/Cancel flow and a strength-free contact form. Mirrors that only phone/address are editable.
- **My dues** — year-by-year table with Paid/Unpaid pills and paid dates, plus an outstanding-count summary. Read-only (officers record payment).
- Deep link `member.html#edit` opens straight into edit mode.

How it maps to the API:

| Action | Endpoint | Shape |
|--------|----------|-------|
| load profile | `GET /api/me` | `MeResponse` |
| save contact details | `PUT /api/me` | `{ phone, address }` |
| load dues | `GET /api/me/dues` | `[{ year, paid, paidDate }]` |

Status badge classes cover all states: `badge--active / inactive / suspended / terminated`.

## Admin console — `admin.html`
The officer's console, same Luminary system (`styles.css` + shared portal bits from `member.css` + `admin.css` + `admin.js`).

- **Members** — searchable / status-filtered / paginated roster table (name + email, number, status badge, current-year dues pill, joined). Row → opens a **detail drawer**.
- **Member drawer** — facts, **change status** (options limited to valid transitions per `MemberStatus.canTransitionTo`; terminated is terminal), and **dues history** with per-year **Mark paid** (reactivates an INACTIVE member, mirroring the backend).
- **Register member** — modal form; auto-assigns the next `TEC-YYYY-NNN`, rejects duplicate email, seeds an unpaid current-year dues record.
- **Announcements** — subject + message + recipient group (Active / All), with live recipient counts; “All” excludes terminated. Returns a sent count.
- Deep links for demos: `admin.html#member`, `admin.html#register`, `admin.html#announcements`.

How it maps to the API:

| Action | Endpoint |
|--------|----------|
| list / search / filter / paginate | `GET /api/members?query=&status=&page=` |
| register | `POST /api/members` |
| change status | `PATCH /api/members/{id}/status` |
| dues history | `GET /api/members/{id}/dues` |
| record payment | `POST /api/members/{id}/dues/{year}/pay` |
| announcement | `POST /api/announcements` → `{ recipientCount }` |

## Status
Design prototype only — not wired into the Spring app. **Done:** login + first-run flow, member portal,
**admin console** — the full Luminary design language across all three role experiences. **Next:** port to the
chosen production stack (swap the `mock*` seams for `fetch` with the Bearer token).
