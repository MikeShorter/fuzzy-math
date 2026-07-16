plugins {
    id("fuzzy.jvm-interop-conventions")
    id("fuzzy.publishing-conventions")
}

description =
    "Scalar summaries of a membership function — centroid, bisector, and the " +
        "maxima family. CLAUDE.md §11a's sanctioned seam to a control layer " +
        "above: the last thing the substrate ships, the first thing a control " +
        "layer reaches for."

dependencies {
    // `api`: DoubleMembershipFn and Domain<Double> appear in every public
    // signature here. This is the whole dependency (§10) — and note what is
    // deliberately absent: fuzzy-number. The defuzzifiers compute at the fold's
    // fidelity precisely because they CANNOT see fuzzy-number's closed forms
    // (§22.2's rule), and adding the edge would not change that — overrides
    // live on the type, and this module is downstream of it.
    api(project(":fuzzy-set"))
}

// §10's graph gains fuzzy-defuzz → fuzzy-set. Still acyclic.
//
// §22.6: there is NO DefuzzLaws suite — nothing here is consumer-extensible
// (§22.2 closed the override path), so a published suite would be an extension
// point's criteria with no extension point, the inverse of §14.5's defect. The
// module's central facts are pinned in fuzzy-laws' own tests instead, via the
// first test-scope-only edge in the graph.
