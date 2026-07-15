plugins {
    `kotlin-dsl`
}

// THE VERSION CATALOG WORKAROUND, stated explicitly (SCAFFOLD_PROMPT deliverable 1).
//
// Verified against docs.gradle.org/current/userguide/version_catalogs.html:
//   "the plugins block in the precompiled script plugin cannot access the
//    version catalog."
// This is Gradle issue #15383 and is still open — typesafe `libs.*` accessors
// are not generated for precompiled script plugins.
//
// Two workarounds are needed, for two different problems:
//
//  1. PLUGIN versions (this file). build-logic/build.gradle.kts is an ordinary
//     build script, so `libs.*` DOES work here. We put each Gradle plugin on
//     build-logic's compile classpath as a plain library (its `*-gradle-plugin`
//     artifact). The convention plugins can then write
//     `plugins { id("org.jetbrains.kotlin.jvm") }` with NO version, because the
//     version is already fixed by this classpath. No version literal anywhere.
//
//  2. LIBRARY versions needed INSIDE a convention plugin (e.g. kotest for the
//     test conventions). There, `libs.*` is unavailable, so we use the
//     documented VersionCatalogsExtension lookup:
//         val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
//         libs.findLibrary("kotest-property").get()
//     See fuzzy.kotlin-conventions.gradle.kts.
//
// Rejected alternative: the `LibrariesForLibs` hack —
//     implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
// It reaches into Gradle's generated-accessor internals, is undocumented, and
// breaks across Gradle releases. Not appropriate for a build we intend others
// to contribute to.

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.vanniktech.publish.plugin)
    implementation(libs.dokka.gradle.plugin)
}
