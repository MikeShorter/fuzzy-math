plugins {
    id("fuzzy.jvm-interop-conventions")
    id("fuzzy.publishing-conventions")
}

description =
    "Membership functions, the pointwise algebra of Zadeh 1965, hedges, and the " +
        "Domain capability seam — fuzzy set theory as a substrate, faithful to the " +
        "1965 paper and nothing else."

dependencies {
    // `api`, not `implementation`: Algebra, TNorm, TConorm and Negation all appear
    // in fuzzy-set's public signatures — the pointwise operations are parameterised
    // by an Algebra (§6, §15), so consumers must see those types.
    //
    // This is the whole dependency (§10). fuzzy-set does not reimplement the
    // connective layer: union IS a TConorm, intersection IS a TNorm, complement IS
    // a Negation. If a `min(a, b)` appears in this module, something has gone
    // wrong — Zadeh §III is the default instantiation of one parameterised
    // mechanism, not a mechanism of its own (§6).
    api(project(":fuzzy-algebra"))
}
