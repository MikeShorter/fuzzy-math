# fuzzy-math — Decision Record

Convention: decisions live here and are ratified before code. Newest-first.
This file is the spine of the project; if code and this file disagree, this
file is wrong and should be fixed deliberately, not silently.

---

## Updated: 2026-07-15 — Slice 1 scaffolding

### 14. Decisions surfaced while scaffolding — **AWAITING RATIFICATION**

These are choices the founding decisions did not cover, which building slice 1
forced. Per §13 ("if a real decision surfaces that CLAUDE.md doesn't cover, add
it there and ask before coding around it") they are recorded here rather than
buried in a build file. **Each is implemented as a single-line knob**, so
ratifying or reversing any of them is a one-line change and not a refactor.

§14.1 is **ratified**. §14.2–§14.5 still stand open.

#### 14.1 JVM target: toolchain 24, bytecode 17 — **RATIFIED 2026-07-15**

**Decided:** `jvm-toolchain = "24"`, `jvm-target = "17"` in
`gradle/libs.versions.toml`. Compile on the newest JDK; emit bytecode the whole
ecosystem can consume.

The two are **independent knobs and must stay that way**. The toolchain is a
local build detail, invisible to consumers — bump it freely. The target is a
promise to every downstream, and changing it is a decision, not a bump.

**Why 17 and not 24.** The initial instruction was "target Java 24", which was
implemented literally and flagged as in tension with §1, then ratified to 17.
The tension: §1 justifies being public and on Maven Central on adoption grounds
— *"Removing it removes adoption. The artifact's value here is credibility and
lead-generation, not unit sales."* Emitting class file 68 would have:

- excluded **Java 17 and 21** — both LTS, and between them most of the JVM
  installed base;
- pinned to a release that is **not LTS and went EOL in September 2025**, so
  approximately nobody runs Java 24 in production — they are on 21 or 25. The
  floor would have excluded almost everyone *without* including the people it
  nominally targeted;
- bought nothing. `fuzzy-algebra` is arithmetic over primitive doubles and SAM
  interfaces; it uses no API introduced after Java 8.

**Why 17 rather than 21**, given 21 is the current mainstream LTS: there is no
cost to 17. Nothing in the module graph (§10) needs a post-17 language or
library feature — the substrate is arithmetic and interfaces. When something
genuinely does, raise the floor then, deliberately, and note it here. Raising a
floor is easy; lowering one after consumers have adopted you is not.

**Standing rule:** `jvm-target` moves only via a decision in this file.

#### 14.2 Kotlin 2.4.20-Beta1, and the beta stdlib in the published POM

**Instructed:** Kotlin 2.4.20-Beta1, floor 2.4.10. Verified present on Maven
Central 2026-07-15 (the `org.jetbrains.kotlin.jvm` plugin marker is published,
not portal-only). `kotlin = "2.4.20-Beta1"` in the catalog; `"2.4.10"` — the
latest stable — is a one-line change.

**The consequence.** `kotlin.stdlib.default.dependency` is left at its default,
so KGP adds `kotlin-stdlib` **at the compiler's version**. The published POM
therefore carries `kotlin-stdlib:2.4.20-Beta1`, and Gradle's highest-version
conflict rule can pull that beta into a consumer's graph even if they pinned
2.4.10 themselves.

**Options** (the third was recommended and not chosen for you):
1. Leave as-is — simplest, mildly rude to downstreams. **Current state.**
2. Kotlin 2.4.10 everywhere — fully stable, loses the beta compiler.
3. Compile with the beta, publish a stable stdlib: set
   `kotlin.stdlib.default.dependency=false` in `gradle.properties` and declare
   `libs.kotlin.stdlib` (already in the catalog) explicitly at 2.4.10. Reads
   "2.4.10 as absolute bottom" as being about the *published contract*, which
   is arguably what that phrase meant. Small risk: a beta compiler may emit
   references to stdlib members absent from 2.4.10, and KGP warns when the
   stdlib is older than the compiler. Negligible for code this arithmetic.

#### 14.3 Publishing via vanniktech 0.37.0, not plain `maven-publish`

Not covered by §1, which names Maven Central as the channel but not the
mechanism. OSSRH is retired; the stock `maven-publish` plugin has **no route to
the Central Portal** and would leave a manual bundle-zip-and-POST step.
`com.vanniktech.maven.publish` is the de-facto standard for the Portal and also
handles signing, sources/javadoc jars and POM validation.

It is a **build-time** dependency only — it contributes nothing to any published
artifact's runtime graph, so it does not violate §10's "fuzzy-algebra → NO
DEPENDENCIES". Reversible: the convention plugin
(`build-logic/.../fuzzy.publishing-conventions.gradle.kts`) is the only file
that knows about it.

POM `url`/`scm` currently point at `github.com/eusrbin/fuzzy-math`, which is a
**guess** — correct it before the first publish.

#### 14.4 `fuzzy-laws`' published API is dependency-free and hand-samples

§7 makes `fuzzy-laws` a consumable artifact — *"A user writing their own t-norm
adds `fuzzy-laws` in test scope and calls `TNormLaws.verify(myTNorm)`"* — and
§9 forbids coroutines anywhere in the public API. kotest-property's `checkAll`
is `suspend`. These two constrain each other, and the resolution:

- **`fuzzy-laws`' published dependency is `fuzzy-algebra` and nothing else.**
  Its `check`/`verify` API samples `[0,1]` itself: a fixed edge set (boundaries,
  0.5, `MIN_VALUE`, `1 − ulp(1)`, `1/3`) plus seeded `java.util.Random` draws,
  checked over the full cross product. Deterministic by default, so a failing
  report reproduces.
- **kotest-property is used for `fuzzy-laws`' own tests**, in test scope, where
  a coroutine is nobody's problem.

Rationale beyond §9: a consumer on JUnit, TestNG or Clojure `deftest` should not
have to adopt a test framework to check a theorem. Cost: no shrinking in the
published API — acceptable, since the inputs are three bare doubles and there is
nothing structural to shrink to.

`LawViolationException` extends `AssertionError` for the same reason: it must
read as a test failure in a runner `fuzzy-laws` was never told about.

#### 14.5 `TConormLaws` — a seventh suite, beyond §7's six

§7's tier table names six suites. A seventh is implemented: `TConormLaws`.

Reason: §9 makes `TConorm` a **separate type** from `TNorm` precisely so the two
cannot be mixed up. That decision means a consumer who writes their own conorm
cannot check it — `TNormLaws.verify` will not accept it, and no other suite
covers it. A published extension mechanism (§7) with a type you can implement
but cannot verify is incomplete. The same reasoning that justifies the type
justifies the suite.

Trivial to drop if unwanted; nothing depends on it.

---

## Updated: 2026-07-15 — Founding decisions

### 0. What this is

A JVM substrate for **pure fuzzy mathematics**: the algebra, not the industry
that grew out of it.

The gap being filled: essentially every fuzzy library on the JVM
(jFuzzyLogic, FuzzyLite, et al.) is a **fuzzy control** kit — Mamdani/Sugeno
inference, rule bases, FCL. None is a faithful, general implementation of the
underlying mathematics as a reusable substrate. The closest prior art in any
language is Dmitry Kazakov's *Fuzzy sets for Ada* (confidence factors, fuzzy
sets, possibility theory, intuitionistic fuzzy sets, fuzzy numbers, linguistic
variables) — with his control language IFCL built separately **on top**. That
layering is correct and is the architecture adopted here. It is also
unreachable from the JVM.

