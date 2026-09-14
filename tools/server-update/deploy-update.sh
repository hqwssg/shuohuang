#!/bin/bash
set -Eeuo pipefail
PACKAGE_ROOT=$(cd -- "$(dirname -- "$0")" && pwd)
DB_CONTAINER=${DB_CONTAINER:-ruoyi-db}
WEB_CONTAINER=${WEB_CONTAINER:-ruoyi-web}
SCREEN_CONTAINER=${SCREEN_CONTAINER:-ruoyi-screen}
WEB_HOST_PORT=${WEB_HOST_PORT:-53221}
SCREEN_HOST_PORT=${SCREEN_HOST_PORT:-53231}
APPLY_PERMISSION_RESET=${APPLY_PERMISSION_RESET:-false}
UPDATE_ID=$(date +%Y%m%d-%H%M%S)
BACKUP_DIR=${BACKUP_DIR:-/opt/ruoyi-backups/$UPDATE_ID}
STAGING=/opt/carbon-update/$UPDATE_ID
TEMP_DIR=$(mktemp -d)
PAUSED_SCREEN=false
cleanup() {
  if [ "$PAUSED_SCREEN" = true ]; then docker unpause "$SCREEN_CONTAINER" >/dev/null || true; fi
  rm -rf -- "$TEMP_DIR"
}
trap cleanup EXIT
trap 'echo "ERROR: update failed at line $LINENO. Backups: $BACKUP_DIR. Do not reinitialize the database." >&2' ERR

if [ -z "${MYSQL_ROOT_PASSWORD:-}" ]; then
  read -rsp 'MySQL/MariaDB root password: ' MYSQL_ROOT_PASSWORD
  echo
fi
case "${1:-}" in ''|--check) ;; *) echo 'Usage: bash deploy-update.sh [--check]' >&2; exit 1 ;; esac
echo "Verifying package: $PACKAGE_ROOT"
(cd "$PACKAGE_ROOT" && sha256sum -c SHA256SUMS > "$TEMP_DIR/checksums.log")
for container in "$DB_CONTAINER" "$WEB_CONTAINER" "$SCREEN_CONTAINER"; do
  test "$(docker inspect -f '{{.State.Running}}' "$container")" = true
done
docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'

mysql_query() {
  docker exec -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" "$DB_CONTAINER" \
    mysql --protocol=TCP -h127.0.0.1 -P3306 -uroot --default-character-set=utf8mb4 -N -B carbon_emissions -e "$1"
}
mysql_query 'SELECT VERSION(); SELECT COUNT(*) FROM sys_user; SELECT COUNT(*) FROM emission_template;'
DB_HOST=${DB_HOST:-$(docker inspect -f '{{range .NetworkSettings.Networks}}{{println .IPAddress}}{{end}}' "$DB_CONTAINER" | sed -n '1p')}
WEB_INTERNAL_HOST=${WEB_INTERNAL_HOST:-$(docker inspect -f '{{range .NetworkSettings.Networks}}{{println .IPAddress}}{{end}}' "$WEB_CONTAINER" | sed -n '1p')}
test -n "$DB_HOST"
test -n "$WEB_INTERNAL_HOST"
echo "Database internal address: $DB_HOST:3306"
echo "Management internal address: $WEB_INTERNAL_HOST:8081"

