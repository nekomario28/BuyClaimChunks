#!/usr/bin/env bash
set -euo pipefail

backend=${1:?Usage: run-backend-server-smoke.sh <ftb|openpac|none|both>}
if [[ "$backend" != "ftb" && "$backend" != "openpac" && "$backend" != "none" && "$backend" != "both" ]]; then
  echo "Unsupported test backend: $backend" >&2
  exit 1
fi

rm -rf run
mkdir -p run
printf 'eula=true\n' > run/eula.txt

final_log="server-smoke-${backend}.log"
temp_root=${RUNNER_TEMP:-/tmp}
temp_log="${temp_root}/buyclaimchunks-${backend}-server-smoke.log"
rm -f "$final_log" "$temp_log"

setsid ./gradlew runServer \
  -Ptest_backend="$backend" \
  --no-daemon --console=plain >"$temp_log" 2>&1 &
server_pid=$!

cleanup() {
  kill -TERM -- "-$server_pid" 2>/dev/null || true
  wait "$server_pid" 2>/dev/null || true
}
trap cleanup EXIT

for _ in {1..150}; do
  if grep -Eq 'Done \([0-9.]+s\)!' "$temp_log"; then
    cp "$temp_log" "$final_log"

    case "$backend" in
      ftb)
        grep -Fq 'BuyClaimChunks Continued initialized with ftb backend' "$final_log"
        ;;
      openpac)
        grep -Fq 'BuyClaimChunks Continued initialized with openpac backend' "$final_log"
        ;;
      none)
        grep -Fq 'No supported claim backend is installed.' "$final_log"
        grep -Fq 'BuyClaimChunks Continued initialized with unavailable backend' "$final_log"
        ;;
      both)
        grep -Fq 'Both FTB Chunks and Open Parties and Claims are installed.' "$final_log"
        grep -Fq 'BuyClaimChunks Continued initialized with unavailable backend' "$final_log"
        ;;
    esac

    echo "Universal JAR $backend environment reached Done with the expected backend selection."
    exit 0
  fi

  if ! kill -0 "$server_pid" 2>/dev/null; then
    wait "$server_pid" || true
    cp "$temp_log" "$final_log"
    cat "$temp_log"
    echo "$backend dedicated server exited before reaching Done." >&2
    exit 1
  fi

  sleep 2
done

cp "$temp_log" "$final_log"
cat "$temp_log"
echo "$backend dedicated server did not reach Done before the smoke-test deadline." >&2
exit 1
