import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm

// Maven Central publishing + licensing.
//
// CLAUDE.md §1: public, Maven Central, Apache-2.0 — "a paid library cannot go
// on Maven Central, and Maven Central *is* the distribution channel for JVM
// libraries."
//
// CONFIGURED BUT NOT PUBLISHED (SCAFFOLD_PROMPT deliverable 4). Nothing here
// uploads anything; `publishToMavenCentral()` only *registers* the target.
//
// Why a third-party plugin (CLAUDE.md §14.3): OSSRH is retired, so the stock
// `maven-publish` plugin has no route to the Central Portal on its own — it
// would leave a manual bundle-zip-and-POST step. vanniktech is the de-facto
// standard for the Portal and also handles signing + sources/javadoc jars +
// POM validation. It is a BUILD-time dependency only: it contributes nothing
// to the published artifact's runtime dependency graph.

plugins {
    id("com.vanniktech.maven.publish")
    id("org.jetbrains.dokka")
}

mavenPublishing {
    // Registers the Central Portal endpoint. Does not upload.
    publishToMavenCentral()

    // Central requires signed artifacts. Signing is only *performed* by publish
    // tasks, so `./gradlew build` works with no keys configured. Supply
    // credentials out-of-band when the time comes (never in this repo):
    //   ~/.gradle/gradle.properties:
    //     mavenCentralUsername / mavenCentralPassword
    //     signingInMemoryKey / signingInMemoryKeyPassword
    signAllPublications()

    // Central also requires a javadoc jar. For Kotlin that means Dokka HTML
    // packaged as -javadoc.jar; Dokka 2.x renamed the task, hence the explicit
    // name rather than JavadocJar.Dokka()'s default.
    configure(
        KotlinJvm(
            javadocJar = JavadocJar.Dokka("dokkaGeneratePublicationHtml"),
            sourcesJar = true,
        ),
    )

    // Coordinates default to project.group / project.name / project.version.
    // group and version come from the root gradle.properties, which Gradle
    // applies to every project — no allprojects block required.

    pom {
        name.set(project.name)
        // Each module sets `description` in its own build.gradle.kts; Central
        // rejects a POM without one, so fail loudly here rather than at upload.
        description.set(
            provider {
                project.description
                    ?: throw GradleException(
                        "Module '${project.name}' must set `description` in its " +
                            "build.gradle.kts — Maven Central rejects a POM without one.",
                    )
            },
        )
        inceptionYear.set("2026")
        url.set("https://github.com/eusrbin/fuzzy-math")

        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("repo")
            }
        }

        developers {
            developer {
                id.set("eusrbin")
                name.set("Mike")
                url.set("https://eusrbin.dk")
            }
        }

        scm {
            url.set("https://github.com/eusrbin/fuzzy-math")
            connection.set("scm:git:https://github.com/eusrbin/fuzzy-math.git")
            developerConnection.set("scm:git:ssh://git@github.com/eusrbin/fuzzy-math.git")
        }
    }
}