if [ "$APPLY_PERMISSION_RESET" = true ]; then
  # Never turn unrelated real accounts into fixed-ID sample accounts.
  collisions=$(mysql_query "SELECT COUNT(*) FROM sys_user WHERE user_id BETWEEN 2 AND 9 AND NOT ((user_id=2 AND user_name IN ('ry','yuanping_admin')) OR (user_id=3 AND user_name='suning_admin') OR (user_id=4 AND user_name='locomotive_admin') OR (user_id=5 AND user_name='carbon_accountant') OR (user_id=6 AND user_name='data_entry') OR (user_id=7 AND user_name='data_reviewer') OR (user_id=8 AND user_name='carbon_auditor') OR (user_id=9 AND user_name='screen_operator'));")
  role_collisions=$(mysql_query "SELECT COUNT(*) FROM sys_role WHERE role_id BETWEEN 1 AND 8 AND role_key NOT IN ('admin','common','company_admin','region_admin','department_admin','carbon_accountant','carbon_data_entry','carbon_reviewer','carbon_auditor','screen_operator');")
  department_users=$(mysql_query 'SELECT COUNT(*) FROM sys_user WHERE user_id > 9 AND dept_id BETWEEN 100 AND 109;')
  post_collisions=$(mysql_query "SELECT COUNT(*) FROM sys_post WHERE post_id BETWEEN 1 AND 8 AND post_code NOT IN ('ceo','se','hr','user','company_manager','region_manager','department_manager','carbon_accounting','data_entry','data_review','carbon_audit','screen_ops');")
  notice_collisions=$(mysql_query "SELECT COUNT(*) FROM sys_notice WHERE notice_id IN (1,2,3) AND notice_title NOT LIKE '%若依%' AND notice_content NOT LIKE '%ruoyi.vip%' AND notice_title <> '碳排放管理系统使用通知';")
  if [ "$collisions" != 0 ] || [ "$role_collisions" != 0 ] || [ "$department_users" != 0 ] || [ "$post_collisions" != 0 ] || [ "$notice_collisions" != 0 ]; then
    echo 'ERROR: permission reset would overwrite real users/roles or remove a used department.' >&2
    echo 'Stop here and remap the optional SQL to unused IDs; do not bypass this check.' >&2
    exit 1
  fi
fi
docker exec "$WEB_CONTAINER" sh -c 'java -version; command -v nginx; command -v redis-server; command -v pgrep'
docker exec "$SCREEN_CONTAINER" sh -c 'java -version; command -v nginx; command -v pgrep; test -f /home/goview/sqllite/goview.db'
if [ "${1:-}" = --check ]; then
  echo 'Preflight passed. No application or database updates have been performed.'
  echo 'For Python, use REPORT_PYTHON=python3.11 if the container default is older than 3.10.'
  exit 0
fi
: "${WEB_PUBLIC_URL:?Set WEB_PUBLIC_URL to the browser-visible management URL}"
: "${SCREEN_PUBLIC_URL:?Set SCREEN_PUBLIC_URL to the browser-visible screen URL}"
WEB_PUBLIC_URL=${WEB_PUBLIC_URL%/}
SCREEN_PUBLIC_URL=${SCREEN_PUBLIC_URL%/}
APP_DB_USERNAME=${APP_DB_USERNAME:-root}
APP_DB_PASSWORD=${APP_DB_PASSWORD:-$MYSQL_ROOT_PASSWORD}
REPORT_PYTHON=${REPORT_PYTHON:-python3}
if [ -z "${GOVIEW_AUDIT_SECRET:-}" ]; then
  GOVIEW_AUDIT_SECRET=$(openssl rand -hex 32)
fi
export GOVIEW_AUDIT_SECRET

backup_application() {
  local container=$1 home=$2 filename=$3 start=$4
  docker exec "$container" sh -c '
    set -e
    set -- "$1" etc/nginx
    for optional in etc/carbon-integration.env usr/local/lib/carbon-integration "$2"; do
      if [ -e "/$optional" ]; then set -- "$@" "$optional"; fi
    done
    tar -C / -czf - "$@"
  ' sh "$home" "$start" > "$BACKUP_DIR/$filename"
}
echo 'Preparing runtimes before applying database migrations...'
for container in "$WEB_CONTAINER" "$SCREEN_CONTAINER"; do
  docker exec "$container" mkdir -p "$STAGING/scripts"
  docker cp "$PACKAGE_ROOT/scripts/." "$container:$STAGING/scripts/"
