#!/bin/sh
set -eu
mode=$1
phase=$2
payload=$3

install_package() {
  if command -v dnf >/dev/null 2>&1; then dnf install -y "$@"
  elif command -v yum >/dev/null 2>&1; then yum install -y "$@"
  else echo 'ERROR: install the required runtime packages manually.' >&2; exit 1
  fi
}

if [ "$phase" = prepare ]; then
  command -v java >/dev/null
  command -v nginx >/dev/null
  command -v pgrep >/dev/null
  command -v curl >/dev/null || install_package curl
  if [ "$mode" = web ]; then
    java -version 2>&1 | grep -Eq 'version "(17|18|19|[2-9][0-9])\.' || {
      echo 'ERROR: main/model backend requires Java 17 or newer.' >&2; exit 1;
    }
    command -v redis-server >/dev/null
    REPORT_PYTHON=${REPORT_PYTHON:-python3}
    "$REPORT_PYTHON" -c 'import sys; assert sys.version_info >= (3,10), "Python >= 3.10 required"'
    if [ ! -x /opt/carbon-report-venv/bin/python ]; then
      "$REPORT_PYTHON" -m venv /opt/carbon-report-venv
    fi
    /opt/carbon-report-venv/bin/python -c 'import sys; assert sys.version_info >= (3,10)'
    /opt/carbon-report-venv/bin/python -m pip install -r "$payload/web-main/requirements-cli.txt"
    /opt/carbon-report-venv/bin/python -c 'import pydantic, docx, PIL, matplotlib, jinja2, httpx'
    /opt/carbon-report-venv/bin/python -c 'import pydantic; assert int(pydantic.__version__.split(".")[0]) >= 2'
    command -v libreoffice >/dev/null || command -v soffice >/dev/null || install_package libreoffice
    command -v libreoffice >/dev/null || command -v soffice >/dev/null
    if command -v fc-list >/dev/null && ! fc-list :lang=zh | grep -q .; then
      install_package google-noto-sans-cjk-fonts || echo 'WARNING: install a Chinese font to avoid blank PDF text.'
    fi
  else
    test -f /home/goview/sqllite/goview.db || {
      echo 'ERROR: existing GoView database is missing; this is an update-only package.' >&2; exit 1;
    }
  fi
  exit 0
fi

test "$phase" = apply
. "$payload/scripts/runtime-common.sh"
mkdir -p /usr/local/lib/carbon-integration
cp "$payload/scripts/runtime-common.sh" /usr/local/lib/carbon-integration/runtime-common.sh
if [ "$mode" = web ]; then
  stop_java ruoyi-admin.jar
  stop_java carbon-emission-model.jar
  mkdir -p /home/ruoyi/projects /home/ruoyi/renderer /home/ruoyi/logs
  # Replace only application-owned directories; leave uploaded/production data alone.
  for entry in ruoyi-ui carbon-model; do
    target=/home/ruoyi/projects/$entry
    if [ -e "$target" ]; then mv "$target" "$target.before-$UPDATE_ID"; fi
  done
  if [ -e /home/ruoyi/renderer/carbon_report_agent ]; then
    mv /home/ruoyi/renderer/carbon_report_agent "/home/ruoyi/renderer/carbon_report_agent.before-$UPDATE_ID"
  fi
  cp -r "$payload/web-main/html/dist" /home/ruoyi/projects/ruoyi-ui
  cp -r "$payload/web-main/html/carbon-model" /home/ruoyi/projects/carbon-model
  cp -r "$payload/web-main/renderer/carbon_report_agent" /home/ruoyi/renderer/
  cp "$payload/web-main/jar/ruoyi-admin.jar" /home/ruoyi/ruoyi-admin.jar
  cp "$payload/web-main/jar/carbon-emission-model.jar" /home/ruoyi/carbon-emission-model.jar
  cp "$payload/web-main/python/steam_calculator.py" /home/ruoyi/steam_calculator.py
  cp "$payload/web-main/conf/nginx.conf" /etc/nginx/nginx.conf
  cp "$payload/scripts/start-web.sh" /usr/local/bin/start-ruoyi-web.sh
  chmod 755 /home /home/ruoyi /home/ruoyi/projects
  chmod -R a+rX /home/ruoyi/projects/ruoyi-ui /home/ruoyi/projects/carbon-model
  chmod 700 /usr/local/bin/start-ruoyi-web.sh
  sh /usr/local/bin/start-ruoyi-web.sh
else
  stop_java goview.war
  mkdir -p /home/goview/projects /home/goview/logs
  if [ -e /home/goview/projects/goview ]; then
    mv /home/goview/projects/goview "/home/goview/projects/goview.before-$UPDATE_ID"
  fi
  cp -r "$payload/web-screen/html/dist" /home/goview/projects/goview
  cp "$payload/web-screen/jar/goview.war" /home/goview/goview.war
  cp "$payload/web-screen/conf/nginx.conf" /etc/nginx/nginx.conf
  cp "$payload/scripts/start-screen.sh" /usr/local/bin/start-ruoyi-screen.sh
  chmod 755 /home /home/goview /home/goview/projects
  chmod -R a+rX /home/goview/projects/goview
  chmod 700 /usr/local/bin/start-ruoyi-screen.sh
  sh /usr/local/bin/start-ruoyi-screen.sh
fi