So: **substrate below, control above, and we only build the substrate.**
Someone else can build the control layer on these artifacts. That is the point.

### 1. Project identity

| | |
|---|---|
| Project | `fuzzy-math` |
| Artifacts | `fuzzy-*` |
| Package root | `dk.eusrbin.fuzzy` |
| Maven groupId | `dk.eusrbin` |
| Build | Gradle, Kotlin DSL, multi-project, **version catalog** (`gradle/libs.versions.toml`) |
| Language | Kotlin, **JVM-only** |
| Visibility | Public, Maven Central |
| License | Apache-2.0 |

**Rationale — name:** "fuzzy-math" is the honest umbrella over sets + logic +
numbers + aggregation without over-promising any one of them. Known collision:
in dev-space "fuzzy" usually means approximate *string matching* (fzf,
fuzzywuzzy). The `dk.eusrbin` groupId namespaces us clear of that crowd and
uses a domain already owned.

**Rationale — Apache-2.0:** JVM default, explicit patent grant, approved by
corporate legal without a meeting. Permissive licensing of the substrate
forecloses nothing later: copyright is retained, so dual-licensing or a
proprietary module on top remains possible. (Not legal advice; get a real
opinion before actually monetising.)

**Rationale — public:** a paid library cannot go on Maven Central, and Maven
Central *is* the distribution channel for JVM libraries. Removing it removes
adoption. The artifact's value here is credibility and lead-generation, not
unit sales.