done
docker exec "$WEB_CONTAINER" mkdir -p "$STAGING/web-main"
docker cp "$PACKAGE_ROOT/web-main/." "$WEB_CONTAINER:$STAGING/web-main/"
docker exec "$SCREEN_CONTAINER" mkdir -p "$STAGING/web-screen"
docker cp "$PACKAGE_ROOT/web-screen/." "$SCREEN_CONTAINER:$STAGING/web-screen/"
docker exec -e REPORT_PYTHON="$REPORT_PYTHON" "$WEB_CONTAINER" sh "$STAGING/scripts/container-update.sh" web prepare "$STAGING"
docker exec "$SCREEN_CONTAINER" sh "$STAGING/scripts/container-update.sh" screen prepare "$STAGING"
docker exec -e DB_HOST="$DB_HOST" "$WEB_CONTAINER" /opt/carbon-report-venv/bin/python -c \
  'import os,socket; s=socket.create_connection((os.environ["DB_HOST"],3306),10); s.close()'
docker exec "$SCREEN_CONTAINER" curl -sS --connect-timeout 10 -o /dev/null "http://$WEB_INTERNAL_HOST/"
REPORT_SOFFICE=$(docker exec "$WEB_CONTAINER" sh -c 'command -v libreoffice || command -v soffice')

echo 'Entering maintenance window: stop business writers before backup/migrations.'
docker exec "$WEB_CONTAINER" sh -c '. "$1"; stop_java ruoyi-admin.jar; stop_java carbon-emission-model.jar' sh "$STAGING/scripts/runtime-common.sh"
docker exec "$SCREEN_CONTAINER" sh -c '. "$1"; stop_java goview.war' sh "$STAGING/scripts/runtime-common.sh"
echo "Backing up production state to $BACKUP_DIR"
umask 077
mkdir -p "$BACKUP_DIR"
docker exec -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" "$DB_CONTAINER" \
  mysqldump --protocol=TCP -h127.0.0.1 -P3306 -uroot --default-character-set=utf8mb4 \
  --single-transaction --routines --triggers carbon_emissions > "$BACKUP_DIR/database.sql.partial"
test -s "$BACKUP_DIR/database.sql.partial"
mv "$BACKUP_DIR/database.sql.partial" "$BACKUP_DIR/database.sql"
backup_application "$WEB_CONTAINER" home/ruoyi web.tgz usr/local/bin/start-ruoyi-web.sh
backup_application "$SCREEN_CONTAINER" home/goview screen.tgz usr/local/bin/start-ruoyi-screen.sh
# Retain the SQLite database together with any WAL sidecars, without writers.
docker pause "$SCREEN_CONTAINER" >/dev/null
PAUSED_SCREEN=true
docker cp "$SCREEN_CONTAINER:/home/goview/sqllite" "$BACKUP_DIR/goview-sqllite"
docker unpause "$SCREEN_CONTAINER" >/dev/null
PAUSED_SCREEN=false

