#!/usr/bin/env bash
#
# Start the membership app locally with the full Grafana observability stack.
#
#   ./scripts/run-with-observability.sh         # start backends + run the app (foreground)
#   ./scripts/run-with-observability.sh down     # stop & remove the backend containers
#
# Then open Grafana (default http://localhost:3000) -> Explore -> Tempo / Prometheus / Loki
# and filter by service.name=membership.
#
# Override any port/secret via env, e.g.:
#   SERVER_PORT=8090 GRAFANA_PORT=3001 PG_PORT=5433 ./scripts/run-with-observability.sh
#
set -euo pipefail
cd "$(dirname "$0")/.."

# ── Config (override via env) ────────────────────────────────────────────────
APP_PORT="${SERVER_PORT:-8080}"
PG_PORT="${PG_PORT:-5432}"
GRAFANA_PORT="${GRAFANA_PORT:-3000}"
OTLP_HTTP_PORT="${OTLP_HTTP_PORT:-4318}"
OTLP_GRPC_PORT="${OTLP_GRPC_PORT:-4317}"
LGTM_CONTAINER=tec-otel-lgtm
PG_CONTAINER=tec-membership-pg

# ── Teardown ─────────────────────────────────────────────────────────────────
if [[ "${1:-}" == "down" ]]; then
  docker rm -f "$LGTM_CONTAINER" "$PG_CONTAINER" >/dev/null 2>&1 || true
  echo "✓ removed $LGTM_CONTAINER and $PG_CONTAINER"
  exit 0
fi

command -v docker >/dev/null || { echo "docker is required"; exit 1; }

port_in_use() {  # 0 if something is already listening on 127.0.0.1:$1
  if (exec 3<>"/dev/tcp/127.0.0.1/$1") >/dev/null 2>&1; then return 0; fi
  return 1
}

ensure_container() {  # name, primary-port, docker run-args...
  local name="$1" port="$2"; shift 2
  if docker ps --format '{{.Names}}' | grep -qx "$name"; then
    echo "✓ $name already running"
  elif port_in_use "$port"; then
    echo "• port $port already in use — reusing the existing service (not starting $name)"
  else
    docker rm -f "$name" >/dev/null 2>&1 || true
    if docker run -d --name "$name" "$@" >/dev/null 2>&1; then
      echo "✓ started $name"
    else
      echo "• could not start $name (port $port likely already in use) — continuing, assuming an existing service is there"
    fi
  fi
}

# ── 1. Grafana LGTM stack (Grafana + Tempo + Mimir/Prometheus + Loki + collector) ──
ensure_container "$LGTM_CONTAINER" "$OTLP_HTTP_PORT" \
  -p "${GRAFANA_PORT}:3000" -p "${OTLP_GRPC_PORT}:4317" -p "${OTLP_HTTP_PORT}:4318" \
  grafana/otel-lgtm:latest

# ── 2. PostgreSQL ────────────────────────────────────────────────────────────
ensure_container "$PG_CONTAINER" "$PG_PORT" \
  -e POSTGRES_DB=membership_db -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=amos33 \
  -p "${PG_PORT}:5432" postgres:17

if docker ps --format '{{.Names}}' | grep -qx "$PG_CONTAINER"; then
  printf 'waiting for postgres'
  until docker exec "$PG_CONTAINER" pg_isready -U postgres >/dev/null 2>&1; do printf '.'; sleep 1; done
  echo " ready"
else
  echo "• using existing Postgres on localhost:${PG_PORT} (expects db 'membership_db', user 'postgres')"
fi

# ── 3. App config / secrets (override via env or a .env file) ────────────────
export SERVER_PORT="$APP_PORT"
export SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:${PG_PORT}/membership_db"
export OTEL_OTLP_ENDPOINT="http://localhost:${OTLP_HTTP_PORT}"
export DEPLOY_ENV="${DEPLOY_ENV:-local}"
export AUTH_JWT_SECRET="${AUTH_JWT_SECRET:-$(openssl rand -base64 48)}"
export AUTH_ADMIN_EMAIL="${AUTH_ADMIN_EMAIL:-admin@taraku-enlightenment.org}"
export AUTH_ADMIN_PASSWORD="${AUTH_ADMIN_PASSWORD:-AdminPass123!}"
export MAIL_USERNAME="${MAIL_USERNAME:-dummy}"   # dummy SMTP -> welcome emails fail (visible as logs/spans)
export MAIL_PASSWORD="${MAIL_PASSWORD:-dummy}"

cat <<EOF

  ─────────────────────────────────────────────────────────────
   Grafana   http://localhost:${GRAFANA_PORT}   (Explore → Tempo / Prometheus / Loki, service.name=membership)
   App       http://localhost:${APP_PORT}
   Admin     ${AUTH_ADMIN_EMAIL} / ${AUTH_ADMIN_PASSWORD}   (first login forces a password change)
   Health    http://localhost:${APP_PORT}/actuator/health
  ─────────────────────────────────────────────────────────────
   Stop the app with Ctrl+C. Tear down backends:  $0 down
   Full app (React SPA at /) is served from the packaged jar.

EOF

# ── 4. Build the jar if needed (REBUILD=1 forces), then run it ────────────────
JAR="$(ls -t target/membership-*.jar 2>/dev/null | head -1 || true)"
if [[ -z "$JAR" || "${REBUILD:-}" == "1" ]]; then
  echo "packaging jar (mvn package -DskipTests; builds the SPA)…"
  ./mvnw package -DskipTests
  JAR="$(ls -t target/membership-*.jar | head -1)"
fi

echo "running $JAR"
exec java -jar "$JAR"
