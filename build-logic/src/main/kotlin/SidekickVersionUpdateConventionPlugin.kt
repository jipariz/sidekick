import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Properties

/**
 * Maintains per-family versioning for the Sidekick monorepo.
 *
 * A *family* is a directory containing one or more publishable modules
 * that share internal API surface and therefore must move together. The
 * five families are:
 *
 *   core                       — plugin-api, runtime, noop
 *   plugins/network-monitor    — api, plugin, ktor
 *   plugins/log-monitor        — api, plugin, kermit
 *   plugins/preferences        — api, ksp, gradle-plugin (included build)
 *   plugins/custom-screen      — api
 *
 * Each family root owns a single `version.properties`:
 *   sdk.version       — semver (MAJOR.MINOR.PATCH)
 *   sdk.content.hash  — SHA-256 over the union of every member module's
 *                       source tree (sorted, OS-independent)
 *
 * MAJOR and MINOR are bumped manually by editing the file.
 * PATCH is bumped automatically by `updateModuleVersions` when the hash
 * differs from `sdk.content.hash` — any source change in any member of
 * the family bumps the family's version.
 *
 * Two tasks registered on the root project:
 *
 *   ./gradlew updateModuleVersions
 *     Local dev task. For each family: recomputes the union hash, and if
 *     it differs from the stored hash, bumps PATCH and rewrites the
 *     family's version.properties.
 *
 *   ./gradlew checkModuleVersions
 *     CI gate. Fails the build if any family's hash drifted but PATCH
 *     wasn't bumped, or if a managed family is missing its version.properties.
 *
 * Why per-family, not per-module: Sidekick's plugin modules have tight
 * intra-family coupling (e.g. `network-monitor:plugin` imports types
 * from `network-monitor:api`). A per-module scheme would let
 * `:api`'s version drift from `:plugin`'s after an internal-only change,
 * silently producing combinations the BOM coordinates but the per-module
 * numbers don't accurately describe. Per-family keeps the version numbers
 * honest about cross-module API compatibility.
 */
class SidekickVersionUpdateConventionPlugin : Plugin<Project> {

    /** A publishable family + the member directories whose source it covers. */
    private data class Family(
        /** Display path used in log + failure messages (e.g. ":plugins:network-monitor"). */
        val displayPath: String,
        /** The family root directory (contains version.properties). */
        val dir: File,
        /** Every member module's project directory under the family. */
        val memberDirs: List<File>,
    )

    /** Hard-coded family roots; each has a version.properties at its root. */
    private val familyRoots: List<String> = listOf(
        "core",
        "plugins/network-monitor",
        "plugins/log-monitor",
        "plugins/preferences",
        "plugins/custom-screen",
    )

    override fun apply(target: Project) {
        if (target != target.rootProject) {
            throw GradleException(
                "SidekickVersionUpdateConventionPlugin must be applied to the root project."
            )
        }

        with(target) {
            tasks.register("updateModuleVersions") {
                group = "versioning"
                description = "Bumps PATCH of every Sidekick family whose source has changed."
                outputs.upToDateWhen { false }

                doLast {
                    val families = managedFamilies(this@with)
                    var bumped = 0
                    var seeded = 0
                    families.forEach { family ->
                        val info = computeFamilyHashInfo(family)
                        if (info == null) {
                            logger.warn(
                                "[VersionBump] SKIPPING: ${family.displayPath} " +
                                    "(no version.properties — create one with `sdk.version=0.1.0` and re-run)."
                            )
                            return@forEach
                        }

                        // First-run seeding: version.properties was hand-created
                        // with sdk.version but no sdk.content.hash. Write the hash
                        // and KEEP the version — this is how the migration commit
                        // pins every family at its starting version without an
                        // immediate bump.
                        if (info.lastHash == null) {
                            logger.lifecycle(
                                "[VersionBump] SEED: ${family.displayPath} -> hash recorded " +
                                    "(version held at ${info.currentVersion})"
                            )
                            info.props.setProperty("sdk.content.hash", info.currentHash)
                            FileOutputStream(info.versionFile).use { out ->
                                info.props.store(
                                    out,
                                    "Version maintained by updateModuleVersions task. " +
                                        "DO NOT EDIT HASH MANUALLY."
                                )
                            }
                            seeded++
                            return@forEach
                        }

                        if (info.isMatch) {
                            logger.lifecycle(
                                "[VersionBump] NO CHANGE: ${family.displayPath} (${info.currentVersion})"
                            )
                            return@forEach
                        }

                        val parts = info.currentVersion.split(".")
                        if (parts.size != 3 || parts.any { it.toIntOrNull() == null }) {
                            logger.error(
                                "[VersionBump] ERROR: Invalid version '${info.currentVersion}' " +
                                    "in ${family.displayPath} — expected MAJOR.MINOR.PATCH."
                            )
                            return@forEach
                        }
                        val major = parts[0].toInt()
                        val minor = parts[1].toInt()
                        val patch = parts[2].toInt()
                        val newVersion = "$major.$minor.${patch + 1}"

                        logger.lifecycle(
                            "[VersionBump] CHANGE DETECTED: ${family.displayPath} " +
                                "${info.currentVersion} -> $newVersion"
                        )

                        info.props.setProperty("sdk.version", newVersion)
                        info.props.setProperty("sdk.content.hash", info.currentHash)
                        FileOutputStream(info.versionFile).use { out ->
                            info.props.store(
                                out,
                                "Version auto-bumped by updateModuleVersions task. " +
                                    "DO NOT EDIT HASH MANUALLY."
                            )
                        }
                        bumped++
                    }
                    logger.lifecycle(
                        "[VersionBump] Done. $bumped bumped, $seeded seeded of ${families.size} family/families."
                    )
                }
            }

            tasks.register("checkModuleVersions") {
                group = "verification"
                description = "Fails if any Sidekick family has un-bumped version changes."

                val families = managedFamilies(this@with)
                inputs.files(families.map { File(it.dir, "version.properties") })
                families.forEach { family ->
                    family.memberDirs.forEach { member ->
                        sourceSetMainRoots(member).forEach { root ->
                            inputs.dir(root).withPathSensitivity(
                                org.gradle.api.tasks.PathSensitivity.RELATIVE
                            )
                        }
                    }
                }

                doLast {
                    val failures = mutableListOf<String>()
                    families.forEach { family ->
                        val info = computeFamilyHashInfo(family)
                        when {
                            info == null ->
                                failures += "  - ${family.displayPath}: missing version.properties at family root."
                            info.lastHash == null ->
                                failures += "  - ${family.displayPath}: version.properties has no sdk.content.hash yet " +
                                    "(needs seeding)."
                            !info.isMatch ->
                                failures += "  - ${family.displayPath}: source changed but version not bumped " +
                                    "(version.properties hash: ${info.lastHash}, actual: ${info.currentHash})."
                        }
                    }
                    if (failures.isNotEmpty()) {
                        throw GradleException(
                            buildString {
                                appendLine("Family version check failed:")
                                failures.forEach { appendLine(it) }
                                appendLine()
                                appendLine("Run `./gradlew updateModuleVersions` and commit the result.")
                            }
                        )
                    }
                    logger.lifecycle(
                        "[VersionCheck] OK. ${families.size} family version(s) consistent with source."
                    )
                }
            }
        }
    }

