import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File
import java.io.FileInputStream
import java.util.Properties

/**
 * Reads the nearest `version.properties` file above this module (within
 * the project tree) and applies its `sdk.version` to `project.version`.
 *
 * Versions are tracked **per plugin family**, not per module — siblings
 * within a family (e.g. `network-monitor/api`, `network-monitor/plugin`,
 * `network-monitor/ktor`) all resolve to `plugins/network-monitor/version.properties`
 * because they share internal API surface and must move together.
 *
 * The companion plugin `SidekickVersionUpdateConventionPlugin` (applied
 * to the root project) maintains the family-level files via the
 * `updateModuleVersions` and `checkModuleVersions` tasks.
 */
class SidekickVersionReadConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val rootDir = rootProject.rootDir.canonicalFile
            val versionFile = findFamilyVersionFile(projectDir.canonicalFile, rootDir)
                ?: throw GradleException(
                    "No `version.properties` found in ${project.path} or any parent directory " +
                        "up to the repository root. Family-level version.properties lives at the " +
                        "plugin-family root (e.g. `plugins/network-monitor/version.properties`)."
                )

            val versionProps = Properties()
            FileInputStream(versionFile).use { versionProps.load(it) }

            val moduleVersion = versionProps.getProperty("sdk.version")
                ?: throw GradleException(
                    "version.properties at ${versionFile.path} must contain 'sdk.version'"
                )

            project.version = moduleVersion

            logger.lifecycle("[Version] ${project.path} -> $moduleVersion (from ${versionFile.parentFile.name}/version.properties)")
        }
    }

    /**
     * Walk up from `start` looking for the nearest `version.properties`,
     * bounded by `rootDir` (we never look at or above the repo root).
     */
    private fun findFamilyVersionFile(start: File, rootDir: File): File? {
        var dir: File? = start
        val rootPath = rootDir.canonicalPath
        while (dir != null && dir.canonicalPath.startsWith(rootPath) && dir.canonicalPath != rootPath) {
            val candidate = File(dir, "version.properties")
            if (candidate.exists()) return candidate
            dir = dir.parentFile
        }
        return null
    }
}
