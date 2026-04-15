# Claude Code Skills

Sidekick ships two Claude Code skills that automate common tasks when working with the SDK. Copy the skill folder into your project's `.claude/skills/` directory (create it if it doesn't exist).

---

## `/setup-sidekick` — Setup wizard & preferences migration { #setup-sidekick }

**Skill folder:** `.claude/skills/setup-sidekick/`

An interactive wizard that handles the full Sidekick onboarding flow for a consumer app:

- Adds `core:runtime` / `core:noop` dependencies with the correct debug/release split.
- Prompts you to choose which plugins to enable (Network Monitor, Log Monitor, Preferences, Custom Screens) and adds only the modules you need.
- Wires `SidekickShell` around your root composable with `remember`-wrapped plugin instances.
- **Migrates an existing hand-written DataStore preferences class** to the `@SidekickPreferences` annotation processor: removes boilerplate keys and property getters, replaces them with `@Preference`-annotated vars, and adds the KSP wiring to `build.gradle.kts`.

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
