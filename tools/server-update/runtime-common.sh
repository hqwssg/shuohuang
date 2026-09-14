#!/bin/sh

stop_java() {
  artifact=$1
  for pid in $(pgrep -x java || true); do
    command_line=$(tr '\000' ' ' < "/proc/$pid/cmdline" 2>/dev/null || true)
    case "$command_line" in
      *"$artifact"*)
        kill "$pid" 2>/dev/null || true
        elapsed=0
        while kill -0 "$pid" 2>/dev/null && [ "$elapsed" -lt 30 ]; do
          sleep 1
          elapsed=$((elapsed + 1))
        done
        kill -0 "$pid" 2>/dev/null && kill -9 "$pid" 2>/dev/null || true
        ;;
    esac
  done
}

start_nginx() {
  nginx -t || return 1
  if pgrep -x nginx >/dev/null 2>&1; then
    nginx -s reload
  else
    nginx
  fi
}
