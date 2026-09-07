#!/bin/sh
set -e

DB_HOST=${DB_HOST:-ruoyi-db}
MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD:-1275410782h}
WEB_PUBLIC_URL=${WEB_PUBLIC_URL:-http://127.0.0.1}
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
  command -v java >/dev/null 2>&1 || pkg_install java-17-openjdk-headless || pkg_install java-17-openjdk
  command -v nginx >/dev/null 2>&1 || pkg_install nginx
  command -v redis-server >/dev/null 2>&1 || pkg_install redis
  command -v python3 >/dev/null 2>&1 || pkg_install python3
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
  DIST_SRC=$(resolve_dist_dir "$DEPLOY_DIR/web-main/dist")
  CARBON_DIST_SRC=$(resolve_dist_dir "$DEPLOY_DIR/web-main/carbon-model-dist")
  mkdir -p /home/ruoyi/projects/ruoyi-ui /home/ruoyi/projects/carbon-model /home/ruoyi/uploadPath /home/ruoyi/logs
  cp -f "$DEPLOY_DIR/web-main/ruoyi-admin.jar" /home/ruoyi/ruoyi-admin.jar
  cp -f "$DEPLOY_DIR/web-main/carbon-emission-model.jar" /home/ruoyi/carbon-emission-model.jar
  cp -f "$DEPLOY_DIR/web-main/python/steam_calculator.py" /home/ruoyi/steam_calculator.py
  rm -rf /home/ruoyi/projects/ruoyi-ui/*
  cp -r "$DIST_SRC/." /home/ruoyi/projects/ruoyi-ui/
  test -f /home/ruoyi/projects/ruoyi-ui/index.html
  rm -rf /home/ruoyi/projects/carbon-model/*
  cp -r "$CARBON_DIST_SRC/." /home/ruoyi/projects/carbon-model/
  test -f /home/ruoyi/projects/carbon-model/index.html
  if [ -d "$DEPLOY_DIR/web-main/uploadPath" ]; then
    cp -r "$DEPLOY_DIR/web-main/uploadPath/." /home/ruoyi/uploadPath/
  fi
  chmod 755 /home /home/ruoyi /home/ruoyi/projects /home/ruoyi/projects/ruoyi-ui /home/ruoyi/projects/carbon-model /home/ruoyi/uploadPath
  chmod -R a+rX /home/ruoyi/projects/ruoyi-ui /home/ruoyi/projects/carbon-model /home/ruoyi/uploadPath
  cp -f "$DEPLOY_DIR/web-main/nginx.conf" /etc/nginx/nginx.conf
}

cat_start_script() {
  cat >/usr/local/bin/start-ruoyi-web.sh <<EOF
#!/bin/sh
set -e
mkdir -p /data/redis /home/ruoyi/uploadPath /home/ruoyi/logs
if ! pgrep -x redis-server >/dev/null 2>&1; then
  redis-server --daemonize yes --bind 0.0.0.0 --protected-mode no --dir /data/redis
fi
pkill -f 'ruoyi-admin.jar' 2>/dev/null || true
pkill -f 'carbon-emission-model.jar' 2>/dev/null || true
nohup env \\
  SERVER_PORT=8081 \\
  SPRING_DATA_REDIS_HOST=127.0.0.1 \\
  SPRING_DATA_REDIS_PORT=6379 \\
  SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_URL='jdbc:mysql://$DB_HOST:3306/carbon_emissions?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=GMT%2B8&allowPublicKeyRetrieval=true' \\
  SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_USERNAME=root \\
  SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_PASSWORD='$MYSQL_ROOT_PASSWORD' \\
  GOVIEW_AUDIT_SECRET='$GOVIEW_AUDIT_SECRET' \\
  FILE_DOMAIN='$WEB_PUBLIC_URL/prod-api' \\
  FILE_PATH=/home/ruoyi/uploadPath \\
  java -jar /home/ruoyi/ruoyi-admin.jar > /home/ruoyi/logs/ruoyi-admin.log 2>&1 &
nohup env \\
  SERVER_PORT=8082 \\
  SPRING_DATASOURCE_URL='jdbc:mysql://$DB_HOST:3306/carbon_emissions?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=utf8&useUnicode=true' \\
  SPRING_DATASOURCE_USERNAME=root \\
  SPRING_DATASOURCE_PASSWORD='$MYSQL_ROOT_PASSWORD' \\
  SPRING_SQL_INIT_MODE=always \\
  PYTHON_PATH=python3 \\
  STEAM_CALCULATOR_SCRIPT=/home/ruoyi/steam_calculator.py \\
  java -jar /home/ruoyi/carbon-emission-model.jar > /home/ruoyi/logs/carbon-emission-model.log 2>&1 &
nginx -t
nginx -s stop >/dev/null 2>&1 || true
nginx
EOF
  chmod +x /usr/local/bin/start-ruoyi-web.sh
}

ensure_runtime
install_payload
cat_start_script
sh /usr/local/bin/start-ruoyi-web.sh

echo "Main web container setup finished."