**Rationale — version catalog:** all dependency and plugin versions live in
`gradle/libs.versions.toml`, never inline in a `build.gradle.kts`. Gradle's
current recommended practice, typesafe accessors, and with twelve eventual
modules sharing pins it is the only sane single source of truth. `fuzzy-bom`
(§10) serves *consumers*; the catalog serves *this build* — they are different
things and both exist.

**Rationale — `build-logic`, not `buildSrc`, not the root build file:** shared
configuration (Kotlin conventions, JVM interop compiler flags, publishing,
licensing) lives in convention plugins under `build-logic/`, wired in as an
**included build** via `includeBuild` in `settings.gradle.kts`. Not a root
build file (`allprojects`/`subprojects` blocks do not scale to twelve modules
and couple everything to everything). Not `buildSrc` (any change to it
invalidates the whole build; an included build caches granularly).

**Rationale — JVM-only, not KMP:** consumers are JAR consumers. KMP would bar
`java.*` and complicate the build for zero benefit.

### 2. The sources are the spec

This library implements published mathematics. Each operation traces to a
source. Where a source and our code disagree, the source wins.

- **Zadeh (1965), "Fuzzy Sets"**, Information and Control 8:338–353 —
  §II definitions, §III union/intersection/complement + properties,
  §IV algebraic operations, §V convexity/boundedness/shadow/separation.
  The core of `fuzzy-set`.
