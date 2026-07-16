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

    // CLAUDE.md §16.6 — a new edge, and a change to §10's graph. §15.7 requires
    // that fuzzy-laws gain suites for slice 2a's own laws in the same slice, and
    // MembershipFn/Domain appear in their signatures. Still acyclic: fuzzy-set
    // depends on fuzzy-algebra and nothing else.
    //
    // The alternative — set laws in fuzzy-set's own test source set — was
    // rejected: it would make them internal tests rather than a published
    // artifact, which is the one distinction §7 exists to draw.
    api(project(":fuzzy-set"))

    // CLAUDE.md §20 — the same edge for the same reason, one slice on. §15.7
    // requires a slice's laws to ship with it, and FuzzyNumber appears in
    // FuzzyNumberLaws' signatures. Still acyclic:
    // fuzzy-laws -> fuzzy-number -> fuzzy-set -> fuzzy-algebra.
    //
    // Note the shape this is settling into: fuzzy-laws accretes an edge to every
    // module it publishes laws for, so it ends up depending on most of the graph.
    // That is the cost of §7's decision that the laws are a CONSUMABLE artifact
    // rather than internal tests, and it was accepted when §16.6 added fuzzy-set.
    // It is only paid in test scope. Recorded here rather than rediscovered at
    // module nine.
    api(project(":fuzzy-number"))

    // CLAUDE.md §21.8 — the edge §10's note anticipated, arriving on schedule.
    // MembershipFn<Pair<X,Y>> and Mapping appear in RelationLaws' signatures.
    // Still acyclic: fuzzy-relation -> fuzzy-set -> fuzzy-algebra.
    api(project(":fuzzy-relation"))
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
