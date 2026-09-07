#!/bin/sh
set -e

SCREEN_PUBLIC_URL=${SCREEN_PUBLIC_URL:-http://127.0.0.1:3000}
RUOYI_AUDIT_URL=${RUOYI_AUDIT_URL:-http://ruoyi-web:8081/internal/goview/audit}
GOVIEW_AUDIT_SECRET=${GOVIEW_AUDIT_SECRET:-}
DEPLOY_DIR=${DEPLOY_DIR:-/opt/ruoyi-deploy}

pkg_install() {
  if command -v dnf >/dev/null 2>&1; then
    dnf install -y "$@"
  elif command -v yum >/dev/null 2>&1; then
    yum install -y "$@"
  else
    echo "ERROR: dnf/yum not found in this openEuler container."
    exit 1
  fi
}

ensure_runtime() {
  if ! command -v java >/dev/null 2>&1; then
    pkg_install java-1.8.0-openjdk-headless || pkg_install java-1.8.0-openjdk || pkg_install java-17-openjdk-headless || pkg_install java-17-openjdk
  fi
  command -v nginx >/dev/null 2>&1 || pkg_install nginx
}

resolve_dist_dir() {
  src=$1
  if [ -f "$src/index.html" ]; then
    echo "$src"
    return 0
  fi
  if [ -f "$src/dist/index.html" ]; then
    echo "$src/dist"
    return 0
  fi

  echo "ERROR: frontend index.html not found under $src" >&2
  find "$src" -maxdepth 3 -name index.html -print >&2 2>/dev/null || true
  exit 1
}

install_payload() {
  DIST_SRC=$(resolve_dist_dir "$DEPLOY_DIR/web-screen/dist")
  mkdir -p /home/goview/projects/goview /home/goview/sqllite /home/goview/upload /home/goview/logs
  cp -f "$DEPLOY_DIR/web-screen/goview.war" /home/goview/goview.war
  rm -rf /home/goview/projects/goview/*
  cp -r "$DIST_SRC/." /home/goview/projects/goview/
  test -f /home/goview/projects/goview/index.html
  if [ -f /home/goview/sqllite/goview.db ]; then
    echo "Keeping existing GoView database: /home/goview/sqllite/goview.db"
  else
    cp -r "$DEPLOY_DIR/web-screen/sqllite/." /home/goview/sqllite/
  fi
  if [ -d "$DEPLOY_DIR/web-screen/upload" ]; then
    cp -r "$DEPLOY_DIR/web-screen/upload/." /home/goview/upload/
  fi
  chmod 755 /home /home/goview /home/goview/projects /home/goview/projects/goview /home/goview/sqllite /home/goview/upload
  chmod -R a+rX /home/goview/projects/goview /home/goview/sqllite /home/goview/upload
  cp -f "$DEPLOY_DIR/web-screen/nginx.conf" /etc/nginx/nginx.conf
}

cat_start_script() {
  cat >/usr/local/bin/start-ruoyi-screen.sh <<EOF
#!/bin/sh
set -e
mkdir -p /home/goview/sqllite /home/goview/upload /home/goview/logs
pkill -f 'goview.war' 2>/dev/null || true
nohup env \\
  SERVER_PORT=8083 \\
  SPRING_DATASOURCE_URL='jdbc:sqlite:/home/goview/sqllite/goview.db' \\
  V2_XNLJMAP_OSS='file:/home/goview/upload/' \\
  V2_FILEURL=/home/goview/upload \\
  V2_HTTPURL='$SCREEN_PUBLIC_URL/' \\
  RUOYI_AUDIT_ENABLED=true \\
  RUOYI_AUDIT_URL='$RUOYI_AUDIT_URL' \\
  GOVIEW_AUDIT_SECRET='$GOVIEW_AUDIT_SECRET' \\
  java -jar /home/goview/goview.war > /home/goview/logs/goview.log 2>&1 &
nginx -t
nginx -s stop >/dev/null 2>&1 || true
nginx
EOF
  chmod +x /usr/local/bin/start-ruoyi-screen.sh
}

ensure_runtime
install_payload
cat_start_script
sh /usr/local/bin/start-ruoyi-screen.sh

echo "Screen container setup finished."
