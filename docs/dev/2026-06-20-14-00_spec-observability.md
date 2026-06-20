# Observability Spec — OpenTelemetry + Grafana

**Date:** 2026-06-20
**Status:** Draft (interview output)
**Related:** whole app on `main` (Spring Boot 4 modular monolith + React SPA). Installed skills: `otel-collector`, `otel-instrumentation`, `otel-ottl`, `otel-semantic-conventions`.

---

## Goal

Add **traces, metrics, and logs** to the membership app so operators can see request flow, latency, errors, JVM/DB health, and key business events — using Spring Boot's recommended **Micrometer + OpenTelemetry (OTLP)** approach, visualised in **Grafana** (local dev via the `grafana/otel-lgtm` image). Frontend RUM is **phase 2**.

## Roles

| Role | Description |
|------|-------------|
| **Operator** | Runs/monitors the app (SRE/officer-on-call). Consumes Grafana dashboards, health/readiness, alerts. |
| **Developer** | Debugs latency/errors by following traces and correlated logs. |
| **System** | Auto-emits telemetry (HTTP, JDBC, JVM) and propagates trace context. |

## Decisions (from interview)

| Area | Decision |
|------|----------|
| Signals | **Traces + Metrics + Logs** |
| Instrumentation | **Spring Micrometer + OpenTelemetry, in-process** (no Java agent) |
| Transport | **OTLP** to the **`grafana/otel-lgtm`** dev image (Tempo + Mimir/Prometheus + Loki + Grafana) |
| Sampling | **100% (always-on)**; configurable |
| PII | **IDs only** — never emails/names/addresses in telemetry |
| Actuator | Add it; **health/info (+ probes) public, everything else ADMIN-only** |
| Custom telemetry | Domain counters · scheduled-job spans · email-send outcomes · async event correlation |
| Tests | Telemetry **disabled** in the test profile |
| Dashboards | **Provision a starter dashboard** (committed JSON) |
| Frontend RUM | **Deferred to phase 2** (Grafana Faro) |

---

## Architecture

```
Spring Boot app (membership)
  Micrometer Observation API ──► Micrometer Tracing ──► OTLP exporter ─┐
  Micrometer (OTLP registry, metrics) ─────────────────────────────────┼─► OTLP (4317/4318)
  OTel Logback appender (logs) ────────────────────────────────────────┘        │
                                                                                 ▼
                                                              grafana/otel-lgtm container
                                                          (OTel Collector → Tempo/Mimir/Loki)
                                                                                 │
                                                                                 ▼
                                                                     Grafana UI (:3000)
```

- **Resource identity:** `service.name=membership` (already `spring.application.name`), `service.version` from the existing `git-commit-id`/build-info, `deployment.environment` from config.
- **Endpoint:** `OTEL_EXPORTER_OTLP_ENDPOINT` (default `http://localhost:4318`); export is a no-op/disabled when unset.

## Components & changes

1. **Dependencies** (`pom.xml`): `spring-boot-starter-actuator`, `micrometer-tracing-bridge-otel`, `opentelemetry-exporter-otlp`, `micrometer-registry-otlp`, OTel Logback appender, `io.micrometer:context-propagation`, and `spring-modulith-observability` (module-boundary spans).
2. **Config** (`application.yaml`): `management.otlp.{tracing,metrics,logging}`, `management.tracing.sampling.probability=1.0`, actuator exposure (`health,info,prometheus`), health probes (`liveness`/`readiness`), resource attributes.
3. **Actuator security** (`SecurityConfig`): permit `/actuator/health/**` + `/actuator/info`; require `ADMIN` for the rest of `/actuator/**` (it currently falls under the public non-`/api` rule — must be tightened).
4. **Custom telemetry:**
   - **Domain counters** (Micrometer): `members.registered`, `dues.paid`, `announcements.sent` (tag/`recipientCount`), `members.auto_inactivated`, `password_reset.requested` / `.completed`.
   - **Scheduled-job spans** (`@Observed`/Observation around the 3 dues jobs): duration, outcome, processed/chunk counts; reflect ShedLock **skipped vs executed**.
   - **Email-send outcomes**: success/failure counter + spans around `EmailService.send` (failures are currently only logged).
   - **Async event correlation:** propagate trace context across `@ApplicationModuleListener` so registration → async dues-record creation → welcome email share one trace.
5. **Local stack:** `compose.yaml` (or `docker-compose-observability.yaml`) with `grafana/otel-lgtm` (ports 3000 UI, 4317/4318 OTLP); document `docker compose up`.
6. **Starter dashboard:** committed Grafana dashboard JSON (request rate/latency/errors, JVM, DB pool, domain counters, job runs) under `observability/` or provisioned via the compose mount.
7. **Tests:** test profile disables exporters (no OTLP endpoint / `management.*.export.enabled=false`) so the 80-test suite stays fast and connection-noise-free.

