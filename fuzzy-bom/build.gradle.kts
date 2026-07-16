plugins {
    id("fuzzy.platform-publishing-conventions")
}

description =
    "Bill of materials for fuzzy-math — one version constraint per published " +
        "module, so a consumer aligns the family with a single import."

// CLAUDE.md §1: "fuzzy-bom serves CONSUMERS; the catalog serves THIS BUILD —
// they are different things and both exist." This is the consumer half.
//
// §23.5: the six PUBLISHED modules and nothing else. fuzzy-clj is deliberately
// absent — it is an unpublished conformance harness (§23.4). Note also who
// this serves: Maven and Gradle consumers. tools.deps cannot import a BOM at
// all (verified — only jar artifacts are supported), so deps.edn consumers pin
// per-artifact versions instead; §10's fuzzy-clj → BOM arrow died of that.
dependencies {
    constraints {
        api(project(":fuzzy-algebra"))
        api(project(":fuzzy-set"))
        api(project(":fuzzy-number"))
        api(project(":fuzzy-relation"))
        api(project(":fuzzy-defuzz"))
        api(project(":fuzzy-laws"))
    }
}
