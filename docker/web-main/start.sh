#!/bin/sh
set -e

mkdir -p /run/nginx /home/ruoyi/uploadPath /data /var/log/nginx
redis-server --daemonize yes --dir /data --appendonly yes
java ${JAVA_OPTS:-} -jar /home/ruoyi/ruoyi-admin.jar &
exec nginx -g "daemon off;"
