# fuzzy-math

**A JVM substrate for pure fuzzy mathematics: the algebra, not the industry that
grew out of it.**

[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](LICENSE)

```kotlin
implementation("dk.eusrbin:fuzzy-algebra:0.1.0")
implementation("dk.eusrbin:fuzzy-set:0.1.0")
implementation("dk.eusrbin:fuzzy-number:0.1.0")
testImplementation("dk.eusrbin:fuzzy-laws:0.1.0")
```

> **Status: pre-release.** `fuzzy-algebra`, `fuzzy-set`, `fuzzy-number` and
> `fuzzy-laws` — Zadeh 1965 §II–§V complete, plus exact α-cut arithmetic.
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

That is an *arbitration* rule, so it has a precondition: **the source must be
consultable.** An unreachable source cannot arbitrate, and citing one as though
it could is a promise the project cannot keep. So the KDoc distinguishes two
kinds of claim and does not render them identically:

- **`Source:`** — read and checked against a text on hand. Falsifiable, and
  someone has tried.
- **`Attributed:`** — a belief about who is owed credit. Standard in the
  literature, not verified here, and *labelled* so you know which you are
  looking at.

On hand, and therefore able to arbitrate:

- **Zadeh (1965), "Fuzzy Sets"**, *Information and Control* 8:338–353
- **Bergmann (2008), *An Introduction to Many-Valued and Fuzzy Logic*** (CUP)

Not on hand, and therefore **nothing specific hangs on it**: Klement, Mesiar &
Pap (2000), *Triangular Norms*. It is a general reference. Where it was once
cited for a construction, the primary is named instead — Hamacher 1978, Sugeno
1977, Yager 1980, Fodor 1995, Mostert–Shields 1957 — and marked `Attributed:`,
because that is the true state. The mathematics is verified independently by
`fuzzy-laws` regardless of who is owed the credit: misattribution is the
exposure, being wrong is not.

This is not fastidiousness. The index to these sources was folklore until it was
checked, and **one entry was wrong** — union and intersection were cited to the
wrong section *and* the wrong equation numbers, in a shipped artifact, in direct
contradiction of this project's own decision record. A citation nobody checks is
decoration; a confidently wrong one is worse than none, because it is the one
thing a reader will not re-derive.

That is not a bibliography, it is a design constraint. It is also *executable* —
see `fuzzy-laws` below.

## The four artifacts

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

### `fuzzy-set` — Zadeh 1965, faithfully

Membership functions, the pointwise algebra (§II–§IV), hedges, and the `Domain`
capability seam.

```java
MembershipFn<String> warm = x -> x.equals("warm") ? 0.75 : 0.25;
Domain<String> terms = Enumerable.of("cold", "warm", "hot");

FuzzySets.union(warm, cool);       // pointwise — needs no domain
warm.height(terms);                // a Sup — needs one
```

**The domain is a parameter, not part of a set's type.** A fuzzy set *is* its
membership function — Zadeh's §II definition says the function characterises the
set completely, so nothing else is carried. Operations split cleanly: pointwise
ones (union, complement, hedges) need nothing; anything requiring a `Sup` over X
takes a `Domain`. You cannot ask for a supremum you cannot compute, because it
is a required argument.

The surprise is which side containment falls on. `f_A ≤ f_B` is a **∀ over X**,
so Zadeh's eq. (2) needs a domain — while union and intersection do not.

**A ∀ over a grid is not a proof.** Over a `Sampled` domain, "no counterexample"
means only that none turned up among the points looked at, so containment returns
a three-valued `Verdict` — `Proven` / `Refuted` / `NotRefuted` — rather than a
boolean that would assert a proof nobody performed. A witness *found* on a grid
refutes absolutely; sampling is lossy in one direction only.

### `fuzzy-number` — exact α-cut arithmetic

Triangular, trapezoidal and Gaussian fuzzy numbers, and arithmetic on them.

```java
FuzzyNumber x = TriangularNumber.of(1, 2, 3);   // "about 2"
FuzzyNumber y = x.times(x);
y.alphaCutInterval(0.5);                        // [2.25, 6.25] — exact
```

**`×` returns the exact answer, not a triangle.** Triangular numbers are closed
under `+` and `−` and **not** under `×`: the α-cut of `T(1,2,3)` is `[1+α, 3−α]`,
so the product's is `[(1+α)², (3−α)²]` — *quadratic* in α, where a triangle's is
linear. Every mainstream library returns `T(1,4,9)` anyway. Look at where that
lies:

| α | exact | the triangle `T(1,4,9)` | error |
|---|---|---|---|
| 0.0 | `[1.000, 9.000]` | `[1.000, 9.000]` | 0.000 |
| 0.5 | `[2.250, 6.250]` | `[2.500, 6.500]` | **0.250** |
| 1.0 | `[4.000, 4.000]` | `[4.000, 4.000]` | 0.000 |

**It is exact at the support and at the peak — the two things anyone
spot-checks — and wrong through the entire interior.** That is why the
approximation is everywhere and why nobody notices.

