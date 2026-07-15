import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// CLAUDE.md §9 — JVM interop rules, binding on all public API.
//
// §9 is a set of promises to Java and Clojure consumers. Most of it is enforced
// by hand at the source level (fun interface, primitive double, @JvmStatic,
// no value classes in public signatures). The two parts a compiler flag can
// enforce live here.
//
// Applied on top of fuzzy.kotlin-conventions. Task-level configuration is used
// rather than the `kotlin { }` extension because Gradle only generates typesafe
// extension accessors for plugins named directly in a precompiled script's
// `plugins { }` block — and `fuzzy.kotlin-conventions` is opaque to that
// analysis. KotlinCompile resolves fine: build-logic has kotlin-gradle-plugin
// on its compile classpath.

plugins {
    id("fuzzy.kotlin-conventions")
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.addAll(
            // Interface methods with bodies compile to real Java `default`
            // methods, not to a DefaultImpls sidecar. This is what lets a Java
            // consumer call TNorm.residuum() on a lambda-implemented TNorm,
            // and what makes `fun interface` genuinely SAM-usable from Java.
            //
            // Verified against kotlinlang.org/docs/compiler-reference.html:
            // spelled `-jvm-default` (no X prefix) since Kotlin 2.2; the
            // legacy `-Xjvm-default=all` is superseded. `enable` is already
            // the 2.2+ default — set explicitly because §9 depends on it and
            // a silent upstream default flip would break the promise quietly.
            "-jvm-default=enable",

            // Emit parameter names into the bytecode. Java consumers using
            // reflection, and Clojure's interop, both read better for it.
            "-java-parameters",
        )
    }
}
