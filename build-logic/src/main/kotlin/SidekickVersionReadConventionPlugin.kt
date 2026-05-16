import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.FileInputStream
import java.util.Properties

/**
 * Reads the module's `version.properties` file and applies the version to
 * the project. Apply this to every publishable Sidekick module — the BOM
 * later picks up each module's version via `project.version`.
 *
 * The companion plugin `SidekickVersionUpdateConventionPlugin` (applied to
 * the root project) provides the `updateModuleVersions` and
 * `checkModuleVersions` tasks that maintain these files automatically.
 */
class SidekickVersionReadConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val versionFile = project.file("version.properties")
            if (!versionFile.exists()) {
                throw GradleException(
                    "Module ${project.path} is missing a `version.properties` file. " +
                        "Run `./gradlew updateModuleVersions` from the root project to create one."
                )
            }

            val versionProps = Properties()
            FileInputStream(versionFile).use { versionProps.load(it) }

            val moduleVersion = versionProps.getProperty("sdk.version")
                ?: throw GradleException(
                    "version.properties in ${project.path} must contain 'sdk.version'"
                )

            project.version = moduleVersion

            logger.lifecycle("[Version] ${project.path} -> $moduleVersion")
        }
    }
}
