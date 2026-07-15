import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Baseline Kotlin/JVM conventions for every fuzzy-* module.
//
// CLAUDE.md §1: JVM-only, not KMP — consumers are JAR consumers.
// Applied via build-logic, never via allprojects/subprojects.

plugins {
    id("org.jetbrains.kotlin.jvm")
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
    //
    // No junit-bom and no explicit junit-platform-launcher: kotest brings a
    // self-consistent JUnit Platform and pinning our own would fight it across a
    // major version. The reasoning is in gradle/libs.versions.toml, where the
    // absence would otherwise look like an oversight.
    "testImplementation"(catalogLibrary("kotest-runner-junit5"))
    "testImplementation"(catalogLibrary("kotest-assertions-core"))
    "testImplementation"(catalogLibrary("kotest-property"))
}
