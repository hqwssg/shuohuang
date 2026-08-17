#!/bin/sh
set -e

mkdir -p /run/nginx /home/goview/upload /home/goview/sqllite /var/log/nginx
java ${JAVA_OPTS:-} -jar /home/goview/goview.war &
exec nginx -g "daemon off;"
