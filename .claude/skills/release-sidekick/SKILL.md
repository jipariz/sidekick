---
name: release-sidekick
description: >
  Cut a Sidekick BOM release to Maven Central, or run the per-family version
  bump task. Walks through the full flow: detecting current state, bumping
  the BOM calendar version, tagging, watching the publish workflow, and
  prompting for the manual Central Portal promotion step. Trigger with:
  "release sidekick", "publish to maven central", "cut a BOM", "bump versions",
  or "/release-sidekick".
argument-hint: "[bump-only | release]"
allowed-tools: Read Write Edit Bash Glob Grep AskUserQuestion
---

# Sidekick Release Skill

You are coordinating a Sidekick release. Sidekick uses **per-family semver** for plugin modules and a **calendar-versioned BOM** that pins each family's current version. Background and rationale are documented in `CLAUDE.md` → "Versioning and Publishing".

This skill handles two modes:

1. **Bump-only** — run `./gradlew updateModuleVersions` so any source change is reflected in the right family's PATCH. Common before pushing a code-change PR.
2. **Release** — cut a new BOM coordinate, tag it, watch the publish workflow, and prompt the user to promote the staging deployments in the Sonatype Central Portal.

---

## Phase 1 — Determine intent

If `$ARGUMENTS` is `bump-only` or `release`, use that directly. Otherwise ask:

```
AskUserQuestion:
  question: "What do you want to do?"
  header: "Mode"
  options:
    - label: "Bump module versions"
      description: "Run ./gradlew updateModuleVersions to refresh family hashes and bump PATCH on any family whose source changed. Use this before pushing a code-change PR."
    - label: "Cut a BOM release"
      description: "Bump the BOM calendar version, tag, push, watch the publish workflow, and promote on Central Portal. Use this when you want to ship a new BOM to Maven Central."
```

---

## Phase 2 — Bump-only mode

1. Confirm the user is on a feature branch (not `main`). If they're on `main`, warn and suggest branching first.
2. Run `./gradlew updateModuleVersions --no-configuration-cache` and capture the output.
3. Parse the lines:
   - `[VersionBump] NO CHANGE: …` → that family is unchanged.
   - `[VersionBump] CHANGE DETECTED: <family> X.Y.Z -> X.Y.(Z+1)` → that family was bumped.
   - `[VersionBump] SEED: …` → first-time hash recording. Surface as "seeded — check git diff to confirm intent."
4. If anything was bumped:
   - Run `git status --short` to show which `version.properties` files changed.
   - Tell the user: "Commit these `version.properties` updates alongside your code changes in the same PR. CI's `checkModuleVersions` will fail otherwise."
5. If nothing was bumped, just report: "All families up to date with their source content."

If the user appears to have changed `MAJOR` or `MINOR` manually (look for `version.properties` modifications in `git diff` where the prefix differs from before), remind them: "ABI-impacting changes to one family may warrant manually bumping the MINOR of every depending family in the same PR — the hash automation doesn't follow inter-family dependencies."

---

## Phase 3 — Release mode

The release flow has several gates because the actions affect Maven Central. Walk through them in order, confirming each step with the user before proceeding.

### 3.1 Preflight checks (read-only — do these in parallel)

In one batched message, run:

- `git branch --show-current` — must be `main`. If not, abort and tell the user to merge first.
- `git status --short` — must be clean. If not, abort.
- `git fetch origin main && git log --oneline HEAD..origin/main` — must be empty (local up to date).
- `cat gradle.properties | grep sidekick.bomVersion` — capture the current BOM version.
- `./gradlew checkModuleVersions --no-configuration-cache 2>&1 | tail -5` — must end with `[VersionCheck] OK.`
- `gh auth status 2>&1 | head -3` — must be logged in.
- `gh secret list --env maven-central --repo jipariz/sidekick 2>&1` — confirm 4 secrets exist: `MAVEN_CENTRAL_USERNAME`, `MAVEN_CENTRAL_PASSWORD`, `SIGNING_IN_MEMORY_KEY`, `SIGNING_IN_MEMORY_KEY_PASSWORD`.

If any check fails, stop and tell the user how to fix it. Do not proceed.

### 3.2 Determine the new BOM version

Compute today's date in `YYYY.MM.DD` format. If it equals the current `sidekick.bomVersion`:

```
AskUserQuestion:
  question: "Today's date equals the current BOM version (<value>). What do you want to do?"
  header: "Same-day"
  options:
    - label: "Skip BOM bump — re-tag existing version"
      description: "Useful if you bumped sidekick.bomVersion in a recent PR but haven't tagged yet."
    - label: "Use a suffix (e.g. YYYY.MM.DD.1)"
      description: "Forces a fresh BOM coordinate even though the date is the same."
```

If different, use today's date as the new BOM version.

### 3.3 Bump `sidekick.bomVersion` (only if needed)

If the new BOM version differs from the current one:

1. Create a branch: `git checkout -b chore/bump-bom-<new-version>`.
2. Edit `gradle.properties`:
   ```
   sidekick.bomVersion=<old> → sidekick.bomVersion=<new>
   ```
3. Commit with message `chore(release): bump BOM to <new>`.
4. Push and open a PR with `gh pr create`.
5. Wait for the user to merge it (PR review may be required). Pause here and ask: "Tell me when PR #<num> is merged."
6. After merge: `git checkout main && git pull`.

### 3.4 Tag and push