    /**
     * Returns the families recognized in this repo, each with its member
     * module directories. Members are subdirs of the family root that
     * have a `src/` directory — this naturally includes both regular
     * subprojects and the standalone gradle-plugin included build (which
     * Gradle's `rootProject.subprojects` doesn't surface).
     */
    private fun managedFamilies(rootProject: Project): List<Family> {
        return familyRoots.mapNotNull { relPath ->
            val familyDir = File(rootProject.rootDir, relPath)
            if (!familyDir.isDirectory) return@mapNotNull null
            val members = familyDir.listFiles().orEmpty()
                .filter { it.isDirectory && File(it, "src").isDirectory }
                .sortedBy { it.name }
            Family(
                displayPath = ":${relPath.replace('/', ':')}",
                dir = familyDir,
                memberDirs = members,
            )
        }
    }

    /**
     * Source-set roots that participate in the published artifact:
     * every `src/<sourceSet>Main/` directory (KMP convention) plus
     * `src/main/` (plain JVM/Java/Gradle plugin convention). Test source
     * sets are intentionally excluded — test-only changes shouldn't bump
     * a published version.
     */
    private fun sourceSetMainRoots(moduleDir: File): List<File> {
        val src = File(moduleDir, "src")
        if (!src.exists() || !src.isDirectory) return emptyList()
        return src.listFiles().orEmpty()
            .filter { it.isDirectory && (it.name == "main" || it.name.endsWith("Main")) }
            .sortedBy { it.name }
    }

    private data class FamilyHashInfo(
        val props: Properties,
        val versionFile: File,
        val currentVersion: String,
        val currentHash: String,
        val lastHash: String?,
        val isMatch: Boolean,
    )

    /**
     * Hash info for a family. Returns null only if the family's
     * version.properties is missing entirely — callers decide whether
     * that's fatal (checkModuleVersions treats it as a failure;
     * updateModuleVersions logs a warning).
     */
    private fun computeFamilyHashInfo(family: Family): FamilyHashInfo? {
        val versionFile = File(family.dir, "version.properties")
        if (!versionFile.exists()) return null

        val props = Properties().apply {
            FileInputStream(versionFile).use(::load)
        }
        val currentVersion = props.getProperty("sdk.version")
            ?: throw GradleException(
                "version.properties at ${versionFile.path} must contain 'sdk.version'"
            )
        val lastHash = props.getProperty("sdk.content.hash")
        val currentHash = computeFamilyHash(family)

        return FamilyHashInfo(
            props = props,
            versionFile = versionFile,
            currentVersion = currentVersion,
            currentHash = currentHash,
            lastHash = lastHash,
            isMatch = currentHash == lastHash,
        )
    }

    /**
     * SHA-256 over the byte content of every regular file in every
     * member module's `src/main/` and `src/<sourceSet>Main/` source
     * sets, sorted by family-relative path for OS-independent output.
     * Source code only — build scripts and `build/` outputs are
     * intentionally excluded.
     */
    private fun computeFamilyHash(family: Family): String {
        val md = MessageDigest.getInstance("SHA-256")
        val familyPath = family.dir.toPath()
        val files = family.memberDirs
            .asSequence()
            .flatMap { member -> sourceSetMainRoots(member).asSequence() }
            .flatMap { root ->
                root.walkTopDown()
                    .filter { it.isFile }
                    .filter { !it.toPath().any { p -> p.toString() == "build" } }
            }
            .map { file -> familyPath.relativize(file.toPath()).toString().replace(File.separatorChar, '/') to file }
            .sortedBy { it.first }
            .toList()

        if (files.isEmpty()) {
            // Family with no source yet still hashes deterministically.
            return "0".repeat(64)
        }
        files.forEach { (relPath, file) ->
            md.update(relPath.toByteArray(StandardCharsets.UTF_8))
            md.update(0)
            md.update(file.readBytes())
            md.update(0)
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
