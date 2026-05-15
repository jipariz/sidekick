# Screenshots

These images are generated automatically by `scripts/capture-screenshots.sh` from the desktop demo. Don't edit them by hand — re-capture instead.

## Regenerating

```bash
bash scripts/capture-screenshots.sh
```

The script launches `:demo-app:run` once per shot with the `SIDEKICK_SHOT` env var set. The demo seeds the Network Monitor with sample HTTP calls and the Log Monitor with sample entries when that variable is present, then opens directly on the requested plugin via the `initialPluginId` deep-link.

## Files

| File | What it shows |
|---|---|
| `hero.png` | Pokemon catalog with FAB visible. Banner shot. |
| `menu.png` | Sidekick panel open on the plugin grid. |
| `network-monitor.png` | Network Monitor list + detail with seeded calls. |
| `log-monitor.png` | Log Monitor list with mixed log levels. |
| `preferences.png` | Preferences grid (boolean, string, enum cards). |
| `custom-screens.png` | A Custom Screen plugin (Build Info). |

## Output specs

- Capture size: 1280×800 (macOS Retina captures double, so files may be 2560×1600).
- Background: dark theme forced when `SIDEKICK_SHOT` is active so screenshots are reproducible regardless of the host OS theme.
- The demo's Reveal tutorial overlay is suppressed in screenshot mode.

## Adding new shots

1. Pick a target name and add it to the `SHOTS=(...)` array in `scripts/capture-screenshots.sh`.
2. Map the target to a plugin id in `ScreenshotConfig.initialPluginId` (`demo-app/src/commonMain/.../ScreenshotMode.kt`).
3. (Optional) Add seed data for that plugin in the same file's `seedScreenshotData(...)`.
