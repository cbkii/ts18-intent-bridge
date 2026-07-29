#!/usr/bin/env bash
# Shared, Android/Termux-safe helpers for the TS18 device toolkit.

ts18_info() { printf '[INFO] %s\n' "$*" >&2; }
ts18_warn() { printf '[WARN] %s\n' "$*" >&2; }
ts18_die() {
  printf '[ERROR] %s\n' "$*" >&2
  exit 1
}

ts18_safe_name() {
  printf '%s' "$1" | tr -c 'A-Za-z0-9._-' '_'
}

ts18_require_android() {
  command -v getprop >/dev/null 2>&1 || ts18_die 'Android getprop was not found; run this on the TS18 through Termux or adb shell.'
  command -v pm >/dev/null 2>&1 || ts18_die 'Android package manager command was not found.'
}

ts18_root_available() {
  command -v su >/dev/null 2>&1 && su -c 'id -u' 2>/dev/null | tr -d '\r' | grep -Fxq 0
}

ts18_root() {
  ts18_root_available || ts18_die 'Root is required for this operation.'
  local quoted='' arg
  for arg in "$@"; do
    printf -v quoted '%s %q' "$quoted" "$arg"
  done
  su -c "${quoted# }"
}

ts18_capture() {
  local results_file=$1 output_file=$2 label=$3
  shift 3
  local started status
  started=$(date +%s)
  set +e
  "$@" >"$output_file" 2>&1
  status=$?
  set -e
  printf '%s\t%s\t%s\t%s\n' "$label" "$status" "$started" "$(date +%s)" >>"$results_file"
  return "$status"
}

ts18_capture_root() {
  local results_file=$1 output_file=$2 label=$3
  shift 3
  ts18_capture "$results_file" "$output_file" "$label" ts18_root "$@"
}

ts18_sha256() {
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | awk '{print $1}'
  elif command -v toybox >/dev/null 2>&1; then
    toybox sha256sum "$1" | awk '{print $1}'
  else
    ts18_die 'sha256sum is required.'
  fi
}

ts18_make_zip() {
  local source_dir=$1 output_zip=$2
  command -v zip >/dev/null 2>&1 || ts18_die 'zip is required. In Termux run: pkg install zip unzip'
  command -v unzip >/dev/null 2>&1 || ts18_die 'unzip is required. In Termux run: pkg install zip unzip'
  (
    cd "$source_dir"
    find . -type f -print0 | LC_ALL=C sort -z | xargs -0 zip -q -X "$output_zip"
  )
  unzip -t "$output_zip" >/dev/null
}

ts18_wait_pid() {
  local pid=$1 attempts=${2:-30}
  while ((attempts > 0)); do
    kill -0 "$pid" 2>/dev/null || return 0
    sleep 1
    attempts=$((attempts - 1))
  done
  return 1
}

ts18_stop_writer() {
  local pid=$1
  kill -TERM "$pid" 2>/dev/null || return 0
  if ! ts18_wait_pid "$pid" 10; then
    kill -KILL "$pid" 2>/dev/null || true
    ts18_wait_pid "$pid" 5 || return 1
  fi
}