---

## Edge cases & cross-cutting invariants

- **No PII in telemetry.** Member IDs and membership numbers only — never email/name/address on spans, metric tags, or log MDC. (New recurring-bug guard.)
- **Async context loss.** `@ApplicationModuleListener` runs after commit on a separate thread; without context propagation the dues/email work appears as **disconnected traces**. Must wire `context-propagation` + a context-propagating task decorator (and/or `spring-modulith-observability`).
- **Actuator exposure.** Non-`/api` paths are currently public (SPA). Actuator must NOT inherit that for anything beyond health/info — else `env`/`loggers`/`heapdump` leak.
- **Export resilience.** A down/missing collector must never fail a request or block startup — exporters degrade quietly; app behaviour is unchanged when `OTEL_EXPORTER_OTLP_ENDPOINT` is unset.
- **Scheduled jobs + ShedLock.** Only one instance runs each job; a skipped run (lock held) should be observable as skipped, not missing.
- **Metric cardinality.** Keep tags bounded (status, job name, outcome) — never member id/email as a metric tag (only as span/log attributes).
- **Test isolation.** Observability autoconfig must load without a backend; no test may emit to a real endpoint.

---

## User Stories

- **US-1**: As an Operator, I open **Grafana** (`localhost:3000`, fed by `otel-lgtm`) so that I can explore the app's traces, metrics, and logs in one place.
- **US-2**: As an Operator, I hit the **health/readiness/liveness** actuator endpoints so that I (and orchestrators) can probe whether the app is up and ready.
- **US-3**: As an Operator, I view a **starter Grafana dashboard** (request rate, latency, error rate, JVM, DB pool, domain counters, job runs) so that I get app health at a glance without building queries.
- **US-4**: As a Developer, I follow a **distributed trace** of a request from HTTP → service → JPA/SQL so that I can pinpoint where latency or errors occur.
- **US-5**: As a Developer, I see **logs correlated to traces** (trace/span IDs on every line) so that I can jump from a slow/failed span to its exact log output.
- **US-6**: As a Developer, I see a member registration trace **linked to its async dues-record creation and welcome email** so that cross-module async work isn't a set of disconnected traces.
- **US-7**: As an Operator, I see **domain counters** (members registered, dues paid, announcements sent, members auto-inactivated, password resets) so that I can monitor business activity.
- **US-8**: As an Operator, I see a **span per scheduled dues job** (generate / reminders / inactivation) with duration, outcome, processed count, and whether it ran or was ShedLock-skipped, so that I can confirm the jobs fire correctly across instances.
- **US-9**: As an Operator, I see **email send success/failure** telemetry so that silently-failing emails (welcome, reset, dues, announcements) become visible instead of log-only.
- **US-10**: As the System, I auto-instrument HTTP server requests, JDBC queries, and JVM/DB-pool metrics so that baseline observability needs no per-endpoint code.
- **US-11**: As an Operator, I get telemetry **without any PII** (IDs/membership numbers only) so that the observability store holds no personal data.
- **US-12**: As an Operator, I restrict actuator so that only **health/info are public** and metrics/env/loggers require ADMIN, so that operational endpoints aren't exposed.
- **US-13**: As a Developer, I run the suite with **telemetry disabled** so that tests stay fast and free of exporter connection noise.
- **US-14**: As the System, I degrade gracefully when **no collector is reachable** so that a missing/down telemetry backend never breaks requests or startup.

### Phase 2 (deferred) — Frontend RUM
- **US-15**: As an Operator, I see **real-user monitoring** for the React SPA (page loads, web vitals, JS errors) via **Grafana Faro** so that I understand client-side experience.
- **US-16**: As a Developer, I see **browser→backend traces** (Faro propagates `traceparent` on same-origin `/api` calls) so that a slow page links to its server spans end-to-end.

---

## Build order

1. Deps + `application.yaml` config + `compose.yaml` (`otel-lgtm`); verify traces/metrics/logs land in Grafana for a manual request.
2. Actuator + `SecurityConfig` rule (health/info public, rest ADMIN); test the access split.
3. Async context propagation (registration → dues → email one trace); verify in Tempo.
4. Custom telemetry: domain counters, job spans, email outcomes.
5. Starter dashboard JSON; test-profile disable; docs.
6. **Phase 2:** Grafana Faro in the SPA + Alloy `faro.receiver` (or Faro→OTLP).
