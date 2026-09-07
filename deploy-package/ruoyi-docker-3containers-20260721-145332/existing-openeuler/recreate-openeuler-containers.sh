#!/bin/sh
set -e

DB_CONTAINER=${DB_CONTAINER:-ruoyi-db}
WEB_CONTAINER=${WEB_CONTAINER:-ruoyi-web}
SCREEN_CONTAINER=${SCREEN_CONTAINER:-ruoyi-screen}

OPENEULER_IMAGE=${OPENEULER_IMAGE:-hub.oepkgs.net/openeuler/openeuler:24.03-lts}
NETWORK_NAME=${NETWORK_NAME:-}

DB_SSH_PORT=${DB_SSH_PORT:-53210}
DB_PORT=${DB_PORT:-53214}
WEB_SSH_PORT=${WEB_SSH_PORT:-53220}
WEB_PORT=${WEB_PORT:-53221}
SCREEN_SSH_PORT=${SCREEN_SSH_PORT:-53230}
SCREEN_PORT=${SCREEN_PORT:-53231}

RECREATE_DB=${RECREATE_DB:-true}

image_required() {
  if ! docker image inspect "$OPENEULER_IMAGE" >/dev/null 2>&1; then
    echo "ERROR: openEuler image not found: $OPENEULER_IMAGE"
    echo "Please run one of the following commands first:"
    echo "  docker pull $OPENEULER_IMAGE"
    echo "or:"
    echo "  docker load -i openeuler-24.03-lts.tar"
    exit 1
  fi
}

first_network_of() {
  container=$1
  docker inspect -f '{{range $name, $net := .NetworkSettings.Networks}}{{$name}}{{"\n"}}{{end}}' "$container" 2>/dev/null | head -1
}

detect_network() {
  if [ -n "$NETWORK_NAME" ]; then
    return
  fi

  NETWORK_NAME=$(first_network_of "$DB_CONTAINER")
  if [ -z "$NETWORK_NAME" ]; then
    NETWORK_NAME=$(first_network_of "$WEB_CONTAINER")
  fi
  if [ -z "$NETWORK_NAME" ]; then
    NETWORK_NAME=$(first_network_of "$SCREEN_CONTAINER")
  fi
  if [ -z "$NETWORK_NAME" ]; then
    NETWORK_NAME=ruoyi-net
  fi

  if [ "$NETWORK_NAME" != "bridge" ] && ! docker network inspect "$NETWORK_NAME" >/dev/null 2>&1; then
    docker network create "$NETWORK_NAME" >/dev/null
  fi
}

backup_existing_container() {
  container=$1
  if docker inspect "$container" >/dev/null 2>&1; then
    old_name="$container-old-$(date +%Y%m%d%H%M%S)"
    echo "Renaming existing container $container to $old_name ..."
    docker stop "$container" >/dev/null 2>&1 || true
    docker rename "$container" "$old_name"
  fi
}

create_container() {
  name=$1
  ssh_mapping=$2
  service_mapping=$3

  echo "Creating openEuler container: $name ($ssh_mapping, $service_mapping)"
  docker run -dit \
    --name "$name" \
    --network "$NETWORK_NAME" \
    --privileged \
    --security-opt seccomp=unconfined \
    --restart unless-stopped \
    -p "$ssh_mapping" \
    -p "$service_mapping" \
    "$OPENEULER_IMAGE" \
    /bin/bash
}

image_required
detect_network

echo "Using openEuler image: $OPENEULER_IMAGE"
echo "Using Docker network: $NETWORK_NAME"

if [ "$RECREATE_DB" = "true" ]; then
  backup_existing_container "$DB_CONTAINER"
  create_container "$DB_CONTAINER" "$DB_SSH_PORT:22" "$DB_PORT:3306"
else
  echo "Keeping existing database container: $DB_CONTAINER"
  echo "WARN: its SSH/MySQL mappings are not changed by this script."
fi

backup_existing_container "$WEB_CONTAINER"
backup_existing_container "$SCREEN_CONTAINER"

create_container "$WEB_CONTAINER" "$WEB_SSH_PORT:22" "$WEB_PORT:80"
create_container "$SCREEN_CONTAINER" "$SCREEN_SSH_PORT:22" "$SCREEN_PORT:80"

echo "Containers are ready."
docker ps --filter "name=$DB_CONTAINER" --filter "name=$WEB_CONTAINER" --filter "name=$SCREEN_CONTAINER"
