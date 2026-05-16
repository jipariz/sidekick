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
 * Maintains per-module versioning for the Sidekick monorepo.
 *
 * Each publishable module owns a `version.properties` file with two keys:
 *   sdk.version       — semver (MAJOR.MINOR.PATCH)
 *   sdk.content.hash  — SHA-256 of the module's source tree (sorted)
 *
 * MAJOR and MINOR are bumped manually by editing the file.
 * PATCH is bumped automatically by `updateModuleVersions` when the hash
 * differs from `sdk.content.hash`.
 *
 * Two tasks registered on the root project:
 *
 *   ./gradlew updateModuleVersions
 *     Local dev task. Walks every publishable module, recomputes its
 *     source hash, and if it differs from the stored hash, bumps PATCH
 *     and rewrites version.properties.
 *
 *   ./gradlew checkModuleVersions
 *     CI gate. Fails the build if any module's hash drifted but PATCH
 *     wasn't bumped — i.e. someone changed code without running
 *     updateModuleVersions before pushing. Also fails if a managed module
 *     is missing its version.properties entirely.
 *
 * Adapted from MjDevCz/android-sdk-crypto-ecosystem's
 * SdkVersionUpdateConventionPlugin, with Sidekick-specific changes:
 *   1. The source-hash walk covers KMP source sets (every src/<sourceSet>Main/
 *      directory) as well as plain JVM src/main/, so JVM-only modules like
 *      the KSP processor and the included gradle-plugin are handled too.
 *   2. Module enumeration includes the standalone gradle-plugin build at
 *      plugins/preferences/gradle-plugin/ — it's a separate Gradle root
 *      (included build), invisible to rootProject.subprojects, but still
 *      a publishable module whose hash must be maintained.
 *   3. checkModuleVersions fails (rather than silently skips) when a
 *      managed module is missing version.properties.
 */
class SidekickVersionUpdateConventionPlugin : Plugin<Project> {

    /** A publishable Sidekick module, identified by its filesystem dir. */
    private data class ManagedModule(
        /** Display path used in log + failure messages (e.g. ":core:runtime"). */
        val displayPath: String,
        /** The module's project directory. */
        val dir: File,
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
                description = "Bumps PATCH of every Sidekick module whose source has changed."
                outputs.upToDateWhen { false }

                doLast {
                    val modules = managedModules(this@with)
                    var bumped = 0
                    var seeded = 0
                    modules.forEach { module ->
                        val info = computeModuleHashInfo(module)
                        if (info == null) {
                            logger.warn(
                                "[VersionBump] SKIPPING: ${module.displayPath} " +
                                    "(no version.properties — create one with `sdk.version=0.1.0` and re-run)."
                            )
                            return@forEach
                        }

                        // First-run seeding: version.properties was hand-created
                        // with sdk.version but no sdk.content.hash. Write the hash
                        // and KEEP the version — this is how the migration commit
                        // pins every module at 0.1.0 without an immediate bump.
                        if (info.lastHash == null) {
                            logger.lifecycle(
                                "[VersionBump] SEED: ${module.displayPath} -> hash recorded " +
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
                            logger.lifecycle("[VersionBump] NO CHANGE: ${module.displayPath} (${info.currentVersion})")
                            return@forEach
                        }

                        val parts = info.currentVersion.split(".")
                        if (parts.size != 3 || parts.any { it.toIntOrNull() == null }) {
                            logger.error(
                                "[VersionBump] ERROR: Invalid version '${info.currentVersion}' " +
                                    "in ${module.displayPath} — expected MAJOR.MINOR.PATCH."
                            )
                            return@forEach
                        }
                        val major = parts[0].toInt()
                        val minor = parts[1].toInt()
                        val patch = parts[2].toInt()
                        val newVersion = "$major.$minor.${patch + 1}"

                        logger.lifecycle(
                            "[VersionBump] CHANGE DETECTED: ${module.displayPath} " +
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
                        "[VersionBump] Done. $bumped bumped, $seeded seeded of ${modules.size} module(s)."
                    )
                }
            }

            tasks.register("checkModuleVersions") {
                group = "verification"
                description = "Fails if any Sidekick module has un-bumped version changes."

                val modules = managedModules(this@with)
                inputs.files(modules.map { File(it.dir, "version.properties") })
                modules.forEach { module ->
                    sourceSetMainRoots(module.dir).forEach { root ->
                        inputs.dir(root).withPathSensitivity(
                            org.gradle.api.tasks.PathSensitivity.RELATIVE
                        )
                    }
                }

                doLast {
                    val failures = mutableListOf<String>()
                    modules.forEach { module ->
                        val info = computeModuleHashInfo(module)
                        when {
                            info == null ->
                                failures += "  - ${module.displayPath}: missing version.properties " +
                                    "(publishable module must have one with sdk.version)."
                            info.lastHash == null ->
                                failures += "  - ${module.displayPath}: version.properties has no sdk.content.hash yet " +
                                    "(needs seeding)."
                            !info.isMatch ->
                                failures += "  - ${module.displayPath}: source changed but version not bumped " +
                                    "(version.properties hash: ${info.lastHash}, actual: ${info.currentHash})."
                        }
                    }
                    if (failures.isNotEmpty()) {
                        throw GradleException(
                            buildString {
                                appendLine("Module version check failed:")
                                failures.forEach { appendLine(it) }
                                appendLine()
                                appendLine("Run `./gradlew updateModuleVersions` and commit the result.")
                            }
                        )
                    }
                    logger.lifecycle(
                        "[VersionCheck] OK. ${modules.size} module version(s) consistent with source."
                    )
                }
            }
        }
    }