1. Confirm `gradle.properties:sidekick.bomVersion` matches the planned `<new>`.
2. Compose the tag annotation. Read each family's `version.properties` and build a list:
   ```
   Sidekick BOM <new>

   Pins:
   - core:plugin-api / runtime / noop @ <core sdk.version>
   - network-monitor / -plugin / -ktor @ <network-monitor sdk.version>
   - log-monitor / -plugin / -kermit @ <log-monitor sdk.version>
   - preferences / -ksp / gradle-plugin (dev.parez.sidekick.preferences) @ <preferences sdk.version>
   - custom-screens @ <custom-screens sdk.version>
   ```
3. `git tag -a v<new> -m "<annotation>"`
4. `git push origin v<new>`

### 3.5 Watch the workflow

1. Wait ~3 seconds, then `gh run list --workflow=publish.yml --repo jipariz/sidekick --limit 1 --json databaseId,headBranch,event,status,conclusion,createdAt` to find the new run's id.
2. Confirm `headBranch` matches `v<new>` and `event` is `push`.
3. Launch `gh run watch <id> --repo jipariz/sidekick --exit-status` as a `run_in_background: true` Bash call. Tell the user the build will take 5–15 minutes and you'll be notified when it finishes.
4. On completion:
   - **Exit 0**: both publish steps green. Continue to 3.6.
   - **Non-zero**: read failed step logs with `gh run view <id> --repo jipariz/sidekick --log-failed`. Stop and surface the failure. Common failures: signing key invalid, Portal credentials wrong, namespace not verified.

### 3.6 Prompt for Portal promotion

Tell the user:

> Both bundle uploads succeeded. Open <https://central.sonatype.com> → **Deployments**. You should see two new staging deployments tagged with version `<new>`:
> 1. The main bundle (`dev.parez.sidekick:bom` + every per-family artifact)
> 2. The gradle-plugin bundle (`dev.parez.sidekick:sidekick-preferences-gradle-plugin` + the two plugin markers)
>
> Click **Publish** on each. Propagation to `repo1.maven.org` takes 10–30 minutes per deployment.

Wait for the user to confirm they've promoted both before continuing.

### 3.7 Create the GitHub Release

1. After Central promotion is confirmed, compose release notes. Use this template:

```markdown
**Sidekick BOM <new>** — pins the following per-family versions:

| Family | Version |
|---|---|
| core | <core sdk.version> |
| network-monitor | <network-monitor sdk.version> |
| log-monitor | <log-monitor sdk.version> |
| preferences | <preferences sdk.version> |
| custom-screens | <custom-screens sdk.version> |

## Quick install

```kotlin
dependencies {
    debugImplementation("dev.parez.sidekick:runtime:<core sdk.version>")
    releaseImplementation("dev.parez.sidekick:noop:<core sdk.version>")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(platform("dev.parez.sidekick:bom:<new>"))
            implementation("dev.parez.sidekick:network-monitor-plugin")
            implementation("dev.parez.sidekick:network-monitor-ktor")
            implementation("dev.parez.sidekick:log-monitor-plugin")
            implementation("dev.parez.sidekick:log-monitor-kermit")
            implementation("dev.parez.sidekick:preferences")
            implementation("dev.parez.sidekick:custom-screens")
        }
    }
}
```

For the Preferences KSP processor:

```kotlin
plugins {
    id("dev.parez.sidekick.preferences") version "<preferences sdk.version>"
}
```

See [Installation](https://github.com/jipariz/sidekick/blob/v<new>/docs/installation.md).
```

2. Create the release:
   ```bash
   gh release create v<new> --repo jipariz/sidekick \
     --title "Sidekick BOM <new>" \
     --notes "$(cat <<'EOF'
   <release notes from above>
   EOF
   )"
   ```

3. Surface the release URL to the user.

---

## Phase 4 — Final report

Summarize the work in 2–3 sentences:

- New BOM version published.
- Per-family versions captured in the new BOM.
- Link to the GitHub Release.
- Reminder: the next code-change PR's contributor should run `./gradlew updateModuleVersions` before pushing; CI's `checkModuleVersions` gates this.

---

## Edge cases and rules

- **Never tag directly without merging the `sidekick.bomVersion` bump first.** Tagging a commit that doesn't have the right `bomVersion` in `gradle.properties` would publish an inconsistent BOM. The order must be: PR-merge → tag.
- **Never force-push tags.** If a release run fails partway and needs to be redone, drop the partial staging in Central Portal first, then `git push --delete origin v<x>; git tag -d v<x>` and re-tag at the new (fixed) HEAD.
- **Never auto-promote in Central Portal.** The workflow uses `automaticRelease = false` on purpose; the manual Publish click is the last safety net before artifacts go public-immutable.
- **Do not run this skill if the user is mid-development.** A release should land all merged work to date; uncommitted local changes signal incomplete work.
- **Do not modify `version.properties` files directly in release mode.** Family versions are bumped via `updateModuleVersions` in the contributor PRs that introduce the changes — never as part of a release cut. The release PR only edits `gradle.properties:sidekick.bomVersion`.
- **If `dev.parez` namespace verification or GPG key setup hasn't been done**, abort the release and point the user to `CLAUDE.md` → "Maven Central namespace + signing".
- **Do not bypass any preflight check.** Each one exists because skipping it has caused a real release failure documented in `CLAUDE.md` → "Hard-won lessons".
