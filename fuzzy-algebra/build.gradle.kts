plugins {
    id("fuzzy.jvm-interop-conventions")
    id("fuzzy.publishing-conventions")
}

description =
    "Degrees, negations, t-norms, t-conorms, residua, implication families and " +
        "Algebra bundles — the connective layer of fuzzy mathematics, usable with " +
        "no set theory at all."

// CLAUDE.md §10: fuzzy-algebra has NO DEPENDENCIES. This is deliberate, not an
// accident of it being first: "many-valued logic users want this and no set
// theory". Adding a dependency here needs a decision in CLAUDE.md, not a commit.
//
// (The Kotlin stdlib arrives via the Kotlin Gradle plugin; that is the floor,
// not a choice — see gradle.properties.)