It is also unnecessary. The exact product is still a fuzzy number (its α-cuts
stay nested), so it is exactly representable by its α-cut map — and here it is
even writable in closed form: `μ(y) = min(√y − 1, 3 − √y)` on `[1,9]`. The
choice was never "approximate or refuse". It was **approximate or be right**. If
you want the triangle, ask for it explicitly and own the loss.

**`X ⊖ X` is not crisp zero**, and that is not a bug. α-cut interval arithmetic
has no cancellation — the two `X`s are independent quantities that happen to
share a range — so `T(1,2,3) ⊖ T(1,2,3)` is a fuzzy number spread symmetrically
about zero, `[−2, +2]` wide at the support and `{0}` only at `α = 1`. Ordinary
interval arithmetic has the same trap for the same reason.

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
| `MembershipFnLaws` | any membership fn | degrees in `[0,1]`; **a closed-form override may not be worse than the fold it replaced** — and must *equal* it over an exhaustive domain |
| `ZadehSetLaws` | sets over an algebra | Zadeh's own set-level claims — eqs. 7, 8, 9, 10, 15, 19 |
| `DecompositionLaws` | any membership fn | `A = ⋃ α·Γ_α` round-trips |
| `ConvexityLaws` | any `DoubleMembershipFn` | strong ⟹ convex; a `Proven` must survive the generic search |
| `FuzzyNumberLaws` | any `FuzzyNumber` | α-cuts are **nested** — the precondition `AlphaCutNumber` cannot check at construction |

**The one thing to take away:** a law you read in Zadeh 1965 is not
automatically a law of your algebra. Distributivity is the one people carry over
by habit, and it is false the moment you switch to the Product t-norm — silently,
with every result still a plausible-looking number in `[0,1]`.

`StandardLaws.check(Algebra.PRODUCT)` **must fail**, and the test suite asserts
that it does. A law suite that passes everything is indistinguishable from one
that checks nothing.

#### Tolerances live in one place

The laws do not hold exactly in IEEE 754. `min`/`max` are exactly associative and
idempotent, so the Zadeh tier is checked at `ε = 0`; Product associativity is not
exact; Łukasiewicz suffers cancellation. Tolerances are calibrated in
`Tolerance`, and nowhere else. No epsilons are scattered through test files.

Calibration follows the **operation**, not the algebra's name — a distinction the
suites had to learn the hard way. `Standard` is `min`/`max` **and `1 − x`**, and
`1 − x` is arithmetic rather than lattice selection, so it is not exact:
`1 − (1 − ⅓) = 0.33333333333333326`. Calibrating `EXACT` for the *algebra* made
Zadeh's own complement fail its own suite. An expression is no more exact than
its sloppiest term.

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

Slice 1 was `fuzzy-algebra` + `fuzzy-laws`, shipped together because the laws
artifact validates the algebra artifact from the outside. Slice 2a added
`fuzzy-set`: the `Domain` seam, the pointwise algebra, hedges, α-cuts and
decomposition — everything in Zadeh 1965 that is domain-generic.

Slice 2b added Zadeh §V: convexity, strong convexity, separation and the shadow.
§V needs a **vector space**, not merely a domain — convexity is
`f_A[λx₁ + (1−λ)x₂] ≥ Min[f_A(x₁), f_A(x₂)]` (eq. 25), and forming that segment
needs arithmetic on X itself, which is why those live on `DoubleMembershipFn`
rather than one level up. Zadeh states the precondition himself on p.347: *"we
assume for concreteness that X is a real Euclidean space Eⁿ."*

Two of §V's properties **did not ship, and the reasons are recorded**: strict
convexity is vacuous in ℝ¹ (every convex subset of ℝ is strictly convex — it needs
a dimension to work in), and boundedness is unsamplable from a bounded window in
either direction. Three of §V's four theorems are tested but not shipped as law
suites, because each is conditional on convexity and a grid never *proves*
convexity — so a failure would indict the sampler, not the code.

**That completes Zadeh 1965.** `fuzzy-number` then took the first step past it:
fuzzy numbers, `Interval`, and exact α-cut arithmetic. That module exists to test
a decision rather than to add a feature — the claim that a parametric function may
override an analysis operation with its closed form. Building it split the claim
in two and corrected it: an override is valid where the operation **means the same
thing**, and where the return type cannot hold the closed form, that is not a
barrier but a **signal that one name is covering two questions**. Hence `alphaCut`
(which grid points are in `Γ_α`) alongside `alphaCutInterval` (what `Γ_α` *is*),
and the integral sent to `fuzzy-defuzz` rather than smuggled in as a σ-count.

What remains of the twelve-module graph — `fuzzy-relation`, `fuzzy-defuzz`,
`fuzzy-aggregate`, and the rest — is later mathematics built on this substrate.
The graph and the reasoning behind each cut are in [CLAUDE.md](CLAUDE.md) §10.

## License

Apache-2.0. See [LICENSE](LICENSE) and [NOTICE](NOTICE).

The mathematics is published work and is not owned by this project; the code is.
