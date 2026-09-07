#!/bin/sh
set -u

DB_CONTAINER=${DB_CONTAINER:-ruoyi-db}
WEB_CONTAINER=${WEB_CONTAINER:-ruoyi-web}
SCREEN_CONTAINER=${SCREEN_CONTAINER:-ruoyi-screen}
WEB_HOST_PORT=${WEB_HOST_PORT:-53221}
SCREEN_HOST_PORT=${SCREEN_HOST_PORT:-53231}
SMOKE_TEST_ROUNDS=${SMOKE_TEST_ROUNDS:-3}
SMOKE_TEST_INTERVAL=${SMOKE_TEST_INTERVAL:-5}
READY_TIMEOUT=${READY_TIMEOUT:-90}
TEST_HOST=${TEST_HOST:-127.0.0.1}

WEB_BASE_URL="http://$TEST_HOST:$WEB_HOST_PORT"
SCREEN_BASE_URL="http://$TEST_HOST:$SCREEN_HOST_PORT"
FAILURES=0

http_code() {
  curl -sS -m 10 -o /dev/null -w '%{http_code}' "$1" 2>/dev/null || true
}

wait_for_http() {
  name=$1
  url=$2
  elapsed=0

  while [ "$elapsed" -lt "$READY_TIMEOUT" ]; do
    code=$(http_code "$url")
    if [ "$code" = "200" ]; then
      echo "READY  $name ($url)"
      return 0
    fi
    sleep 1
    elapsed=$((elapsed + 1))
  done

  echo "FAIL   $name did not become ready in ${READY_TIMEOUT}s ($url)"
  FAILURES=$((FAILURES + 1))
  return 1
}

check_http() {
  name=$1
  url=$2
  code=$(http_code "$url")
  if [ "$code" = "200" ]; then
    echo "PASS   $name -> HTTP 200"
  else
    echo "FAIL   $name -> HTTP ${code:-connection-error}"
    FAILURES=$((FAILURES + 1))
  fi
}

check_process() {
  container=$1
  name=$2
  pattern=$3
  if docker exec "$container" sh -c "pgrep -f '$pattern' >/dev/null 2>&1"; then
    echo "PASS   $container: $name is running"
  else
    echo "FAIL   $container: $name is not running"
    FAILURES=$((FAILURES + 1))
  fi
}

check_model_asset() {
  asset_path=$(curl -sS -m 10 "$WEB_BASE_URL/carbon-model/" 2>/dev/null |
    sed -n 's/.*src="\([^"]*\.js\)".*/\1/p' |
    head -1)

  if [ -z "$asset_path" ]; then
    echo "FAIL   carbon model JavaScript asset was not found in index.html"
    FAILURES=$((FAILURES + 1))
    return
  fi

  content_type=$(curl -sS -m 15 -o /dev/null -w '%{content_type}' \
    "$WEB_BASE_URL$asset_path" 2>/dev/null || true)
  case "$content_type" in
    *javascript*)
      echo "PASS   carbon model asset $asset_path ($content_type)"
      ;;
    *)
      echo "FAIL   carbon model asset $asset_path returned ${content_type:-no-content-type}"
      FAILURES=$((FAILURES + 1))
      ;;
  esac
}

command -v curl >/dev/null 2>&1 || {
  echo "ERROR: curl is required on the Docker host."
  exit 1
}

if [ "${SKIP_PROCESS_CHECKS:-false}" != "true" ]; then
  command -v docker >/dev/null 2>&1 || {
    echo "ERROR: docker is required on the Docker host."
    exit 1
  }
fi

wait_for_http "main web" "$WEB_BASE_URL/" || true
wait_for_http "RuoYi API" "$WEB_BASE_URL/prod-api/code" || true
wait_for_http "carbon model API" "$WEB_BASE_URL/api/template/list" || true
wait_for_http "GoView web" "$SCREEN_BASE_URL/" || true
wait_for_http "GoView API" "$SCREEN_BASE_URL/api/goview/sys/getOssInfo" || true

if [ "${SKIP_PROCESS_CHECKS:-false}" != "true" ]; then
  check_process "$DB_CONTAINER" "MySQL/MariaDB" 'mysqld|mariadbd'
  check_process "$WEB_CONTAINER" "RuoYi backend" 'ruoyi-admin.jar'
  check_process "$WEB_CONTAINER" "carbon model backend" 'carbon-emission-model.jar'
  check_process "$WEB_CONTAINER" "Nginx" 'nginx'
  check_process "$SCREEN_CONTAINER" "GoView backend" 'goview.war'
  check_process "$SCREEN_CONTAINER" "Nginx" 'nginx'
fi
check_model_asset

round=1
while [ "$round" -le "$SMOKE_TEST_ROUNDS" ]; do
  echo "Smoke-test round $round/$SMOKE_TEST_ROUNDS"
  check_http "main web" "$WEB_BASE_URL/"
  check_http "RuoYi API" "$WEB_BASE_URL/prod-api/code"
  check_http "carbon model page" "$WEB_BASE_URL/carbon-model/"
  check_http "carbon model API" "$WEB_BASE_URL/api/template/list"
  check_http "GoView web" "$SCREEN_BASE_URL/"
  check_http "GoView API" "$SCREEN_BASE_URL/api/goview/sys/getOssInfo"

  if [ "$round" -lt "$SMOKE_TEST_ROUNDS" ]; then
    sleep "$SMOKE_TEST_INTERVAL"
  fi
  round=$((round + 1))
done

if [ "$FAILURES" -ne 0 ]; then
  echo "Integrated smoke test failed: $FAILURES check(s) failed."
  exit 1
fi

echo "Integrated smoke test passed."
