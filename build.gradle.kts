// Deliberately thin.
//
// CLAUDE.md §1: "Not a root build file (allprojects/subprojects blocks do not
// scale to twelve modules and couple everything to everything)."
//
// There is intentionally NO plugins block, NO allprojects, NO subprojects here.
// Shared configuration lives in build-logic/src/main/kotlin/fuzzy.*.gradle.kts
// and is opted into, per module, by name. `group` and `version` come from
// gradle.properties, which Gradle applies to every project without needing a
// block here.
//
// If you find yourself wanting to add configuration to this file: that is the
// signal to add or extend a convention plugin instead.
