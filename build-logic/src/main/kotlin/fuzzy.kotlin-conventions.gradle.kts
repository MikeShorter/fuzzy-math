import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Baseline Kotlin/JVM conventions for every fuzzy-* module.
//
// CLAUDE.md §1: JVM-only, not KMP — consumers are JAR consumers.
// Applied via build-logic, never via allprojects/subprojects.

plugins {
    id("org.jetbrains.kotlin.jvm")

    // NOT redundant with kotlin("jvm"), which applies plain `java`. The `api`
    // configuration comes from java-library and nowhere else — without this,
    // fuzzy-laws' `api(project(":fuzzy-algebra"))` fails outright with
    // "Could not find method api()", and the kotlin-stdlib pin below has no
    // configuration to attach to.
    //
    // Correct for this project independently of that: these are libraries, and
    // java-library is what makes the api/implementation split real — it is the
    // difference between a consumer seeing TNorm in fuzzy-laws' signatures
    // (they must) and seeing our internals (they must not).
    `java-library`
}

// --- Version catalog access inside a precompiled script plugin --------------
// Typesafe `libs.*` accessors are NOT generated here (Gradle #15383, and the
// userguide states it outright: "the plugins block in the precompiled script
// plugin cannot access the version catalog"). This is the documented lookup.
// Named `catalog*` rather than `version`/`library` to avoid colliding with
// Project.version and the dependencies DSL.
private val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

fun catalogVersion(alias: String): String =
    catalog.findVersion(alias)
        .orElseThrow { GradleException("No [versions] entry '$alias' in gradle/libs.versions.toml") }
        .requiredVersion

fun catalogLibrary(alias: String): Provider<MinimalExternalModuleDependency> =
    catalog.findLibrary(alias)
        .orElseThrow { GradleException("No [libraries] entry '$alias' in gradle/libs.versions.toml") }

kotlin {
    // The JDK that compiles. Distinct from the bytecode level below.
    jvmToolchain(catalogVersion("jvm-toolchain").toInt())

    // Non-negotiable for a published library: every public declaration must
    // state its visibility and return type explicitly. Catches accidental API.
    explicitApi()

    compilerOptions {
        // The bytecode floor imposed on consumers — deliberately NOT the
        // toolchain above. CLAUDE.md §14.1 (ratified 2026-07-15): compile on 24,
        // emit 17, so both live LTS releases can consume this.
        jvmTarget.set(JvmTarget.fromTarget(catalogVersion("jvm-target")))

        // Interface default methods are REAL JVM default methods, plus the
        // DefaultImpls compatibility bridges — pinned rather than inherited
        // from a compiler default nobody chose. CLAUDE.md §23.2: §9's central
        // promise ("reify one method, inherit the lot") is a fact about this
        // flag, the fuzzy-clj conformance suite validates exactly this
        // bytecode, and like jvm-target (§14.1) it moves only via a decision
        // in the record. NO_COMPATIBILITY was rejected there: it would swap
        // tested bytecode for untested bytecode to save bytes nobody counted.
        jvmDefault.set(JvmDefaultMode.ENABLE)
    }
}

java {
    // Keep javac's -release in lockstep with Kotlin's jvmTarget. Without this
    // the two can silently diverge and produce a jar with mixed class-file
    // versions.
    sourceCompatibility = JavaVersion.toVersion(catalogVersion("jvm-target"))
    targetCompatibility = JavaVersion.toVersion(catalogVersion("jvm-target"))
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = false
    }
}

dependencies {
    // String-form configuration names: robust regardless of whether Gradle
    // generates typesafe configuration accessors for this precompiled script.

    // The stdlib, declared by hand because gradle.properties turns off KGP's
    // automatic injection (CLAUDE.md §14.2). With the injection off, this line
    // is the ONLY thing putting a stdlib on the classpath — deleting it does not
    // fall back to a default, it fails to compile.
    //
    // `api`, matching the scope KGP's own injected dependency uses, so the
    // published POM keeps the shape every other Kotlin library has: kotlin-stdlib
    // at `compile` scope. It is a genuine runtime requirement even for Java
    // consumers — the bytecode references kotlin.jvm.internal.Intrinsics.
    //
    // Resolves to 2.4.10 (stable), NOT the 2.4.20-Beta1 compiler. That gap is
    // the entire point; see gradle/libs.versions.toml.
    "api"(catalogLibrary("kotlin-stdlib"))

    // No junit-bom and no explicit junit-platform-launcher: kotest brings a
    // self-consistent JUnit Platform and pinning our own would fight it across a
    // major version. The reasoning is in gradle/libs.versions.toml, where the
    // absence would otherwise look like an oversight.
    "testImplementation"(catalogLibrary("kotest-runner-junit5"))
    "testImplementation"(catalogLibrary("kotest-assertions-core"))
    "testImplementation"(catalogLibrary("kotest-property"))
}
