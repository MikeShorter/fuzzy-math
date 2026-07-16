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

// Slices 2a/2b (§15.7): membership functions, the pointwise algebra, hedges, the
// Domain seam, domain-generic analysis, and Zadeh §V (ℝ¹-bound, §15.5).
include("fuzzy-set")

// §20: fuzzy numbers. Not here because §10 lists it next — because §15.3 was an
// unproven claim until something overrode something. It is proven, and corrected
// (§20.8), by this module.
include("fuzzy-number")

// §21: fuzzy relations. Finishes Zadeh 1965 — composition (p.346) and eqs.
// (22)/(23) were the paper's last unshipped equations — and collects §15.4's
// promise that Product "earns itself twice".
include("fuzzy-relation")

// §22: defuzzification — §11a's sanctioned seam to the control layer above,
// and the one module in §10's tail that needs no source we lack (§18.2's rule
// amputates the rest until sources arrive).
include("fuzzy-defuzz")