    /**
     * Publishable Sidekick modules. Combines two sources:
     *
     *   1. Standard subprojects under :core: / :plugins: that have a src/
     *      directory, excluding :bom, :demo-app, and Gradle-synthesized
     *      parent projects (e.g. :plugins:network-monitor — the container
     *      for :plugins:network-monitor:api etc.).
     *
     *   2. The standalone gradle-plugin at plugins/preferences/gradle-plugin/,
     *      which is an included Gradle build (not a subproject) but is still
     *      a publishable module whose version.properties must be maintained
     *      from the same root-level tasks.
     */
    private fun managedModules(rootProject: Project): List<ManagedModule> {
        val subprojects = rootProject.subprojects.mapNotNull { project ->
            val path = project.path
            val pathOk = (path.startsWith(":core:") || path.startsWith(":plugins:")) &&
                path != ":bom" && path != ":demo-app"
            if (!pathOk || !project.file("src").isDirectory) null
            else ManagedModule(displayPath = path, dir = project.projectDir)
        }
        val includedGradlePlugin = File(rootProject.rootDir, "plugins/preferences/gradle-plugin")
        val extras = if (File(includedGradlePlugin, "src").isDirectory) {
            listOf(
                ManagedModule(
                    displayPath = ":plugins:preferences:gradle-plugin (included build)",
                    dir = includedGradlePlugin,
                )
            )
        } else emptyList()
        return (subprojects + extras).sortedBy { it.displayPath }
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

    private data class ModuleHashInfo(
        val props: Properties,
        val versionFile: File,
        val currentVersion: String,
        val currentHash: String,
        val lastHash: String?,
        val isMatch: Boolean,
    )

    /**
     * Returns hash info for a module, or null if its version.properties is
     * missing. Callers decide whether null is fatal (checkModuleVersions
     * treats it as a failure; updateModuleVersions logs a warning).
     */
    private fun computeModuleHashInfo(module: ManagedModule): ModuleHashInfo? {
        val versionFile = File(module.dir, "version.properties")
        if (!versionFile.exists()) return null

        val props = Properties().apply {
            FileInputStream(versionFile).use(::load)
        }
        val currentVersion = props.getProperty("sdk.version")
            ?: throw GradleException(
                "version.properties in ${module.displayPath} must contain 'sdk.version'"
            )
        val lastHash = props.getProperty("sdk.content.hash")
        val currentHash = computeContentHash(module.dir)

        return ModuleHashInfo(
            props = props,
            versionFile = versionFile,
            currentVersion = currentVersion,
            currentHash = currentHash,
            lastHash = lastHash,
            isMatch = currentHash == lastHash,
        )
    }

    /**
     * SHA-256 over the byte content of every regular file in the module's
     * `src/main/` and `src/<sourceSet>Main/` source sets, sorted by
     * relative path for OS-independent output. Source code only — build
     * scripts and `build/` outputs are intentionally excluded.
     */
    private fun computeContentHash(moduleDir: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        val roots = sourceSetMainRoots(moduleDir)
        if (roots.isEmpty()) {
            // Modules with no Main source yet still hash deterministically.
            return "0".repeat(64)
        }
        val modulePath = moduleDir.toPath()
        val files = roots
            .asSequence()
            .flatMap { root ->
                root.walkTopDown()
                    .filter { it.isFile }
                    .filter { !it.toPath().any { p -> p.toString() == "build" } }
            }
            .map { file -> modulePath.relativize(file.toPath()).toString().replace(File.separatorChar, '/') to file }
            .sortedBy { it.first }
            .toList()

        files.forEach { (relPath, file) ->
            md.update(relPath.toByteArray(StandardCharsets.UTF_8))
            md.update(0)
            md.update(file.readBytes())
            md.update(0)
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
