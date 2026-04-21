# Theming

## Default Behavior

By default `Sidekick` applies its own Material 3 color scheme — a dark indigo palette in dark mode and a complementary light palette in light mode, automatically following the system dark-mode setting.

```kotlin
// Uses Sidekick's own theme (default)
Sidekick(plugins = plugins, onClose = { ... })
```

## Inheriting the Host App's Theme

Pass `useSidekickTheme = false` to make Sidekick inherit the host app's ambient `MaterialTheme` instead:

```kotlin
MaterialTheme(colorScheme = myBrandColorScheme) {
    // ...
    Sidekick(
        plugins = plugins,
        onClose = { sidekickVisible = false },
        useSidekickTheme = false, // uses myBrandColorScheme
    )
}
```

This is useful when your brand colors already look good in the debug panel and you want a consistent feel.

## Summary

| `useSidekickTheme` | Result |
|--------------------|--------|
| `true` *(default)* | Sidekick's own light/dark Material 3 color scheme |
| `false` | Inherits the host app's ambient `MaterialTheme` |

## HTTP Badge and Status Colors

The network-monitor plugin derives its HTTP method badge colors and status chip colors directly from `MaterialTheme.colorScheme`:

| UI element | Color token |
|------------|-------------|
| GET badge | `primary` |
| POST badge | `secondary` |
| PUT badge | `tertiary` |
| DELETE badge | `error` |
| PATCH badge | `tertiaryContainer` |
| Other method | `outline` |
| 2xx status | `secondary` |
| 3xx status | `primary` |
| 4xx status | `tertiary` |
| 5xx status | `error` |
| Pending | `outlineVariant` |

These adapt automatically to whichever theme is active, whether that's Sidekick's built-in scheme or your app's own.
