#!/bin/sh
set -e

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PACKAGE_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)

DB_CONTAINER=${DB_CONTAINER:-ruoyi-db}
WEB_CONTAINER=${WEB_CONTAINER:-ruoyi-web}
SCREEN_CONTAINER=${SCREEN_CONTAINER:-ruoyi-screen}

MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD:-1275410782h}
CONTAINER_SSH_PASSWORD=${CONTAINER_SSH_PASSWORD:-}
GOVIEW_AUDIT_SECRET=${GOVIEW_AUDIT_SECRET:-}
WEB_PUBLIC_URL=${WEB_PUBLIC_URL:-http://127.0.0.1:53221}
SCREEN_PUBLIC_URL=${SCREEN_PUBLIC_URL:-http://127.0.0.1:53231}
BUSINESS_SCHEMA_ONLY=${BUSINESS_SCHEMA_ONLY:-false}
WEB_HOST_PORT=${WEB_HOST_PORT:-53221}
SCREEN_HOST_PORT=${SCREEN_HOST_PORT:-53231}
SMOKE_TEST_ROUNDS=${SMOKE_TEST_ROUNDS:-3}
SMOKE_TEST_INTERVAL=${SMOKE_TEST_INTERVAL:-5}

DEPLOY_DIR=/opt/ruoyi-deploy

check_container() {
  name=$1
  docker inspect "$name" >/dev/null 2>&1 || {
    echo "ERROR: container not found: $name"
    exit 1
  }
  docker exec "$name" sh -c "grep -qi openeuler /etc/os-release" || {
    echo "ERROR: $name is not an openEuler container."
    echo "Run: docker exec $name cat /etc/os-release"
    exit 1
  }
}

copy_dir() {
  src=$1
  container=$2
  dest=$3
  docker exec "$container" sh -c "mkdir -p '$dest'"
  docker cp "$src/." "$container:$dest/"
}

copy_file() {
  src=$1
  container=$2
  dest=$3
  dest_dir=$(dirname "$dest")
  docker exec "$container" sh -c "mkdir -p '$dest_dir'"
  docker cp "$src" "$container:$dest"
}

setup_container_ssh() {
  container=$1
  docker exec "$container" sh -c "mkdir -p '$DEPLOY_DIR/scripts'"
  copy_file "$SCRIPT_DIR/scripts/ssh-container-setup.sh" "$container" "$DEPLOY_DIR/scripts/ssh-container-setup.sh"
  docker exec \
    -e CONTAINER_SSH_PASSWORD="$CONTAINER_SSH_PASSWORD" \
    "$container" sh "$DEPLOY_DIR/scripts/ssh-container-setup.sh"
}

echo "Checking existing openEuler containers..."
check_container "$DB_CONTAINER"
check_container "$WEB_CONTAINER"
check_container "$SCREEN_CONTAINER"

if [ -z "$CONTAINER_SSH_PASSWORD" ]; then
  echo "ERROR: set CONTAINER_SSH_PASSWORD before deployment."
  echo "Example: CONTAINER_SSH_PASSWORD='your-strong-password' sh host-deploy-existing-openeuler.sh"
  exit 1
fi

if [ -z "$GOVIEW_AUDIT_SECRET" ]; then
  echo "ERROR: set GOVIEW_AUDIT_SECRET before deployment."
  echo "Generate one with: openssl rand -hex 32"
  exit 1
fi

echo "Configuring SSH in all three containers..."
setup_container_ssh "$DB_CONTAINER"
setup_container_ssh "$WEB_CONTAINER"
setup_container_ssh "$SCREEN_CONTAINER"

DB_HOST=${DB_HOST:-$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' "$DB_CONTAINER")}
if [ -z "$DB_HOST" ]; then
  DB_HOST=$DB_CONTAINER
fi

echo "Database host used by web container: $DB_HOST"

WEB_INTERNAL_HOST=${WEB_INTERNAL_HOST:-$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' "$WEB_CONTAINER")}
if [ -z "$WEB_INTERNAL_HOST" ]; then
  WEB_INTERNAL_HOST=$WEB_CONTAINER
fi
RUOYI_AUDIT_URL=${RUOYI_AUDIT_URL:-http://$WEB_INTERNAL_HOST:8081/internal/goview/audit}
echo "RuoYi audit endpoint used by screen container: $RUOYI_AUDIT_URL"

if [ "${SKIP_DB_DEPLOY:-false}" = "true" ]; then
  echo "Skipping database import (SKIP_DB_DEPLOY=true)."
else
  echo "Deploying database payload to $DB_CONTAINER..."
  docker exec "$DB_CONTAINER" sh -c "mkdir -p '$DEPLOY_DIR/scripts' '$DEPLOY_DIR/mysql/db'"
  copy_file "$SCRIPT_DIR/scripts/db-container-setup.sh" "$DB_CONTAINER" "$DEPLOY_DIR/scripts/db-container-setup.sh"
  copy_dir "$PACKAGE_ROOT/mysql/db" "$DB_CONTAINER" "$DEPLOY_DIR/mysql/db"
  docker exec \
    -e MYSQL_ROOT_PASSWORD="$MYSQL_ROOT_PASSWORD" \
    -e BUSINESS_SCHEMA_ONLY="$BUSINESS_SCHEMA_ONLY" \
    "$DB_CONTAINER" sh "$DEPLOY_DIR/scripts/db-container-setup.sh"
fi

echo "Deploying main web payload to $WEB_CONTAINER..."
docker exec "$WEB_CONTAINER" sh -c "mkdir -p '$DEPLOY_DIR/scripts' '$DEPLOY_DIR/web-main' '$DEPLOY_DIR/web-main/python'"
copy_file "$SCRIPT_DIR/scripts/web-container-setup.sh" "$WEB_CONTAINER" "$DEPLOY_DIR/scripts/web-container-setup.sh"
copy_file "$PACKAGE_ROOT/web-main/jar/ruoyi-admin.jar" "$WEB_CONTAINER" "$DEPLOY_DIR/web-main/ruoyi-admin.jar"
copy_file "$PACKAGE_ROOT/web-main/jar/carbon-emission-model.jar" "$WEB_CONTAINER" "$DEPLOY_DIR/web-main/carbon-emission-model.jar"
copy_file "$PACKAGE_ROOT/web-main/python/steam_calculator.py" "$WEB_CONTAINER" "$DEPLOY_DIR/web-main/python/steam_calculator.py"
copy_file "$PACKAGE_ROOT/web-main/conf/nginx.conf" "$WEB_CONTAINER" "$DEPLOY_DIR/web-main/nginx.conf"
copy_dir "$PACKAGE_ROOT/web-main/html/dist" "$WEB_CONTAINER" "$DEPLOY_DIR/web-main/dist"
copy_dir "$PACKAGE_ROOT/web-main/html/carbon-model" "$WEB_CONTAINER" "$DEPLOY_DIR/web-main/carbon-model-dist"
if [ -d "$PACKAGE_ROOT/web-main/uploadPath" ]; then
  copy_dir "$PACKAGE_ROOT/web-main/uploadPath" "$WEB_CONTAINER" "$DEPLOY_DIR/web-main/uploadPath"
fi
docker exec \
  -e DB_HOST="$DB_HOST" \
  -e MYSQL_ROOT_PASSWORD="$MYSQL_ROOT_PASSWORD" \
  -e GOVIEW_AUDIT_SECRET="$GOVIEW_AUDIT_SECRET" \
  -e WEB_PUBLIC_URL="$WEB_PUBLIC_URL" \
  "$WEB_CONTAINER" sh "$DEPLOY_DIR/scripts/web-container-setup.sh"

echo "Deploying screen payload to $SCREEN_CONTAINER..."
docker exec "$SCREEN_CONTAINER" sh -c "mkdir -p '$DEPLOY_DIR/scripts' '$DEPLOY_DIR/web-screen'"
copy_file "$SCRIPT_DIR/scripts/screen-container-setup.sh" "$SCREEN_CONTAINER" "$DEPLOY_DIR/scripts/screen-container-setup.sh"
copy_file "$PACKAGE_ROOT/web-screen/jar/goview.war" "$SCREEN_CONTAINER" "$DEPLOY_DIR/web-screen/goview.war"
copy_file "$PACKAGE_ROOT/web-screen/conf/nginx.conf" "$SCREEN_CONTAINER" "$DEPLOY_DIR/web-screen/nginx.conf"
copy_dir "$PACKAGE_ROOT/web-screen/html/dist" "$SCREEN_CONTAINER" "$DEPLOY_DIR/web-screen/dist"
copy_dir "$PACKAGE_ROOT/web-screen/sqllite" "$SCREEN_CONTAINER" "$DEPLOY_DIR/web-screen/sqllite"
if [ -d "$PACKAGE_ROOT/web-screen/upload" ]; then
  copy_dir "$PACKAGE_ROOT/web-screen/upload" "$SCREEN_CONTAINER" "$DEPLOY_DIR/web-screen/upload"
fi
docker exec \
  -e SCREEN_PUBLIC_URL="$SCREEN_PUBLIC_URL" \
  -e RUOYI_AUDIT_URL="$RUOYI_AUDIT_URL" \
  -e GOVIEW_AUDIT_SECRET="$GOVIEW_AUDIT_SECRET" \
  "$SCREEN_CONTAINER" sh "$DEPLOY_DIR/scripts/screen-container-setup.sh"

if [ "${SKIP_SMOKE_TEST:-false}" != "true" ]; then
  echo "Running integrated smoke tests..."
  WEB_CONTAINER="$WEB_CONTAINER" \
    SCREEN_CONTAINER="$SCREEN_CONTAINER" \
    DB_CONTAINER="$DB_CONTAINER" \
    WEB_HOST_PORT="$WEB_HOST_PORT" \
    SCREEN_HOST_PORT="$SCREEN_HOST_PORT" \
    SMOKE_TEST_ROUNDS="$SMOKE_TEST_ROUNDS" \
    SMOKE_TEST_INTERVAL="$SMOKE_TEST_INTERVAL" \
    sh "$SCRIPT_DIR/scripts/integration-smoke-test.sh"
fi

echo "Deployment finished."
echo "Main web: $WEB_PUBLIC_URL"
echo "Screen:   $SCREEN_PUBLIC_URL"
