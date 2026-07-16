plugins {
    id("fuzzy.jvm-interop-conventions")
    id("fuzzy.publishing-conventions")
}

description =
    "Fuzzy relations — sup-T composition, fuzzy sets induced by mappings " +
        "(Zadeh eqs. 22/23), the relational image, and the reflexivity/symmetry/" +
        "transitivity queries. Finishes Zadeh 1965."

dependencies {
    // `api`: MembershipFn, Domain, Verdict, Algebra and TNorm all appear in this
    // module's public signatures — a fuzzy relation IS a MembershipFn over a
    // product space (Zadeh p.345; CLAUDE.md §21.2), so there is no relation type
    // to hide the seam behind.
    api(project(":fuzzy-set"))
}

// §10's graph gains fuzzy-relation → fuzzy-set. Still acyclic.
//
// §21.2: there is deliberately NO FuzzyRelation type in this module. Zadeh is
// unambiguous that a relation is a fuzzy set in a product space and nothing
// more, and nothing here has a closed form to override — the subtype would be
// decoration (§21.3's amendment to §16.5). The module is operations:
// FuzzyRelations, @JvmStatic.
