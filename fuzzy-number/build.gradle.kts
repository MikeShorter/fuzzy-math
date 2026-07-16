plugins {
    id("fuzzy.jvm-interop-conventions")
    id("fuzzy.publishing-conventions")
}

description =
    "Fuzzy numbers — LR/triangular/trapezoidal/Gaussian, exact α-cut interval " +
        "arithmetic, and the closed-form overrides that make CLAUDE.md §15.3 true " +
        "rather than asserted."

dependencies {
    // `api`: MembershipFn, DoubleMembershipFn, Domain and Verdict all appear in
    // this module's public signatures — a FuzzyNumber IS a DoubleMembershipFn.
    api(project(":fuzzy-set"))
}

// §10's graph gains fuzzy-number → fuzzy-set. Still acyclic.
//
// §20.6: almost everything here is `Attributed:`, not `Source:`. Zadeh 1965 has
// no fuzzy arithmetic, and Bergmann 2008 contains zero occurrences of "fuzzy
// number", "LR representation", "α-cut", "Dubois", "Prade", "interval
// arithmetic" or "extension principle" — both checked, not assumed. What IS
// verified: Γ_α is eq. (24), convexity is eq. (25), and f(x) = sup{α | x ∈ Γ_α}
// derives from (24). The rest names its primary and says it is unverified.
