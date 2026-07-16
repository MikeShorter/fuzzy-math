import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.GradleException
import org.gradle.api.Project

/**
 * The POM every `fuzzy-*` artifact publishes — extracted so that
 * `fuzzy.publishing-conventions` (Kotlin/JVM modules) and
 * `fuzzy.platform-publishing-conventions` (the BOM) state it once.
 *
 * CLAUDE.md §14.3 is why every field here matters: Central surfaces
 * `pom.url`/`pom.scm` on every artifact page, and published versions are
 * immutable — a wrong value here is wrong in public, permanently, for that
 * version. The repo URL was corrected there before the first push; keep it
 * correct.
 */
fun MavenPublishBaseExtension.fuzzyPom(project: Project) {
    pom {
        name.set(project.name)
        // Each module sets `description` in its own build.gradle.kts; Central
        // rejects a POM without one, so fail loudly here rather than at upload.
        description.set(
            project.provider {
                project.description
                    ?: throw GradleException(
                        "Module '${project.name}' must set `description` in its " +
                            "build.gradle.kts — Maven Central rejects a POM without one.",
                    )
            },
        )
        inceptionYear.set("2026")
        url.set("https://github.com/MikeShorter/fuzzy-math")

        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("repo")
            }
        }

        developers {
            developer {
                id.set("MikeShorter")
                name.set("Michael E Shorter")
                url.set("https://eusrbin.dk")
            }
        }

        scm {
            url.set("https://github.com/MikeShorter/fuzzy-math")
            connection.set("scm:git:https://github.com/MikeShorter/fuzzy-math.git")
            developerConnection.set("scm:git:ssh://git@github.com/MikeShorter/fuzzy-math.git")
        }
    }
}
