# fuzzy-math

**A JVM substrate for pure fuzzy mathematics: the algebra, not the industry that
grew out of it.**

[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](LICENSE)

```kotlin
implementation("dk.eusrbin:fuzzy-algebra:0.1.0")
testImplementation("dk.eusrbin:fuzzy-laws:0.1.0")
```

> **Status: slice 1, pre-release.** `fuzzy-algebra` and `fuzzy-laws` only.
> Nothing is published to Maven Central yet.

---

## The gap this fills

Essentially every fuzzy library on the JVM — jFuzzyLogic, FuzzyLite, and the
rest — is a **fuzzy control** kit: Mamdani/Sugeno inference, rule bases, FCL.
They are built to *do* something with fuzzy logic. None is a faithful, general
implementation of the underlying mathematics as a reusable substrate.

The closest prior art in any language is Dmitry Kazakov's *Fuzzy sets for Ada*:
confidence factors, fuzzy sets, possibility theory, intuitionistic fuzzy sets,
fuzzy numbers, linguistic variables — with his control language IFCL built
separately **on top**. That layering is correct. It is also unreachable from the
JVM.

So: **substrate below, control above, and we only build the substrate.**

Someone else can build the control layer on these artifacts. That is the point,
not a limitation. Fuzzy control is an explicit non-goal here — see
[CLAUDE.md](CLAUDE.md) §11.

## What "the sources are the spec" means

This library implements published mathematics. Every public operation cites its
source in KDoc, and **where a source and our code disagree, the source wins**.

- **Zadeh (1965), "Fuzzy Sets"**, *Information and Control* 8:338–353
- **Bergmann (2008), *An Introduction to Many-Valued and Fuzzy Logic*** (CUP)
- **Klement, Mesiar & Pap (2000), *Triangular Norms*** — the t-norm reference

That is not a bibliography, it is a design constraint. It is also *executable* —
see `fuzzy-laws` below.

## The two artifacts

### `fuzzy-algebra` — no dependencies

Degrees, negations, t-norms, t-conorms, residua, implication families, `Algebra`
bundles, ordinal sums.

It depends on nothing, deliberately. Zadeh's own footnote 3 (p.339) observes
that interpreting a membership grade as a truth value makes fuzzy set theory a
many-valued logic on `[0,1]` — set operations and logical connectives are the
same functions in different hats. So the connective layer is useful with **no set
theory at all**, and people who want Bergmann's many-valued logic and nothing
else should not have to take a set theory to get it.

```java
Algebra a = Algebra.LUKASIEWICZ;
a.and(0.7, 0.6);      // 0.3
a.implies(0.7, 0.6);  // 0.9  — the residuum, derived, never configured
```

Three things worth knowing about the design:

**Min/max are not a special mechanism — they are just the Gödel t-norm.** There
is one parameterised mechanism and Zadeh's algebra is its default
instantiation. This retroactively unifies Zadeh §III and §IV: his "algebraic
product" *is* the Product t-norm, and footnote 4's dual `A + B − AB` *is* its
conorm. He filed them separately without noticing they were the same shape.

**Implication is derived, not configured.** Zadeh 1965 has no implication;
t-norm theory supplies one canonically as the residuum
`x ⇒ y = sup{ z | T(x,z) ≤ y }`. For a left-continuous t-norm that is
*determined* — so the API computes it, and it is not a parameter of `Algebra`.
You cannot pair a t-norm with the wrong implication because you are never asked
for one.

**The three basis norms plus ordinal sum are a generating set.** By
Mostert–Shields, every continuous t-norm is an ordinal sum of Łukasiewicz, Gödel
and Product. Those three plus `TNorms.ordinalSum` are a basis, not a grab-bag.

### `fuzzy-laws` — the correctness criteria, shipped

**This is not an internal test folder. It is a published artifact.**

Write your own t-norm; add `fuzzy-laws` in test scope; find out whether you were
right:

```java
@Test void einsteinIsATNorm() {
    TNorm einstein = (a, b) -> (a * b) / (2 - (a + b - a * b));
    TNormLaws.verify(einstein);   // throws, with a counterexample, if not
}
```

Its only published dependency is `fuzzy-algebra` — no test framework required,
so it works from JUnit, TestNG, or a Clojure `deftest`.

#### The laws stratify, and that is the whole idea

Zadeh notes (p.343) that fuzzy sets form a distributive lattice. True — and a
claim about **min/max specifically**. Distributivity and idempotence *fail* for
Product and Łukasiewicz; min is the only idempotent t-norm. So:

| Tier | Holds for | Laws |
|---|---|---|
| `TNormLaws` / `TConormLaws` | any t-norm | commutativity, associativity, monotonicity, boundary |
| `DeMorganLaws` | dual `(T,S,N)` triple | Zadeh eqs. 7, 8 |
| `ResiduumLaws` / `BLAlgebraLaws` | left-continuous `T` + residuum | residuation adjunction, divisibility, prelinearity |
| `MVAlgebraLaws` | Łukasiewicz | MV-algebra axioms |
| `StandardLaws` | **min/max only** | idempotence, distributivity (eqs. 9, 10), absorption |

**The one thing to take away:** a law you read in Zadeh 1965 is not
automatically a law of your algebra. Distributivity is the one people carry over
by habit, and it is false the moment you switch to the Product t-norm — silently,
with every result still a plausible-looking number in `[0,1]`.

`StandardLaws.check(Algebra.PRODUCT)` **must fail**, and the test suite asserts
that it does. A law suite that passes everything is indistinguishable from one
that checks nothing.

#### Tolerances live in one place

The laws do not hold exactly in IEEE 754. `min`/`max` are exactly associative
and idempotent, so the Zadeh tier is checked at `ε = 0`; Product associativity
is not exact; Łukasiewicz suffers cancellation. Tolerances are calibrated per
algebra in `Tolerance`, and nowhere else. No epsilons are scattered through test
files.

## Interop: any JVM language, not just Kotlin

Binding on all public API:

- `fun interface` (SAM) for every function-shaped abstraction — Java 8 lambdas
  satisfy them directly, Clojure `reify`s them in one line
- **primitive `double` in and out** — no boxing on the hot path
- `@JvmStatic` / `@JvmField` on factories and constants; `@JvmOverloads` on defaults
- no value/inline classes in public signatures (name mangling is hostile from Java)
- no coroutines, anywhere
- operators are Kotlin-only sugar, always alongside a named method
- `TNorm` and `TConorm` are **separate types** despite identical shape — the
  types exist to prevent mixing them up

```clojure
(reify TNorm (apply [_ a b] (* a b)))
```

## Building

The Gradle wrapper is committed, so a JDK is the only prerequisite.

```bash
./gradlew build                  # compile + test
./gradlew publishToMavenLocal    # install locally to try it in another project
```

## Roadmap

Slice 1 is `fuzzy-algebra` + `fuzzy-laws`, shipped together because the laws
artifact validates the algebra artifact from the outside. `fuzzy-set` — the
`Domain` capability seam, α-cuts, convexity, shadows — is slice 2. The full
twelve-module graph, and the reasoning behind each cut, is in
[CLAUDE.md](CLAUDE.md) §10.

## License

Apache-2.0. See [LICENSE](LICENSE) and [NOTICE](NOTICE).

The mathematics is published work and is not owned by this project; the code is.
