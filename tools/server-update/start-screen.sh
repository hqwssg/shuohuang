#!/bin/sh
set -eu
. /etc/carbon-integration.env
. /usr/local/lib/carbon-integration/runtime-common.sh
mkdir -p /home/goview/logs /home/goview/upload
stop_java goview.war
cd /home/goview
export GOVIEW_AUDIT_SECRET
nohup env \
  SERVER_PORT=8083 SPRING_DATASOURCE_URL=jdbc:sqlite:/home/goview/sqllite/goview.db \
  GOVIEW_UPLOAD_MAPPING=file:/home/goview/upload/ GOVIEW_UPLOAD_PATH=/home/goview/upload \
  V2_XNLJMAP_OSS=file:/home/goview/upload/ V2_FILEURL=/home/goview/upload V2_HTTPURL="$SCREEN_PUBLIC_URL/" \
  RUOYI_SECURITY_ENABLED=true RUOYI_SECURITY_ENDPOINT="http://$WEB_INTERNAL_HOST:8081/internal/security/context" \
  RUOYI_AUDIT_ENABLED=true RUOYI_AUDIT_URL="http://$WEB_INTERNAL_HOST:8081/internal/goview/audit" \
  java -jar /home/goview/goview.war > /home/goview/logs/goview.log 2>&1 < /dev/null &
start_nginx
