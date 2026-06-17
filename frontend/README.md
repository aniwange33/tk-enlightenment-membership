# Taraku Members — Web App (React SPA)

The production frontend for the Taraku Enlightenment Club members' app. React + TypeScript + Vite,
in the **“Luminary”** design system (ported from `../ui-prototype`). Talks to the Spring Boot REST API
with a stateless JWT bearer token.

## Develop
```bash
cd frontend
npm install
npm run dev          # http://localhost:5173
```
`npm run dev` proxies `/api` → `http://localhost:8080` (see `vite.config.ts`), so run the Spring Boot
app alongside it. No CORS needed.

## Build
```bash
npm run build        # typechecks, then emits into ../src/main/resources/static
```
The output goes into Spring's static resources so the whole app ships as **one jar**. That folder is
git-ignored — it's a build artifact (wired into Maven at package time; see "Backend integration").

## Structure
```
src/
  api/        types.ts (mirrors the DTOs) · client.ts (typed fetch wrapper + endpoint groups)
  auth/       jwt.ts (decode) · AuthContext.tsx (token in localStorage, role/identity from JWT)
  components/ AuthLayout · TopBar · PasswordInput · StatusBadge · Foot · CenterNote
  pages/      Login · ChangePassword · ForgotPassword · ResetPassword · MemberPortal · AdminConsole
  styles/     luminary.css (shared system) · member.css · admin.css
  util/       format.ts
public/       tk-logo.jpg (club emblem)
```

## How it uses the API
The `mock*` seams from the prototype are gone — every screen calls the real endpoints via `src/api/client.ts`:

| Area | Endpoints |
|------|-----------|
| Auth | `POST /api/auth/{login,change-password,forgot-password,reset-password}` |
| Member | `GET/PUT /api/me`, `GET /api/me/dues` |
| Admin members | `GET/POST /api/members`, `GET /api/members/{id}`, `PATCH …/status`, `GET …/dues`, `POST …/dues/{year}/pay` |
| Announcements | `POST /api/announcements` |

- The JWT is stored in `localStorage` and attached as `Authorization: Bearer …`.
- `role`, `memberId`, `email`, `mustChangePassword` are read from the JWT claims.
- Routing: unauthenticated → `/login`; `mustChangePassword` → `/change-password`; then ADMIN → `/admin`, MEMBER → `/member`.
- Backend `ProblemDetail` errors surface via the `ApiError` class.

## Backend integration (remaining wiring — not yet done)
To serve the SPA from Spring and ship a single jar:
1. **Maven** — add `frontend-maven-plugin` (or `exec`) to run `npm ci && npm run build` during `mvn package`, emitting into `target/classes/static`.
2. **Security** — permit the SPA shell + assets in `SecurityConfig` (`/`, `/index.html`, `/assets/**`, `/tk-logo.jpg`) and add a forward/fallback so client-side routes (`/member`, `/admin`, …) return `index.html`.
