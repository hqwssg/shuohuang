#!/bin/bash
set -euo pipefail
source "$(dirname "$0")/runtime-common.sh"

nginx_calls=()
nginx_running=true
nginx_config_ok=true
nginx() {
  nginx_calls+=("$*")
  if [ "$*" = '-t' ] && [ "$nginx_config_ok" = false ]; then return 1; fi
}
pgrep() { [ "$nginx_running" = true ]; }

start_nginx
test "${nginx_calls[*]}" = '-t -s reload'
nginx_calls=()
nginx_running=false
start_nginx
test "${#nginx_calls[@]}" = 2
test "${nginx_calls[0]}" = '-t'
test "${nginx_calls[1]}" = ''

# A bad configuration must never reload or start another listener.
nginx_calls=()
nginx_config_ok=false
if (set -e; start_nginx); then echo 'FAIL: bad Nginx configuration accepted' >&2; exit 1; fi

value="password with 'quote' and \$variables; spaces"
quoted=$(printf "'%s'" "$(printf '%s' "$value" | sed "s/'/'\"'\"'/g")")
restored=$(sh -c "credential=$quoted; printf '%s' \"\$credential\"")
test "$value" = "$restored"
echo 'PASS: Nginx reload/start/config-failure and POSIX credential quoting.'
