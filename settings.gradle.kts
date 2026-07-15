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

// The modules of §10's graph that exist. The rest are deliberately absent —
// not stubbed, not commented-in — until a slice builds them.
//
// Slice 1 (§12) predicted that adding one later would be "a single include(…)
// here plus a build.gradle.kts applying the same convention plugins". Adding
// fuzzy-set in slice 2a cost exactly that, so the shape is holding.
include("fuzzy-algebra")
include("fuzzy-laws")

// Slice 2a (§15.7): membership functions, the pointwise algebra, hedges, the
// Domain seam, and domain-generic analysis. Zadeh §V — convexity, boundedness,
// shadow, separation — is slice 2b and is ℝ¹-bound (§15.5).
include("fuzzy-set")
