#!/bin/sh
set -eu
. /etc/carbon-integration.env
. /usr/local/lib/carbon-integration/runtime-common.sh
mkdir -p /home/ruoyi/logs /home/ruoyi/uploadPath/reports /home/ruoyi/report-work /data/redis
if ! pgrep -x redis-server >/dev/null 2>&1; then
  redis-server --daemonize yes --bind 127.0.0.1 --protected-mode yes --appendonly yes --dir /data/redis
fi
stop_java ruoyi-admin.jar
stop_java carbon-emission-model.jar
cd /home/ruoyi
export GOVIEW_AUDIT_SECRET
nohup env \
  SERVER_PORT=8081 SPRING_DATA_REDIS_HOST=127.0.0.1 SPRING_DATA_REDIS_PORT=6379 \
  SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_URL="jdbc:mysql://$DB_HOST:3306/carbon_emissions?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=GMT%2B8&allowPublicKeyRetrieval=true" \
  SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_USERNAME="$APP_DB_USERNAME" \
  SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_PASSWORD="$APP_DB_PASSWORD" \
  FILE_DOMAIN="$WEB_PUBLIC_URL/prod-api" FILE_PATH=/home/ruoyi/uploadPath \
  CARBON_REPORT_PYTHON=/opt/carbon-report-venv/bin/python \
  CARBON_REPORT_RENDERER_ROOT=/home/ruoyi/renderer \
  CARBON_REPORT_WORK_DIR=/home/ruoyi/report-work \
  CARBON_REPORT_ARTIFACT_DIR=/home/ruoyi/uploadPath/reports \
  CARBON_REPORT_SOFFICE_PATH="$REPORT_SOFFICE" \
  java -jar /home/ruoyi/ruoyi-admin.jar > /home/ruoyi/logs/ruoyi-admin.log 2>&1 < /dev/null &
nohup env \
  CARBON_SERVER_PORT=8082 \
  CARBON_DB_URL="jdbc:mysql://$DB_HOST:3306/carbon_emissions?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=utf8&useUnicode=true" \
  CARBON_DB_USERNAME="$APP_DB_USERNAME" CARBON_DB_PASSWORD="$APP_DB_PASSWORD" \
  SPRING_SQL_INIT_MODE=never CARBON_SQL_INIT_MODE=never SPRING_PROFILES_ACTIVE=job \
  RUOYI_SECURITY_ENABLED=true RUOYI_SECURITY_ENDPOINT=http://127.0.0.1:8081/internal/security/context \
  RUOYI_AUDIT_ENABLED=true RUOYI_AUDIT_ENDPOINT=http://127.0.0.1:8081/internal/carbon/audit/operation \
  PYTHON_PATH="$REPORT_PYTHON" STEAM_CALCULATOR_SCRIPT=/home/ruoyi/steam_calculator.py \
  java -jar /home/ruoyi/carbon-emission-model.jar > /home/ruoyi/logs/carbon-emission-model.log 2>&1 < /dev/null &
start_nginx
