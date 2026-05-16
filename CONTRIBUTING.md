# Contributing

## Versioning

Each publishable module owns its semver version in its own `version.properties` file (e.g. `core/runtime/version.properties`). The Sidekick BOM (`dev.parez.sidekick:bom`) is calendar-versioned and pins every module's current version transitively.

**Before pushing a change to any module's `src/*Main/` sources, run:**

```bash
./gradlew updateModuleVersions
```

This walks every publishable module, recomputes its source-content hash, and bumps the PATCH on any module whose hash changed. Commit the updated `version.properties` files alongside your code change.

CI's `checkModuleVersions` task runs on every PR and will fail if you forget — with a message listing the modules that need a bump.

### Bumping MAJOR / MINOR

The hash automation only handles PATCH. For breaking or non-backwards-compatible API changes:

1. Edit the relevant `version.properties` manually — bump MAJOR or MINOR, reset PATCH to 0.
2. Run `./gradlew updateModuleVersions` to refresh the content hash so CI passes.
3. Mention the bump rationale in your PR description.

If `plugin-api` gets an ABI-impacting change, also bump the MINOR of every depending plugin in the same PR — the hash automation doesn't follow inter-module dependencies.

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
