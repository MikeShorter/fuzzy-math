import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm

// Maven Central publishing + licensing.
//
// CLAUDE.md §1: public, Maven Central, Apache-2.0 — "a paid library cannot go
// on Maven Central, and Maven Central *is* the distribution channel for JVM
// libraries."
//
// CONFIGURED BUT NOT PUBLISHED. Nothing here uploads anything;
// `publishToMavenCentral()` only *registers* the target.
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

    // The POM itself lives in FuzzyPom.kt, shared with
    // fuzzy.platform-publishing-conventions (§23.6) — one statement of the
    // §14.3-sensitive fields, two conventions that publish them.
    fuzzyPom(project)
}
