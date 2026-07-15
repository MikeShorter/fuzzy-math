// fuzzy-math — root settings.
//
// CLAUDE.md §1: shared configuration lives in convention plugins under
// build-logic/, wired in as an INCLUDED BUILD. Not buildSrc (any change to it
// invalidates the whole build); not allprojects/subprojects in a root build
// file (does not scale to twelve modules and couples everything to everything).

pluginManagement {
    // The included build that supplies the `fuzzy.*` convention plugins.
    // It must be inside pluginManagement so `plugins { id("fuzzy.…") }` in a
    // module's build script resolves against it.
    includeBuild("build-logic")

    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    // Modules may not declare their own repositories; the build owns that.
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
    }
    // NOTE: gradle/libs.versions.toml is picked up implicitly by convention.
    // The `build-logic` included build does NOT inherit it and re-declares it
    // explicitly — see build-logic/settings.gradle.kts.
}

rootProject.name = "fuzzy-math"

// CLAUDE.md §12 — Slice 1 is fuzzy-algebra + fuzzy-laws ONLY.
// The remaining ten modules of the §10 module graph are deliberately absent:
// not stubbed, not commented-in. Adding one later is a single `include(…)`
// here plus a build.gradle.kts applying the same convention plugins.
include("fuzzy-algebra")
include("fuzzy-laws")
