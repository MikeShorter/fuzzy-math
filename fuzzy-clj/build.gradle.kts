plugins {
    // `base` only: this module compiles nothing and publishes nothing. It is
    // the §9 conformance harness (CLAUDE.md §23.4) — Clojure consuming the
    // real jars, wired into `check` so every build re-proves §9 before
    // anything can publish. No publishing conventions, deliberately: its
    // audience is this repository, not Maven Central.
    base
}

description =
    "UNPUBLISHED. The §9 conformance harness: fuzzy-laws for the JVM-interop " +
        "contract, written in Clojure against the published artifacts."

// The suite's classpath: Clojure itself plus every published module — the
// same accretion §10's note predicted for fuzzy-laws, arriving test-scope at
// the module that validates §9 (§23.5).
val conformance: Configuration by configurations.creating

dependencies {
    conformance(libs.clojure)
    conformance(project(":fuzzy-algebra"))
    conformance(project(":fuzzy-set"))
    conformance(project(":fuzzy-number"))
    conformance(project(":fuzzy-relation"))
    conformance(project(":fuzzy-defuzz"))
    conformance(project(":fuzzy-laws"))
}

// Hand-rolled JavaExec rather than a Clojure Gradle plugin (§23.6): one task
// beats a third-party plugin for a module with nothing to compile. The knob is
// this registration.
val conformanceTest by tasks.registering(JavaExec::class) {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs the §9 conformance suite — Clojure consuming the real jars, no Kotlin anywhere."

    classpath = files(layout.projectDirectory.dir("src/main/clojure")) + conformance
    mainClass.set("clojure.main")
    // clojure.main -m requires the namespace and calls its -main, which runs
    // clojure.test and exits non-zero on any failure — so a §9 regression
    // fails `check`, not just a log line.
    args("-m", "fuzzy.conformance-test")

    inputs.dir(layout.projectDirectory.dir("src/main/clojure"))
    outputs.upToDateWhen { false }
}

tasks.named("check") {
    dependsOn(conformanceTest)
}
