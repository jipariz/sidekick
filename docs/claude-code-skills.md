# Claude Code Skills

Sidekick ships two Claude Code skills that automate common tasks when working with the SDK. Copy the skill folder into your project's `.claude/skills/` directory (create it if it doesn't exist).

---

## `/setup-sidekick` — Setup wizard & preferences migration { #setup-sidekick }

**Skill folder:** `.claude/skills/setup-sidekick/`

An interactive wizard that handles the full Sidekick onboarding flow for a consumer app:

- Adds `core:runtime` / `core:noop` dependencies with the correct debug/release split.
- Prompts you to choose which plugins to enable (Network Monitor, Log Monitor, Preferences, Custom Screens) and adds only the modules you need.
- Wires `SidekickShell` around your root composable with `remember`-wrapped plugin instances.
- **Migrates an existing hand-written DataStore preferences class** to the `@SidekickPreferences` annotation processor: removes boilerplate keys and property getters, replaces them with `@Preference`-annotated vars, adds the KSP wiring to `build.gradle.kts`, and **preserves your existing DataStore file name** by setting `storeName` automatically so no stored values are lost.

### How to install

```bash
cp -r /path/to/sidekick/.claude/skills/setup-sidekick  your-app/.claude/skills/
```

### How to use

In Claude Code, type:

```
/setup-sidekick
```

Or describe what you want:

> "Add Sidekick to my app with the network monitor and preferences plugins."
> "Migrate my AppPreferences DataStore class to Sidekick."

The skill will read your `build.gradle.kts`, ask a few questions, and apply all changes in one pass.

### Migrating from an existing DataStore

When you run `/setup-sidekick` (or describe your existing `AppSettingsStore` to Claude Code), the skill reads your hand-written DataStore class and:

1. Inspects how you construct your `DataStore<Preferences>` to detect the file name you passed to `preferencesDataStore()` or `PreferenceDataStoreFactory`.
2. Emits the annotated replacement class with `storeName` set to that exact value:

    ```kotlin
    // Detected: preferencesDataStore(name = "app_settings")
    // Generated:
    @SidekickPreferences(title = "App Settings", storeName = "app_settings")
    class AppPreferences {
        @Preference(label = "Dark Mode", defaultValue = "false")
        var darkMode: Boolean = false
        // ...
    }
    ```

3. Removes the old `AppSettingsStore` class and updates all its call sites to use the generated accessor.

Because `storeName` is set explicitly, the generated code opens the same `.preferences_pb` file your existing store was writing to — **no stored values are lost on update**.

If the skill cannot detect the DataStore file name (e.g. it is constructed dynamically), it will ask you to confirm or provide it before generating.

### What it does not migrate automatically

| Pattern | Why | What to do |
|---------|-----|------------|
| Composite types (two `Long` keys → one domain object) | KSP can't model multi-key types | Keep manually and reconstruct in your ViewModel |
| `getOrNullFlow` / `getBlockingOrNull` | KSP always emits a non-null `StateFlow` | Keep manually or use a null-safe default + `.value` |
| Enum stored via `.value` property (not `.name`) | KSP uses `Enum.valueOf(name)` — a custom string key breaks round-trip | Annotate the enum with `.name` storage or keep manually |

---

## `/create-plugin` — Scaffold a new plugin module { #create-plugin }

**Skill folder:** `.claude/skills/create-plugin/`

Creates a new `plugins/<name>/api` module from scratch: `build.gradle.kts`, the base `SidekickPlugin` class, and the `include` line in `settings.gradle.kts`.

### How to use

```
/create-plugin my-feature-flags
```

!!! note
    This skill is intended for use **inside the Sidekick repository** itself (or in a project that includes Sidekick as a composite build).
