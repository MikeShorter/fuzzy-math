# Claude Code task: scaffold fuzzy-math, slice 1

## Read this first
`CLAUDE.md` in the project root is the ratified decision record and the spine of
this project. Read it fully before writing anything. It is authoritative: if you
think something in it is wrong, say so and ask — do not code around it.

Project root: /Volumes/workspace/prod/fuzzy-math  (exists, currently empty
except CLAUDE.md)

## Scope: slice 1 = `fuzzy-algebra` + `fuzzy-laws` ONLY
Do not create the other ten modules. Do not stub them. The Gradle multi-project
skeleton should be structured so they can be added later without rework, but
slice 1 ships exactly two modules.

## Non-negotiables (from CLAUDE.md — restated so you can't miss them)
- Kotlin, **JVM-only** (no KMP). Gradle **Kotlin DSL**, multi-project.
- **Version catalog is mandatory**: every dependency and plugin version lives
  in `gradle/libs.versions.toml` with typesafe accessors (`libs.foo`). No
  version literals inline in any `build.gradle.kts`. This is decided — do not
  propose alternatives.
- **`build-logic/` convention plugins, wired in as an included build** via
  `includeBuild` in `settings.gradle.kts`. NOT `buildSrc`. NOT
  `allprojects`/`subprojects` blocks in a root build file. Also decided — do
  not propose alternatives.
- Package root `dk.eusrbin.fuzzy`, groupId `dk.eusrbin`. Apache-2.0.
- Public API must be usable from **any JVM language**, not just Kotlin:
  - `fun interface` (SAM) for every function-shaped abstraction
  - primitive `double` in and out — no boxing
  - `@JvmOverloads` on defaults; `@JvmStatic`/`@JvmName` on factories
  - no value/inline classes in public signatures; no coroutines
  - operators only alongside a named method (`times` AND `product`)
  - `TNorm` and `TConorm` are separate types despite identical shape
- Every public operation cites its source in KDoc (Zadeh 1965 §/eq., or
  Bergmann 2008 §). See CLAUDE.md §2.

## `fuzzy-algebra` — no dependencies
- degrees: [0,1] as primitive double, validation/clamping helpers
- negations: `Standard` (1−x), Sugeno family, Yager family
- t-norms: Gödel/minimum, Product, Łukasiewicz, Drastic, Nilpotent minimum,
  Hamacher family
- **ordinal sum** construction (Mostert–Shields: the three fundamental
  continuous t-norms + ordinal sum generate all continuous t-norms — this is a
  generating basis, treat it as first-class, not a footnote)
- conorms: duals of the above
- **residuum derived from a left-continuous t-norm** (CLAUDE.md §5) —
  computed, NOT a constructor parameter. Gödel: 1 if x≤y else y. Product:
  1 if x≤y else y/x. Łukasiewicz: min(1, 1−x+y).
- implication families: R- (residua), S- (`N(x) S y`), QL- (`S(N(x), T(x,y))`)
- biresiduum
- `Algebra` bundling (T, S, N, ⇒): named `Standard` (aliases documented:
  Zadeh, Gödel), `Product` (alias Goguen), `Lukasiewicz`.
  Bundle is the front door; raw t-norm params are the escape hatch.

## `fuzzy-laws` — depends on fuzzy-algebra; a CONSUMABLE artifact
This is not an internal test folder. It is published so that a user who writes
their own t-norm can add it in test scope and call `TNormLaws.verify(myTNorm)`.
Design it as a public API with that ergonomics as the goal.

Law tiers (CLAUDE.md §7) — each a separate verifiable suite:
- `TNormLaws` (universal): commutativity, associativity, monotonicity,
  boundary T(x,1)=x, T(x,0)=0
- `DeMorganLaws`: for a dual (T,S,N) triple — Zadeh eqs. 7, 8
- `ResiduumLaws`: the residuation **adjunction** T(x,z) ≤ y ⟺ z ≤ (x⇒y)
- `BLAlgebraLaws`: divisibility, prelinearity
- `MVAlgebraLaws`: Łukasiewicz
- `StandardLaws`: idempotence, distributivity (Zadeh eqs. 9, 10), absorption
  — **these hold ONLY for min/max**

**Tolerances (CLAUDE.md §8) live here, in ONE place, calibrated per algebra.**
min/max are exactly associative and idempotent (exact equality is fine);
Product associativity is NOT exact in IEEE 754; Łukasiewicz suffers
cancellation. Do not scatter epsilons through test files.

**Required test-of-the-test:** `StandardLaws` must FAIL for the Product
algebra — assert that failure explicitly. If distributivity appears to hold for
Product, the suite is broken.

## Testing
Property-based, generating random degrees in [0,1] and random algebras.
Choose the property-testing library (kotest-property is the obvious candidate)
and say why. `fuzzy-laws`' own tests apply its suites to the built-in algebras.
Pin it in the version catalog like everything else.

## Deliverables
1. `settings.gradle.kts` (including `includeBuild("build-logic")`), root
   `build.gradle.kts` (thin — no `allprojects`/`subprojects` config),
   `gradle.properties`, `gradle/libs.versions.toml`, and `build-logic/` with
   the convention plugins: Kotlin/JVM conventions, the JVM-interop compiler
   settings, and publishing/licensing.

   **Sharing the catalog with `build-logic`:** an included build does NOT
   inherit the root catalog. Wire it in `build-logic/settings.gradle.kts` with
   `dependencyResolutionManagement { versionCatalogs { create("libs") {
   from(files("../gradle/libs.versions.toml")) } } }`. Do NOT use a symlink —
   Git for Windows checks symlinks out as plain text files by default unless
   the user has Developer Mode, which would break the build for contributors.
   Note also that typesafe `libs.*` accessors are NOT directly available inside
   precompiled script plugins (a known Gradle limitation); handle it explicitly
   and say which workaround you used and why. Verify the current mechanism
   against the Gradle docs rather than assuming.
2. `LICENSE` (Apache-2.0), `.gitignore`, `README.md` (what/why + the
   substrate-not-control framing from CLAUDE.md §0)
3. `fuzzy-algebra/` and `fuzzy-laws/` per above
4. Maven Central publishing config (POM metadata, signing, javadoc/sources
   jars) — configure it but DO NOT publish

## How I work
- **Verify every plugin/library version against Maven Central before pinning.
  Do not guess versions.**
- I run all tooling myself (gradle, tests). Give me the exact commands; assume
  nothing has been run.
- Design-first: if a real decision surfaces that CLAUDE.md doesn't cover, add
  it there and ask before coding around it.

Start with the Gradle skeleton + CLAUDE.md-conformance check, then
`fuzzy-algebra`, then `fuzzy-laws`. Show me the run commands.
