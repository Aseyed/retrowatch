#!/usr/bin/env bash
set -euo pipefail

# Simple startup smoke test:
# - installs debug
# - launches main activity
# - fails if logcat shows a fatal exception for the app

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PKG="com.hardcopy.retrowatch"
ACT="com.hardcopy.retrowatch/.RetroWatchActivity"

SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [[ -z "${SDK_ROOT}" ]]; then
  echo "ERROR: ANDROID_SDK_ROOT (or ANDROID_HOME) is not set." >&2
  exit 2
fi

ADB="${SDK_ROOT}/platform-tools/adb"
if [[ ! -f "${ADB}" ]]; then
  echo "ERROR: adb not found at '${ADB}'" >&2
  exit 2
fi

cd "${ROOT_DIR}"

echo "== building + installing debug =="
./gradlew :app:installDebug

echo "== launching =="
"${ADB}" logcat -c
"${ADB}" shell am start -n "${ACT}" >/dev/null
sleep 4

PID="$("${ADB}" shell pidof "${PKG}" || true)"
if [[ -z "${PID}" ]]; then
  echo "ERROR: app process is not running after launch" >&2
  "${ADB}" logcat -d -t 200 | sed -n '1,200p'
  exit 1
fi

if "${ADB}" logcat -d | grep -E "FATAL EXCEPTION|AndroidRuntime" | grep -q "${PKG}"; then
  echo "ERROR: detected fatal exception for ${PKG}" >&2
  "${ADB}" logcat -d | grep -E "FATAL EXCEPTION|AndroidRuntime|StartupException|NoClassDefFoundError" -n || true
  exit 1
fi

echo "OK: ${PKG} launched (pid ${PID}) and no fatal exceptions detected."


