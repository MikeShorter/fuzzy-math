plugins {
    id("fuzzy.jvm-interop-conventions")
    id("fuzzy.publishing-conventions")
}

description =
    "Publishable law suites for fuzzy algebras — verify your own t-norm against " +
        "the published mathematics. Ships the extension mechanism's correctness " +
        "criteria as a consumable artifact, with tolerances calibrated per algebra."

dependencies {
    // `api`, not `implementation`: TNorm, TConorm, Negation and Algebra all
    // appear in fuzzy-laws' public signatures (TNormLaws.verify(TNorm)), so
    // consumers must see them.
    api(project(":fuzzy-algebra"))
}

// CLAUDE.md §7 — this is a CONSUMABLE artifact, not an internal test folder.
//
// Note what is NOT here: kotest is absent from the main source set. It arrives
// only via the test conventions (testImplementation), so fuzzy-laws' published
// runtime dependency is fuzzy-algebra and nothing else. Rationale in
// LawReport/Laws KDoc and CLAUDE.md §14.4 — briefly: kotest-property's checkAll
// is `suspend`, and §9 forbids coroutines in public API. Sampling is therefore
// hand-rolled and seeded, which also lets a Java/Clojure/JUnit/TestNG consumer
// call TNormLaws.verify(myTNorm) without adopting kotest.