import_file() {
  local file=$1 base
  base=$(basename "$file")
  echo "Applying $base"
  docker cp "$file" "$DB_CONTAINER:/tmp/$base"
  docker exec -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" "$DB_CONTAINER" sh -c \
    'mysql --protocol=TCP -h127.0.0.1 -P3306 -uroot --default-character-set=utf8mb4 carbon_emissions < "$1"' sh "/tmp/$base" \
    > "$BACKUP_DIR/$base.log" 2>&1 || { cat "$BACKUP_DIR/$base.log" >&2; return 1; }
}
for file in "$PACKAGE_ROOT"/mysql/migrations/*.sql; do import_file "$file"; done
if [ "$APPLY_PERMISSION_RESET" = true ]; then
  import_file "$PACKAGE_ROOT/mysql/optional/60_cleanup_and_test_users.sql"
else
  echo 'WARNING: fixed role renumbering, legacy sample cleanup and test users were skipped.'
fi

write_variable() {
  printf '%s=' "$1"
  printf "'%s'\n" "$(printf '%s' "$2" | sed "s/'/'\"'\"'/g")"
}
for mode in web screen; do
  env_file="$TEMP_DIR/$mode.env"
  {
    write_variable GOVIEW_AUDIT_SECRET "$GOVIEW_AUDIT_SECRET"
    if [ "$mode" = web ]; then
      write_variable DB_HOST "$DB_HOST"
      write_variable APP_DB_USERNAME "$APP_DB_USERNAME"
      write_variable APP_DB_PASSWORD "$APP_DB_PASSWORD"
      write_variable WEB_PUBLIC_URL "$WEB_PUBLIC_URL"
      write_variable REPORT_PYTHON "$REPORT_PYTHON"
      write_variable REPORT_SOFFICE "$REPORT_SOFFICE"
    else
      write_variable SCREEN_PUBLIC_URL "$SCREEN_PUBLIC_URL"
      write_variable WEB_INTERNAL_HOST "$WEB_INTERNAL_HOST"
    fi
  } > "$env_file"
  if [ "$mode" = web ]; then container=$WEB_CONTAINER; else container=$SCREEN_CONTAINER; fi
  docker cp "$env_file" "$container:/etc/carbon-integration.env"
  docker exec "$container" chmod 600 /etc/carbon-integration.env
  docker exec -e UPDATE_ID="$UPDATE_ID" "$container" sh "$STAGING/scripts/container-update.sh" "$mode" apply "$STAGING"
done

wait_http() {
  local url=$1 codes=$2 code round
  for round in $(seq 1 180); do
    code=$(curl -sS --connect-timeout 3 -m 5 -o /dev/null -w '%{http_code}' "$url" 2>/dev/null || true)
    case ",$codes," in *",$code,"*) echo "PASS $url -> $code"; return 0 ;; esac
    sleep 1
  done
  echo "FAIL $url -> ${code:-connection-error}. Check application logs." >&2
  return 1
}
wait_http "http://127.0.0.1:$WEB_HOST_PORT/" 200
wait_http "http://127.0.0.1:$WEB_HOST_PORT/prod-api/code" 200
wait_http "http://127.0.0.1:$WEB_HOST_PORT/carbon-model/" 200
wait_http "http://127.0.0.1:$WEB_HOST_PORT/api/template/list" 200,401,403
wait_http "http://127.0.0.1:$SCREEN_HOST_PORT/" 200
wait_http "http://127.0.0.1:$SCREEN_HOST_PORT/api/goview/sys/getOssInfo" 200
docker exec "$SCREEN_CONTAINER" curl -fsS --connect-timeout 10 -o /dev/null "http://$WEB_INTERNAL_HOST:8081/code"
asset=$(curl -sS "http://127.0.0.1:$WEB_HOST_PORT/carbon-model/" | sed -n 's/.*src="\([^"]*\.js\)".*/\1/p' | head -1)
test -n "$asset"
asset_type=$(curl -fsS "http://127.0.0.1:$WEB_HOST_PORT$asset" -o /dev/null -w '%{content_type}')
case "$asset_type" in *javascript*) ;; *) echo "ERROR: model asset returned $asset_type, not JavaScript" >&2; exit 1 ;; esac
docker exec "$WEB_CONTAINER" sh -c 'pgrep -x java; nginx -t'
docker exec "$SCREEN_CONTAINER" sh -c 'pgrep -x java; nginx -t'
mysql_query 'SELECT role_id,role_name,role_key FROM sys_role ORDER BY role_id; SELECT COUNT(*) AS operation_logs FROM sys_oper_log;'
echo 'Update and HTTP health checks completed. Authentication/authorization and report generation must also be checked in the browser.'
echo "Management: $WEB_PUBLIC_URL"
echo "Screen:     $SCREEN_PUBLIC_URL"
echo "Backups:    $BACKUP_DIR"
