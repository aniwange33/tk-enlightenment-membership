# Observability (local)

Run the membership app with the full Grafana stack and view **traces, metrics, and logs**
exported over OTLP (Spring Boot 4 Micrometer + OpenTelemetry).

Contents:
- `compose.yaml` — the `grafana/otel-lgtm` backend (Grafana + Tempo + Prometheus/Mimir + Loki + OTel collector)
- `../scripts/run-with-observability.sh` — one command to start the backend + Postgres + the app

---

## Quick start

```bash
# from the repo root
./scripts/run-with-observability.sh
```

This will:
1. Start `grafana/otel-lgtm` (Grafana on `:3000`, OTLP receiver on `:4318`).
2. Start PostgreSQL (`membership_db` / `postgres` / `amos33`) — or **reuse** one already on `:5432`.
3. Export the required config/secrets (random JWT secret, seeded admin, dummy mail, OTLP endpoint).
4. Build the jar if needed (`mvn package -DskipTests`, which also bundles the React SPA) and run it.

Then open **http://localhost:3000** → **Explore** → switch the data source and filter by `service.name=membership`:
- **Tempo** (traces) — Search → Service Name `membership`
- **Prometheus** (metrics) — e.g. `http_server_request_duration_seconds_count`, `jvm_memory_used_bytes`, `hikaricp_connections_active`
- **Loki** (logs) — `{service_name="membership"}` (logs carry trace IDs, so you can jump log → trace)

App is at **http://localhost:8080** (full React SPA at `/`). Admin login is seeded from the env
(`admin@taraku-enlightenment.org` / `AdminPass123!`); the first login forces a password change.

Generate some telemetry by clicking around the app, or:
```bash
curl localhost:8080/actuator/health
```

## Stop / tear down

```bash
# stop the app:        Ctrl+C
# remove the backend containers:
./scripts/run-with-observability.sh down
```

---

## Options & overrides

The script reuses an existing service if its port is taken (no clash), and rebuilds only when needed.

| Variable | Default | Purpose |
|----------|---------|---------|
| `SERVER_PORT` | `8080` | App HTTP port |
| `PG_PORT` | `5432` | Postgres host port |
| `GRAFANA_PORT` | `3000` | Grafana UI port |
| `OTLP_HTTP_PORT` / `OTLP_GRPC_PORT` | `4318` / `4317` | OTLP receiver ports |
| `REBUILD` | _(unset)_ | `REBUILD=1` forces `mvn package` even if a jar exists |
| `AUTH_JWT_SECRET` | random | JWT signing secret |
| `AUTH_ADMIN_EMAIL` / `AUTH_ADMIN_PASSWORD` | `admin@taraku-enlightenment.org` / `AdminPass123!` | Seeded admin |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | `dummy` / `dummy` | SMTP (dummy → welcome emails fail, visible as logs/spans) |
| `DEPLOY_ENV` | `local` | `deployment.environment` resource attribute |

Examples:
```bash
SERVER_PORT=8090 PG_PORT=5433 GRAFANA_PORT=3001 ./scripts/run-with-observability.sh
REBUILD=1 ./scripts/run-with-observability.sh
```

If you reuse an existing Postgres on `:5432`, make sure it has db `membership_db` / user `postgres`
(password from `application-postgres.yaml` = `amos33`), or point the app elsewhere with
`SPRING_DATASOURCE_URL` (or a repo-root `.env`).

---

## Manual alternative (without the script)

```bash
docker compose -f observability/compose.yaml up -d      # Grafana stack
createdb membership_db                                  # or a postgres container on :5432
./mvnw package -DskipTests                              # build jar (incl. SPA)
OTEL_OTLP_ENDPOINT=http://localhost:4318 \
AUTH_JWT_SECRET=... AUTH_ADMIN_EMAIL=... AUTH_ADMIN_PASSWORD=... \
MAIL_USERNAME=dummy MAIL_PASSWORD=dummy \
java -jar target/membership-*.jar
```

---

## Configuration reference

Telemetry is configured in `src/main/resources/application.yaml` under `management:` and exports
over OTLP to `OTEL_OTLP_ENDPOINT` (default `http://localhost:4318`):

- Traces: `management.opentelemetry.tracing.export.otlp.endpoint`
- Logs: `management.opentelemetry.logging.export.otlp.endpoint`
- Metrics: `management.otlp.metrics.export.url`
- Sampling: `management.tracing.sampling.probability` (`1.0` = everything)

Other facts:
- Resource attributes: `service.name=membership`, `service.namespace=taraku`, `deployment.environment`.
- Actuator: `/actuator/health` + `/actuator/info` are public; `/actuator/metrics` requires an ADMIN token.
- No PII in telemetry (member IDs / membership numbers only, never emails/names).
- Telemetry export is disabled in tests; in production set `OTEL_OTLP_ENDPOINT` to your collector.
