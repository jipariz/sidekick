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
 *     updateModuleVersions before pushing.
 *
 * Adapted from MjDevCz/android-sdk-crypto-ecosystem's
 * SdkVersionUpdateConventionPlugin, with two Sidekick-specific changes:
 *   1. The source-hash walk covers KMP source sets (every `src/<sourceSet>Main/`
 *      directory), not just `src/main`.
 *   2. Module filter targets Sidekick's `:core:*` and `:plugins:*` paths,
 *      excluding `:bom` and `:demo-app`.
 */
class SidekickVersionUpdateConventionPlugin : Plugin<Project> {

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
                        val info = computeModuleHashInfo(module) ?: return@forEach

                        // First-run seeding: version.properties was hand-created
                        // with sdk.version but no sdk.content.hash. Write the hash
                        // and KEEP the version — this is how the migration commit
                        // pins every module at 0.1.0 without an immediate bump.
                        if (info.lastHash == null) {
                            logger.lifecycle(
                                "[VersionBump] SEED: ${module.path} -> hash recorded " +
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
                            logger.lifecycle("[VersionBump] NO CHANGE: ${module.path} (${info.currentVersion})")
                            return@forEach
                        }

                        val parts = info.currentVersion.split(".")
                        if (parts.size != 3 || parts.any { it.toIntOrNull() == null }) {
                            logger.error(
                                "[VersionBump] ERROR: Invalid version '${info.currentVersion}' " +
                                    "in ${module.path} — expected MAJOR.MINOR.PATCH."
                            )
                            return@forEach
                        }
                        val major = parts[0].toInt()
                        val minor = parts[1].toInt()
                        val patch = parts[2].toInt()
                        val newVersion = "$major.$minor.${patch + 1}"

                        logger.lifecycle(
                            "[VersionBump] CHANGE DETECTED: ${module.path} " +
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
                inputs.files(modules.map { it.file("version.properties") })
                modules.forEach { module ->
                    sourceSetMainRoots(module).forEach { root ->
                        inputs.dir(root).withPathSensitivity(
                            org.gradle.api.tasks.PathSensitivity.RELATIVE
                        )
                    }
                }

                doLast {
                    val failures = mutableListOf<String>()
                    modules.forEach { module ->
                        val info = computeModuleHashInfo(module) ?: return@forEach
                        when {
                            info.lastHash == null ->
                                failures += "  - ${module.path}: version.properties has no sdk.content.hash yet " +
                                    "(needs seeding)."
                            !info.isMatch ->
                                failures += "  - ${module.path}: source changed but version not bumped " +
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
     * Publishable Sidekick modules: everything under `:core:` and `:plugins:`
     * that has a `src/` directory (excludes Gradle-synthesized parent projects
     * like `:plugins:network-monitor` which only exist as containers for
     * `:plugins:network-monitor:api` etc.). Also excludes the BOM and demo-app.
     * The included gradle-plugin build is a separate Gradle root; it owns its
     * own version.properties read logic.
     */
    private fun managedModules(rootProject: Project): List<Project> =
        rootProject.subprojects.filter { project ->
            val path = project.path
            val pathOk = (path.startsWith(":core:") || path.startsWith(":plugins:")) &&
                path != ":bom" &&
                path != ":demo-app"
            pathOk && project.file("src").isDirectory
        }

    /**
     * KMP source-set roots that participate in the published artifact:
     * every `src/<sourceSet>Main/` directory under the module, excluding `*Test/`.
     * Includes `commonMain`, `androidMain`, `iosMain`, `jvmMain`, `jsMain`,
     * `wasmJsMain`, and any other KMP `Main` source sets.
     */
    private fun sourceSetMainRoots(module: Project): List<File> {
        val src = module.file("src")
        if (!src.exists() || !src.isDirectory) return emptyList()
        return src.listFiles().orEmpty()
            .filter { it.isDirectory && it.name.endsWith("Main") }
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

    private fun computeModuleHashInfo(module: Project): ModuleHashInfo? {
        val versionFile = module.file("version.properties")
        if (!versionFile.exists()) {
            module.logger.warn(
                "[VersionCheck] SKIPPING: ${module.path} (no version.properties — " +
                    "run `./gradlew updateModuleVersions` to seed it)."
            )
            return null
        }

        val props = Properties().apply {
            FileInputStream(versionFile).use(::load)
        }
        val currentVersion = props.getProperty("sdk.version")
            ?: throw GradleException(
                "version.properties in ${module.path} must contain 'sdk.version'"
            )
        val lastHash = props.getProperty("sdk.content.hash")
        val currentHash = computeContentHash(module)

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
     * `src/<sourceSet>Main/` source sets, sorted by relative path for OS-independent
     * output. Source code only — build scripts and generated `build/`
     * outputs are intentionally excluded.
     */
    private fun computeContentHash(module: Project): String {
        val md = MessageDigest.getInstance("SHA-256")
        val roots = sourceSetMainRoots(module)
        if (roots.isEmpty()) {
            // Modules with no Main source yet still hash deterministically.
            return "0".repeat(64)
        }
        val moduleDir = module.projectDir.toPath()
        val files = roots
            .asSequence()
            .flatMap { root ->
                root.walkTopDown()
                    .filter { it.isFile }
                    .filter { !it.toPath().any { p -> p.toString() == "build" } }
            }
            .map { file -> moduleDir.relativize(file.toPath()).toString().replace(File.separatorChar, '/') to file }
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
