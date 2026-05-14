# Publishing Sidekick to Maven Central

This document walks the maintainer through the **one-time setup** needed
before the first Central release, and the **per-release steps** after that.
The build is already wired with [`com.vanniktech.maven.publish`][vmp], an
Apache 2.0 `LICENSE` file is in the repo, and every module emits a Maven
Central-compliant POM (verified locally via `./gradlew publishToMavenLocal`).
Only the **credentials side** is unconfigured — that's covered below.

[vmp]: https://github.com/vanniktech/gradle-maven-publish-plugin

---

## 1. One-time setup

### 1.1 Create a Central Portal account

Maven Central no longer accepts new groupIds through OSSRH. New publishers use
the **Central Portal** at <https://central.sonatype.com>.

1. Sign in with GitHub at <https://central.sonatype.com> (or with email/password).
2. Open **View Account** → **User Tokens** and generate a token. Save:
   - `Username` → use as `ORG_GRADLE_PROJECT_mavenCentralUsername`
   - `Password` → use as `ORG_GRADLE_PROJECT_mavenCentralPassword`

### 1.2 Verify the `dev.parez` namespace

Central requires you to prove control of the groupId before publishing.

1. In the Central Portal, open **Namespaces** → **Add Namespace** →
   `dev.parez`.
2. Choose **DNS verification**. Central will give you a TXT record value
   (e.g. `OSSRH-XXXXX`).
3. Add the TXT record to the `parez.dev` DNS zone:
   ```
   _sonatype TXT  OSSRH-XXXXX
   ```
4. Click **Verify** in the Central Portal. Verification usually completes
   within an hour.

> If you don't control `parez.dev`, you'll have to either move the project
> to a namespace you do own (e.g. `io.github.jipariz` — verified automatically
> against your GitHub account), or transfer the existing `dev.parez` groupId
> to a verified owner. The build-script `groupId` and every published POM
> currently hardcode `dev.parez.sidekick` and `dev.parez.sidekick.preferences`
> — change all of these together if you switch namespaces.

### 1.3 Generate a GPG signing key

Central requires every published artifact (jar, sources, javadoc, pom, module)
to be signed with a GPG key whose public half is on a public keyserver.

```bash
# 1. Generate the key (RSA 4096, no expiry).
gpg --full-generate-key
# Real name: Jiri Parizek
# Email:     jiri.parizek@strv.com
# Passphrase: <pick a strong one — you'll need it for every release>

# 2. List keys and grab the key id (last 16 hex chars of the long fingerprint).
gpg --list-secret-keys --keyid-format=long
#   sec   rsa4096/ABCD1234EF567890 …

# 3. Distribute the public half to keyservers.
gpg --keyserver keys.openpgp.org    --send-keys ABCD1234EF567890
gpg --keyserver keyserver.ubuntu.com --send-keys ABCD1234EF567890

# 4. Export an ASCII-armored copy of the private key for CI / local properties.
gpg --armor --export-secret-keys ABCD1234EF567890 > ~/sidekick-signing-key.asc
```

Keep `sidekick-signing-key.asc` out of version control. The CI workflow loads
it from `secrets.SIGNING_IN_MEMORY_KEY`.

### 1.4 Local-machine credentials

Put the following in `~/.gradle/gradle.properties` (NOT the project's
`gradle.properties`). Gradle picks up the `mavenCentralUsername` /
`mavenCentralPassword` / `signingInMemoryKey` / `signingInMemoryKeyPassword`
properties automatically — the build script reads them via `providers.gradleProperty(...)`.

```properties
# ~/.gradle/gradle.properties
mavenCentralUsername=...           # from §1.1 token
mavenCentralPassword=...           # from §1.1 token

# Paste the ENTIRE contents of sidekick-signing-key.asc as one line.
# Real newlines are not allowed in gradle.properties; replace each
# newline with \n. The vanniktech plugin handles base64-decoding /
# newline expansion internally — pass the ASCII-armored text as-is.
signingInMemoryKey=-----BEGIN PGP PRIVATE KEY BLOCK-----\n\nlQOY...\n-----END PGP PRIVATE KEY BLOCK-----\n
signingInMemoryKeyPassword=...     # your GPG passphrase from §1.3
```

### 1.5 GitHub Actions secrets (for CI release)

Set these as repo-level **Actions secrets** at
`https://github.com/jipariz/sidekick/settings/secrets/actions`:

