#!/usr/bin/env bash
# Captures README/docs screenshots from the desktop demo.
#
# Each shot launches the demo with SIDEKICK_SHOT=<target>:
#   hero        - Pokemon catalog only, no Sidekick panel.
#   menu        - Sidekick open at the plugin grid.
#   network     - Open directly on the Network Monitor (with seeded calls).
#   logs        - Open directly on the Log Monitor (with seeded entries).
#   preferences - Open directly on the Preferences screen.
#   custom      - Open directly on the Custom Debug screen.
#
# Prerequisite (macOS):
#   System Settings -> Privacy & Security -> Screen & System Audio Recording
#   -> Enable for your terminal app (Terminal/iTerm/etc), then restart it.
#
# Output: docs/assets/screenshots/<target>.png

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

OUT="docs/assets/screenshots"
mkdir -p "$OUT"

SHOTS=(hero menu network logs preferences custom)

# Get the demo window's CGWindowID. Returns empty if not found.
find_window_id() {
    # Use GetWindowList Quartz API via Python's pyobjc, or fall back to a tiny Swift script.
    # Simplest cross-version path: parse `osascript` output for the window id.
    osascript -l JavaScript <<'EOF' 2>/dev/null || true
const SystemEvents = Application('System Events');
const procs = SystemEvents.processes.whose({name: 'java'});
for (const p of procs()) {
    for (const w of p.windows()) {
        try {
            if (w.name().indexOf('Sidekick Demo') >= 0) {
                return String(w.id());
            }
        } catch (e) {}
    }
}
return '';
EOF
}

wait_for_window() {
    local elapsed=0
    while (( elapsed < 60 )); do
        local wid
        wid="$(find_window_id)"
        if [[ -n "${wid:-}" && "$wid" != "0" ]]; then
            echo "$wid"
            return 0
        fi
        sleep 0.5
        elapsed=$((elapsed + 1))
    done
    return 1
}

bring_to_front() {
    osascript <<'EOF' 2>/dev/null || true
tell application "System Events"
    set javaProcs to (every process whose name is "java")
    repeat with p in javaProcs
        try
            set frontmost of p to true
        end try
    end repeat
end tell
EOF
}

for shot in "${SHOTS[@]}"; do
    echo "==> Capturing $shot"

    SIDEKICK_SHOT="$shot" ./gradlew :demo-app:run --no-daemon > "/tmp/sidekick-shot-$shot.log" 2>&1 &
    gradle_pid=$!

    wid="$(wait_for_window)" || true
    if [[ -z "${wid:-}" ]]; then
        echo "    ERROR: window did not appear (see /tmp/sidekick-shot-$shot.log)"
        kill $gradle_pid 2>/dev/null || true
        wait $gradle_pid 2>/dev/null || true
        continue
    fi

    sleep 4
    bring_to_front
    sleep 1

    if screencapture -x -o -l "$wid" "$OUT/$shot.png" 2>/dev/null && [[ -s "$OUT/$shot.png" ]]; then
        echo "    saved $OUT/$shot.png"
    else
        echo "    FAILED to capture (check Screen Recording permission for your terminal app)"
        rm -f "$OUT/$shot.png"
    fi

    kill $gradle_pid 2>/dev/null || true
    pkill -f "dev.parez.sidekick.demo" 2>/dev/null || true
    wait $gradle_pid 2>/dev/null || true
    sleep 1
done

echo ""
ls -lh "$OUT"/*.png 2>/dev/null && echo "Done." || {
    echo ""
    echo "No PNGs were produced. Most likely cause: the terminal lacks Screen Recording"
    echo "permission. Grant it in:"
    echo "    System Settings -> Privacy & Security -> Screen & System Audio Recording"
    echo "Then restart your terminal and run this script again."
    exit 1
}