- **Bergmann (2008), *An Introduction to Many-Valued and Fuzzy Logic*** (CUP) —
  §11.7 t-norms/conorms/**implication**, §11.2/11.8/11.9 the Łukasiewicz /
  Gödel / Product systems, §12 MV-algebras, residuated lattices, BL-algebras,
  §16.1 hedges, §17 membership functions. The core of `fuzzy-algebra` and the
  law tiers. (Skip its derivation-system chapters — we are not building a
  theorem prover.)
- **Klement, Mesiar & Pap (2000), *Triangular Norms*** — the t-norm reference.

Note Zadeh's own footnote 3 (p.339): interpreting f_A(x) as truth values makes
fuzzy sets a many-valued logic on [0,1]. Set operations and logical connectives
are the same functions in different hats. This is *why* `fuzzy-algebra` is a
standalone module: it is the connective layer, useful with no set theory at all.

### 3. The capability seam (central design fact)

Fuzzy-set operations split cleanly in two, and the split drives the whole
architecture:

- **Pointwise** — complement, union, intersection, product, sum, hedges.
  Representation-free. Work over *any* X, lazily, given only a membership
  function. No domain knowledge needed.
- **Needs `Sup` over X** — height, support, core, α-cuts, cardinality,
  convexity, shadow/projection, separation degree, relation composition
  (`Sup_v Min[...]`, Zadeh p.346), the extension principle (eq. 23),
  and — surprisingly — **containment, equality, and emptiness**
  (`f_A ≤ f_B` is a ∀ over X, so Zadeh's eq. 2 is *not* pointwise).

You cannot take a Sup over an uncountable X given only a black-box function.

**Decision:** encode the seam in the **type system** via a sealed `Domain`
hierarchy — `Enumerable` (fold over elements), `Sampled` (fold over a numeric
grid; approximate), `Parametric` (analytic), `Opaque` (pointwise only). Asking
for a Sup over an `Opaque` domain must be a **compile error**, not a runtime
failure. This is the single most important thing to let the machine enforce,
and the main reason the implementation language is Kotlin rather than Clojure.

The seam lives *inside* `fuzzy-set`, enforced by sealed types. It is not a
module boundary — splitting it would add friction without adding enforcement.

### 4. Degrees are `Double`. L-fuzzy is a non-goal.

Membership degrees are `[0,1]` as **primitive double**.

The tempting generalisation is Goguen (1967) **L-fuzzy sets** — membership
valued in a lattice L, which subsumes ordinary sets (L={0,1}), fuzzy sets
(L=[0,1]), interval-valued fuzzy sets, Atanassov intuitionistic sets
(L = pairs), and neighbours. Zadeh anticipated it in that same footnote 3
("the range ... can be taken to be a suitable partially ordered set P").

**Decided: not in core, probably never.** Generic-over-L boxes every
evaluation, and the hot path is Sup over a sampled grid — millions of calls.
Boxing there destroys the performance story. Interval-valued and intuitionistic
variants get **separate modules** with concrete representations (pairs of
doubles), not a generic core.

### 5. Implication is derived, not configured

Zadeh 1965 has **no** implication. t-norm theory supplies one canonically: the
**residuum**

    x ⇒ y  =  sup { z ∈ [0,1] | T(x, z) ≤ y }

    Gödel:        1 if x ≤ y, else y
    Product:      1 if x ≤ y, else y/x
    Łukasiewicz:  min(1, 1 − x + y)

The residuation law is an **adjunction**: `T(x,z) ≤ y  ⟺  z ≤ (x ⇒ y)`, i.e.
`T(x,−) ⊣ (x ⇒ −)`.

**Decision:** for a left-continuous t-norm the residuum is *determined*. The
API computes it; it is not a free parameter of `Algebra`. R-implications are
derived; S-implications (`N(x) S y`) and QL-implications (`S(N(x), T(x,y))`)
are provided as separate named families.

### 6. Algebras are bundled; t-norms are generated

**Decision:** the front door is a named `Algebra` bundling `(T, S, N, ⇒)` so
duality cannot be broken by accident. Mixing an arbitrary t-norm with an
arbitrary conorm silently breaks De Morgan; that footgun is not the default.
Raw t-norm parameters remain available as an escape hatch.

Named: `Standard` (aka Zadeh/Gödel: min/max/1−x), `Product` (Goguen),
`Lukasiewicz`.

**Min/max are not a special mechanism — they are just the Gödel t-norm.** There
is one parameterised mechanism, and Zadeh's algebra is its default
instantiation. This retroactively unifies Zadeh §III and §IV: his "algebraic
product" *is* the Product t-norm, and footnote 4's dual `A + B − AB` *is* its
conorm — he filed them separately without noticing they were the same shape.
His `f_A + f_B ≤ 1` side-condition is an uncapped Łukasiewicz conorm missing
its `min(1, ·)`.

By **Mostert–Shields**, every continuous t-norm is an ordinal sum of
Łukasiewicz, Gödel, and Product. **Decision:** provide those three plus an
ordinal-sum construction — a generating basis, not a grab-bag. Additional named
t-norms (Drastic, Nilpotent minimum, Hamacher family) are provided for
convenience; Sugeno and Yager negation families likewise.

### 7. Law tiers, and why they are an artifact

Zadeh notes (p.343) that fuzzy sets form a distributive lattice — but that is a
claim about **min/max specifically**. Distributivity (his eqs. 9, 10) and
idempotence **fail** for Product and Łukasiewicz. Min is the only idempotent
t-norm. The laws therefore stratify:

| Tier | Holds for | Laws |
|---|---|---|
| Universal | any t-norm | commutativity, associativity, monotonicity, boundary `T(x,1)=x` |
| De Morgan | dual (T,S,N) triple | eqs. 7, 8 |
| Residuated / BL | left-continuous T + residuum | residuation adjunction, divisibility, prelinearity |
| MV | Łukasiewicz | MV-algebra axioms |
| Zadeh/Gödel only | min/max | idempotence, **distributivity** (eqs. 9, 10), absorption |

**Decision: publish the law suites as a consumable artifact — `fuzzy-laws`.**
Not internal tests. A user writing their own t-norm adds `fuzzy-laws` in test
scope and calls `TNormLaws.verify(myTNorm)`. This operationalises "the sources
are the spec", makes the library trustworthy in a way docs cannot, and ships
the extension mechanism *with its correctness criteria attached*. `ZadehLaws`
must **fail** for Product — provably, as a test of the test.

### 8. Floating-point reality (tolerances live in `fuzzy-laws`)

The algebraic laws do **not** hold exactly in IEEE 754:

- min/max are exactly associative and idempotent → the Zadeh tier is safe with
  exact equality.
- Product t-norm associativity `(a·b)·c` vs `a·(b·c)` is **not** exact.
- Łukasiewicz suffers cancellation.

**Decision:** law tiers acquire a numerical dimension. Tolerances are
calibrated **per algebra**, defined in one place inside `fuzzy-laws`, never
scattered through test files. (Kazakov's alternative — discretising [0,1] to a
fixed `Resolution` — is noted and **rejected**: it trades the whole continuum,
and the parametric/analytic path, for exactness we can get with tolerances.)

### 9. JVM interop rules (binding on all public API)

Consumers are **any JVM language**, not just Kotlin. The public API is written
to that bar:

- **SAM (`fun interface`) for every function-shaped abstraction** — `TNorm`,
  `TConorm`, `Negation`, `Implication`, `MembershipFn`. Java 8 lambdas satisfy
  them directly; Clojure `reify`s them in one line; no boxing.
- **Primitive `double` in and out.** `MembershipFn<X>` may box X; the *return*
  must stay primitive. Provide a `DoubleMembershipFn` specialisation for X = ℝ
  (Zadeh's common case) to avoid boxing the argument too.
- `@JvmOverloads` on anything with default arguments.
- `@JvmStatic` / `@JvmName` so factories don't land in `FooKt` with mangled
  names.
- **No value/inline classes in public signatures** — name mangling is hostile
  from Java.
- Operators (`a * b`) are Kotlin-only sugar: always ship a **named** method
  (`times`/`product`) alongside.
- **No coroutines** anywhere in the public API.
- Extension functions are sugar only, never core operations (they read as
  statics from Java).
- `TNorm` and `TConorm` are **separate types** despite identical shape — the
  types exist to prevent mixing them up.

### 10. Module graph

Acyclic. Each module independently justifiable.

    fuzzy-algebra        degrees, negations, t-norms, conorms, residua,
                         implication families, Algebra bundles, ordinal sums
                         → NO DEPENDENCIES (deliberate: many-valued logic
                           users want this and no set theory)

    fuzzy-laws           publishable property suites + per-algebra tolerances
                         → fuzzy-algebra (test-scope consumable)

    fuzzy-set            membership fns, pointwise ops, hedges, Domain seam,
                         α-cuts, decomposition, support/core/height,
                         convexity, boundedness, shadow, separation
                         → fuzzy-algebra

    fuzzy-defuzz         centroid, bisector, MOM, SOM/LOM, centre of sums
                         — the sanctioned seam to a control layer above
                         → fuzzy-set

    fuzzy-relation       relations on X×Y, sup-T composition, T-transitivity,
                         similarity, extension principle, cylindrical
                         extension, CRI
                         → fuzzy-set

    fuzzy-number         LR / triangular / trapezoidal / Gaussian,
                         α-cut interval arithmetic
                         → fuzzy-set

    fuzzy-aggregate      OWA, weighted + generalized means
                         → fuzzy-algebra ONLY (aggregation is over *degrees*,
                           not sets — a useful check that the cut is right)

    fuzzy-linguistic     linguistic variables, term sets, hedge application
                         → fuzzy-number

    fuzzy-possibility    possibility / necessity measures
                         → fuzzy-set

    fuzzy-intuitionistic Atanassov (μ,ν) sets, concrete pair-of-doubles repr
                         → fuzzy-algebra

    fuzzy-clj            idiomatic Clojure namespace (reify adapters, sugar)
                         → BOM

    fuzzy-bom            version alignment for consumers

### 11. Non-goals (decided, not deferred)

- **Fuzzy control**: Mamdani/Sugeno inference, rule bases, FCL. Someone builds
  it on top, as Kazakov did. Not here.
- **L-fuzzy / lattice-valued core** — see §4.
- **Neural / GA integration.** (Fuzzy weights explode: no cancellation, widths
  accumulate through layers, and sup-min isn't differentiable. The field went
  probabilistic for good reason.)
- **Neutrosophic sets.** Contested; largely reducible to intuitionistic /
  interval-valued.
Note that defuzzification is **not** on this list — see §11a. It is the
sanctioned seam *to* the control layer, not part of it.

### 11a. Defuzzification is IN — as the seam to the layer above

**Decided (ratified 2026-07-15).** Defuzzification ships, in its own module.

The hesitation was that defuzzification exists only to serve control, which is
a non-goal. The resolution: that is exactly *why* it belongs. A control layer
built on this substrate needs a way back from a fuzzy set to a crisp value.
Providing it is what makes the substrate usable from above; withholding it
would force every consumer to reimplement the same integrals. It is the
handshake — the last thing we ship, the first thing a control layer reaches
for.

Mathematically it is unobjectionable regardless: these are scalar summaries of
a membership function (centroid is `∫x·f(x)dx / ∫f(x)dx`), no more
control-specific than `height` or `core`. Domain-dependent, so it sits above
the capability seam and requires an `Enumerable` or `Sampled` domain.

**Own module (`fuzzy-defuzz`), not folded into `fuzzy-set`**, because:
- it keeps `fuzzy-set` as faithful Zadeh 1965 and nothing else;
- a consumer doing pure set mathematics never needs it;
- naming it as a distinct artifact makes the substrate/control boundary
  *legible in the module graph* rather than buried in a package.

Contents: centroid (centre of gravity), bisector, mean of maxima (MOM),
smallest/largest of maximum (SOM/LOM), centre of sums.

(Alternative considered: fold into `fuzzy-set`'s analysis tier — rejected, it
would blur the one boundary this project exists to draw.)

### 12. Slice 1

**`fuzzy-algebra` + `fuzzy-laws` only.**

They are the foundation everything else stands on; they are independently
useful (Bergmann's many-valued logic needs nothing more); and shipping them
*together* proves the thesis — the laws artifact validates the algebra artifact
from the outside. `fuzzy-set` is slice 2.

### 13. Working discipline

- Design-first: decisions land here and are ratified before code.
- Mike runs all tooling (gradle, tests) and reports results.
- Verify library/plugin versions against Maven Central before pinning — never
  guess.
- Every operation cites its source (§2) in the KDoc.