| Secret | Value |
|---|---|
| `MAVEN_CENTRAL_USERNAME` | from §1.1 |
| `MAVEN_CENTRAL_PASSWORD` | from §1.1 |
| `SIGNING_IN_MEMORY_KEY` | full contents of `sidekick-signing-key.asc` (real newlines OK in GitHub Secrets) |
| `SIGNING_IN_MEMORY_KEY_PASSWORD` | GPG passphrase from §1.3 |

The workflow at `.github/workflows/publish.yml` maps these to the
`ORG_GRADLE_PROJECT_*` env vars the build script expects.

---

## 2. Release flow

The build understands two version modes by convention:

- `0.x.y-SNAPSHOT` → snapshot, uploads to Central Snapshots
  (`https://central.sonatype.com/repository/maven-snapshots/`).
- `0.x.y` (no `-SNAPSHOT`) → release, uploads to the Central staging area
  and waits for explicit promotion (`automaticRelease = false`).

The version is read from `gradle.properties` (`sidekick.version=…`) and
propagated to every module's `coordinates(...)` call.

### 2.1 Cut a snapshot (any time)

Snapshots overwrite previous snapshot artifacts, so you can publish a
snapshot whenever you want feedback from a downstream project before tagging
a release.

```bash
./gradlew publishToMavenCentral --no-configuration-cache
# Or, for an included build (Gradle plugin), also run:
( cd plugins/preferences/gradle-plugin && ../../../gradlew publishToMavenCentral )
```

Wait ~5 minutes for the artifacts to sync. Then consumers can use:

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://central.sonatype.com/repository/maven-snapshots/")
    }
}
```

### 2.2 Cut a release

1. Bump `sidekick.version` in `gradle.properties` to a non-SNAPSHOT value
   (e.g. `0.1.0`).
2. Commit with `chore: release 0.1.0` and tag: `git tag v0.1.0 && git push --tags`.
3. The CI workflow (§3) runs `publishToMavenCentral` against the tagged commit.
4. Log in to <https://central.sonatype.com> → **Deployments** → wait for the
   upload to validate (a few minutes) → **Publish** to promote it from
   staging to Central. Once promoted, the artifact is immutable.
5. Bump `sidekick.version` back to the next snapshot (e.g. `0.1.1-SNAPSHOT`)
   in a follow-up commit.

### 2.3 Verify a release

```bash
# Should resolve from Central within ~30 minutes of promotion.
./gradlew dependencies | grep dev.parez.sidekick
# Or open https://central.sonatype.com/artifact/dev.parez.sidekick/runtime
```

---

## 3. CI workflow

The workflow at `.github/workflows/publish.yml` triggers on tag push (`v*`)
and uses the secrets configured in §1.5. It runs the publish task for both
the main build and the included `preferences/gradle-plugin` build.

For PR builds and `main` branch pushes, only `assembleDebug` + tests run —
no signing, no upload — because the signing-key env var is absent and the
build script's `if (hasSigningKey) { signAllPublications() }` becomes a no-op.

---

## 4. Troubleshooting

### "Plugin marker not found"
You hit the bug fixed in commit `<TBD — first commit after P0 #1>`. Update to the latest snapshot.

### "Failed to publish: 401 Unauthorized"
Token used for `mavenCentralUsername` / `mavenCentralPassword` is wrong or
expired. Regenerate at <https://central.sonatype.com> → **View Account** →
**User Tokens**.

### "Failed to sign: gpg: signing failed: No such file or directory"
You configured `signing.gnupg.keyName` instead of `signingInMemoryKey`. The
build expects in-memory keys (so CI works without a GPG agent). Re-read §1.4.

### "Validation failed: missing field name|description|url|licenses|developers|scm"
Some POM field isn't being emitted. Confirm by inspecting
`~/.m2/repository/dev/parez/sidekick/<module>/<version>/<module>-<version>.pom`
after `./gradlew publishToMavenLocal`. The shared metadata lives in
`build-logic/src/main/kotlin/SidekickPomConfig.kt`; the gradle-plugin and BOM
modules duplicate it inline.

### "Wrong groupId on plugin marker"
Sanity-check `~/.m2/repository/dev/parez/sidekick/preferences/dev.parez.sidekick.preferences.gradle.plugin/<version>/` exists. If it landed under `dev/parez/sidekick/dev.parez.sidekick.preferences.gradle.plugin/`, the
`afterEvaluate { groupId = "dev.parez.sidekick" }` regression is back —
revert to setting `group =` at script-evaluation time (see git history of
`plugins/preferences/gradle-plugin/build.gradle.kts`).
