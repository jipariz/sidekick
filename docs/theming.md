# Theming

## Automatic Theme Inheritance

By default, `SidekickShell` automatically picks up the host app's `MaterialTheme`:

- **Host has a custom `MaterialTheme`** → Sidekick uses those colors.
- **Host uses M3 defaults (or no `MaterialTheme`)** → Sidekick falls back to its own dark indigo scheme (`SidekickDefaultColorScheme`).

The host app's content is rendered **outside** Sidekick's theme and is never affected by it.

```kotlin
// Sidekick automatically uses your brand colors
MaterialTheme(colorScheme = myBrandColorScheme) {
    SidekickShell(plugins = plugins) {
        MyAppContent()
    }
}
```

## Overriding HTTP Badge and Status Colors

To customize the semantic colors used for HTTP method badges and status chips without changing the Material color scheme, pass a `SidekickColors` instance:

```kotlin
SidekickShell(
    plugins = plugins,
    sidekickColors = sidekickColors(
        httpGet    = Color(0xFF1976D2),
        httpPost   = Color(0xFF388E3C),
        httpPut    = Color(0xFFF57C00),
        httpDelete = Color(0xFFD32F2F),
        httpPatch  = Color(0xFF7B1FA2),
    ),
) {
    MyAppContent()
}
```

All parameters have sensible defaults derived from the resolved `MaterialTheme`. Only override what you need.

| Parameter | Used for |
|-----------|----------|
| `httpGet` | GET method badge |
| `httpPost` | POST method badge |
| `httpPut` | PUT method badge |
| `httpDelete` | DELETE method badge |
| `httpPatch` | PATCH method badge |
| `httpOther` | Any other method |
| `onHttpBadge` | Text on method badges |
| `statusSuccess` | 2xx status chips |
| `statusRedirect` | 3xx status chips |
| `statusClientError` | 4xx status chips |
| `statusServerError` | 5xx status chips |
| `statusPending` | In-flight request indicator |
| `statusNetworkError` | Network error chip |
| `onStatusChip` | Text on status chips |

## Forcing a Specific Theme

Wrap with `SidekickTheme` to bypass auto-detection entirely and force a specific color scheme:

```kotlin
SidekickTheme(colorScheme = myForcedColorScheme) {
    SidekickShell(plugins = plugins) {
        MyAppContent()
    }
}
```
