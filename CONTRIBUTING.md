# Contributing

## Versioning

Sidekick uses **per-family semver** coordinated by a calendar-versioned BOM. The five families are:

| Family root | Modules sharing one version |
|---|---|
| `core/` | `plugin-api`, `runtime`, `noop` |
| `plugins/network-monitor/` | `api`, `plugin`, `ktor`, `noop` |
| `plugins/log-monitor/` | `api`, `plugin`, `kermit`, `noop` |
| `plugins/preferences/` | `api`, `ksp`, `gradle-plugin` (included build) |
| `plugins/custom-screens/` | `api` |

Each family root owns a single `version.properties` (e.g. `plugins/network-monitor/version.properties`) that every member module reads from. This is intentional — modules within a family share internal API surface and must move together; per-module versions would let `:network-monitor:plugin` and `:network-monitor:api` drift, silently breaking ABI assumptions.

The Sidekick BOM (`dev.parez.sidekick:bom`) is calendar-versioned (`YYYY.MM.DD`) and pins every family's current version transitively.

**Before pushing a change to any module's `src/main/` or `src/<sourceSet>Main/` sources, run:**

```bash
./gradlew updateModuleVersions
```

This walks every family, recomputes the SHA-256 over the union of every member module's source tree, and bumps the family's PATCH on any change. Commit the updated `version.properties` files alongside your code change.

CI's `checkModuleVersions` task runs on every PR and will fail if you forget — with a message listing the families that need a bump.

### Bumping MAJOR / MINOR

The hash automation only handles PATCH. For breaking or non-backwards-compatible API changes:

1. Edit the relevant family's `version.properties` manually — bump MAJOR or MINOR, reset PATCH to 0.
2. Run `./gradlew updateModuleVersions` to refresh the content hash so CI passes.
3. Mention the bump rationale in your PR description.

If `core` (containing `plugin-api`) gets an ABI-impacting change, also consider bumping the MINOR of every depending family in the same PR — the hash automation doesn't follow inter-family dependencies, only intra-family.

### Cutting a BOM release

The BOM's calendar version lives at `gradle.properties:sidekick.bomVersion`. To cut a release:

1. Edit `sidekick.bomVersion` to today's date (`YYYY.MM.DD`).
2. Commit + open PR + merge.
3. Tag the merge commit `v<bomVersion>` and push. The publish workflow fires automatically.
4. After both staging deployments land at https://central.sonatype.com → Deployments, promote them manually.

## Running tests

```bash
./gradlew allTests
```

## Local development snapshot

```bash
./gradlew publishToMavenLocal --no-configuration-cache
```

Artifacts land in `~/.m2/repository/dev/parez/sidekick/` and can be consumed by any project with `mavenLocal()` in front of `mavenCentral()`.
