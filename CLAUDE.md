# fuzzy-math — Decision Record

Convention: decisions live here and are ratified before code. Newest-first.
This file is the spine of the project; if code and this file disagree, this
file is wrong and should be fixed deliberately, not silently.

---

## Updated: 2026-07-16 — fuzzy-clj + fuzzy-bom design

### 23. The first non-Kotlin consumer — **ALL RATIFIED 2026-07-16**

§9 is the largest ratified claim in the record that nothing had ever tested,
and after the first Central publish its defects become permanent (§14.3). This
slice ran the consumer before the publish. The headline: **§9 held — zero
adapters needed** — and the section's findings are mostly about what that means
for the module §10 promised.

#### 23.1 Erratum: §22's opening sentence, corrected

§22 opened by claiming everything after fuzzy-defuzz was source-gated.
`fuzzy-clj` and `fuzzy-bom` need no source at all — one is adapters and sugar,
the other version alignment; neither is mathematics, so §18.2 has no
jurisdiction. Corrected in place. Recorded here because the failure mode is
§17.1's wearing a new coat: the defuzz brief said "the one module left that
needs no source we do not have", *meant* the mathematical tail, did not say so
— and §22 hardened the imprecision into a claim that the project was blocked.

#### 23.2 The bytecode, read — and `-jvm-default` pinned

`javap` on the real class files, per §17.1's discipline (§16.5's *"it inherits
the lot"* was folklore until someone read the bytecode):

- **`MembershipFn`**: `apply` abstract; all ~17 analysis members are genuine
  JVM `default` methods. §16.5 is **true in the class file**.
- **`DoubleMembershipFn`**: `applyAsDouble` abstract at exactly `(D)D`, plus a
  primitive `apply(double)` default and the boxed bridge. §16.1's promise is
  **kept**, for the consumer it named.
- **`TNorm`**: `apply(DD)D` abstract; `invoke`/`residuum` default.

**And one knob nobody chose:** `$DefaultImpls` classes sit alongside every
interface — the 2.4.20-Beta1 compiler is in `-jvm-default=enable`
(compatibility) mode *by its own default*, recorded nowhere. That is exactly
the state §14.1/§14.2 exist to prevent. **Decided: pinned explicitly to
`enable` in `fuzzy.kotlin-conventions`** — ratifying the precise bytecode the
conformance suite validated. Rejected: `no-compat` (drops `$DefaultImpls`,
smaller jars) — it would swap tested bytecode for untested bytecode to save
bytes nobody counted; revisit deliberately if ever, via this record. **The
standing rule extends: `-jvm-default` moves only via a decision in this file**,
§14.1's rule arriving at its sibling.

#### 23.3 The experiment: §9 held, and the adapter-detector reports a null

Ten sections of Clojure against the real jars (no Kotlin, no kotest), consumed
through a `deps.edn` of `:local/root` jars. Per the governing rule — *every
adapter fuzzy-clj turns out to need is a §9 defect, and the fix belongs
upstream* — the deliverable table:

| §9 promise | result |
|---|---|
| *"Clojure `reify`s them in one line"* | **true, verbatim** — `(reify TNorm (apply [_ a b] …))`, inherited `residuum` works |
| §14.4: laws without adopting a framework | **kept** — `TNormLaws/verify` on the reified t-norm; a broken one throws `LawViolationException`, and `instance? AssertionError` → `true`, so it reads as a failure in a runner `fuzzy-laws` was never told about — that decision's dividend, collected |
| §16.5's inherited members | all reachable from a one-line reify |
| §16.1's primitive path | `applyAsDouble` reify drives `height`, `findNonConvexity`, `Defuzzifiers` |
| §16.4: *"idiomatic Java, not ceremony"* | from Clojure: `condp instance?` + `.getWitness`, three readable lines — **the cost was priced correctly** |
| §21.2's `kotlin.Pair` | works — compose and `findNonTransitivity` run; needs a type hint and `.getFirst`/`.getSecond` with casts |

**Adapters required: zero. The candidate detector — "an adapter that has to
exist is a §9 defect" — never fired. Null result, reported as one** (§21.5's
discipline): §9 is simply true, vindicated by its first real consumer, which is
the best available outcome and not a boring one.

Three qualifications, none a defect, all pinned as executable facts:

1. **Clojure 1.12's functional-interface coercion does not reach these SAMs** —
   a bare `#(…)` fails with a `ClassCastException`. §9 promised `reify`, not
   coercion, so nothing breaks — but the assumption that 1.12 made SAM-wrapping
   obsolete is dead, and the conformance suite asserts the failure so a future
   Clojure that fixes it announces itself (§7's test-of-the-test).
2. **`Enumerable.of` is ambiguous at arity 1 from Clojure** (vararg vs
   `Collection` overload) — resolves reflectively to `Collection`, works, warns.
   An ergonomic nick, not a §9 breach.
3. **`kotlin.Pair`: kept, and priced.** The stdlib is already a hard `api`
   dependency whose version §14.2 controls, so the type costs a *name* in
   consumer source, not a new dependency. Measured pain: one hint, two accessor
   calls. `Map.Entry` would be no better from Clojure; a bespoke pair type is a
   new concept (§15.2). Cost of changing — `Product`, `shadow`, all of
   `fuzzy-relation` — dwarfs the benefit. Permanent after first publish,
   accepted with eyes open.

§21.7's deferred question (does `Verdict` want a conjunction?) was **not met**:
nothing in ten sections needed to combine verdicts. Still deferred, now with
one consumer's evidence that the need is not urgent.

#### 23.4 The §15.2 verdict: the published sugar module deletes itself

*"What does fuzzy-clj actually add beyond kebab-case?"* — measured: **almost
nothing**. Every operation is one clean interop call. A published sugar module
would be abstraction on spec for a consumer who demonstrably does not need it —
§21.2's test, failed the same way one slice later.

**Decided: `fuzzy-clj` ships as an *unpublished conformance module*** — the
brief's own framing (*"fuzzy-clj is fuzzy-laws for §9"*), purified: a
`clojure.test` suite wired into `./gradlew check`, so **every build re-proves
§9 from Clojure**, and a regression — a member losing its `default` body, a
signature going hostile, the `-jvm-default` knob drifting — fails the build
before it can reach a publish. Its audience is this repository and the §9
contract, not Maven Central.

This also dissolves the §10 collision the brief flagged: the many-valued-logic
Clojure user §10 protects depends on `fuzzy-algebra` directly — which the
experiment just proved is pleasant — rather than being handed a sugar module
that drags in set theory.

**The knob:** if a real Clojure consumer ever asks for idiom — kebab-case,
`fn`→SAM wrappers (justified by qualification 1), vector→`Enumerable` — publish
it *then*, additively, with the conformance suite as its ready-made test bed.

#### 23.5 `fuzzy-bom`, and §10's arrow deleted — wrong three times, as suspected

`fuzzy-bom` is a Gradle `java-platform`: version constraints on the **six
published modules** (`fuzzy-algebra`, `fuzzy-set`, `fuzzy-number`,
`fuzzy-relation`, `fuzzy-defuzz`, `fuzzy-laws`). It does **not** list
`fuzzy-clj`, which is unpublished.

§10's `fuzzy-clj → BOM` arrow is deleted: (1) it was a different *kind* of
arrow — every other §10 line is a compile-path dependency on code, and a BOM
contains none; (2) it omitted the real accretion — the conformance harness
depends test-scope on every published module, the same shape §10's note
predicted for `fuzzy-laws`; (3) **verified, not inherited: tools.deps supports
only jar artifacts — Maven BOM / `dependencyManagement` import is
unimplemented, an open feature request** (clojure.org's deps.edn reference;
the tools.deps repository). The arrow pointed the module's own audience at a
facility their build tool cannot use. The BOM serves Maven and Gradle
consumers; the conformance module's `deps.edn` documents per-artifact pinning
by example.

**Central only, no Clojars** — one line, so nobody re-litigates: Clojars
matters for artifacts Clojure consumers *depend on*, and after §23.4 no
published artifact is Clojure-specific; deps.edn resolves Maven Central by
default, which the experiment used. Revisit only if §23.4's knob ever turns.

#### 23.6 Scaffolding, each with its knob (§13: versions verified, not recalled)

- **Clojure `1.12.5`** — latest stable on Central, checked 2026-07-16 (1.13 is
  alpha). Pinned in the version catalog like everything else.
- **Conformance runner: a hand-rolled `JavaExec` on `clojure.main`**, wired
  into `check`. Rejected: the clojurephant Gradle plugin — a third-party plugin
  is a heavier dependency than one task, for one module that compiles nothing.
  The knob is one task registration.
- **`fuzzy.platform-publishing-conventions`** — the existing publishing
  convention is hard-wired to vanniktech's `KotlinJvm`, which a `java-platform`
  cannot use. The shared POM moved to a helper both conventions call; the
  platform convention configures `JavaPlatform()` instead. vanniktech 0.37.0
  re-verified current on Central.

---

## Updated: 2026-07-16 — fuzzy-defuzz design

### 22. Defuzzification — **ALL RATIFIED 2026-07-16**

§11a's module, and the last **mathematical** one §18.2's rule leaves standing:
everything else in §10's *mathematical* tail needs a source not on hand.

> **Corrected by §23.1 (2026-07-16):** as first written, this sentence claimed
> the whole tail was source-gated. False — `fuzzy-clj` and `fuzzy-bom` are not
> mathematics, so §18.2 has no jurisdiction over them, and nothing blocked the
> next slice. §17.1's rule, applied to schedules: a confidently wrong statement
> about what happens next misleads more than a wrong citation. §11a's claim — *scalar summaries of a
membership function*, σ-count-shaped — was **verified rather than inherited**:
the formulas are self-describing arithmetic, and only the names are attribution.
**Bergmann contains zero occurrences of any of it** — "defuzz", "centroid",
"gravity", "mean of maxima", "bisector", "Mamdani" all absent; its one "maxima"
hit is lattice talk and its "control" hits are the journal name — checked, not
assumed (§17.4's discipline). Zadeh 1965 predates defuzzification entirely. So
the module is **`Attributed:` throughout**, primaries named and not on hand
(Mamdani & Assilian 1975; Van Leekwijck & Kerre 1999 as the survey), exactly as
`fuzzy-number` was (§20.6).

#### 22.1 §20.1(b)'s debt does not land — `h` cancels, and §20.1(b) is amended

The brief's algebra, verified numerically (§14.6's standing lesson): a uniform
grid with spacing `h` gives `∫x·f dx ≈ h·Σxf` and `∫f dx ≈ h·Σf`, and the
centroid is their **ratio**, so `h` cancels exactly:

    T(0,1,3), true centroid 4/3:
    n =   64      Σf =   31.5      Σxf/Σf = 1.3333333333
    n = 4096      Σf = 2047.5      Σxf/Σf = 1.3333333333
                  ^ scales with n — the σ-count non-integral, unchanged
                                    ^ flat — the ratio is h-free

**And bisector too**: partial sums are compared against half the total — `h` on
both sides — and the grid bisector converges to the analytic `3 − √3`
(1.8e-2 → 1.8e-4 over 64 → 4096 points). No operation in this module consumes a
standalone `∫f dx`.

**Decided: `integral` does not ship.** §20.1(b)'s *split* was right — `Σ` and
`∫` are two questions — but its *placement* was wrong: it exported the second
question to a module where the integral only ever appears in a ratio that
reduces to the σ-count expression again. Shipping it to keep the promise would
be API with no consumer — §14.6(c)'s "suppression with a nicer name" in a
different register. §15.2's lesson, third application: the concept you delete
is the one you were right about. §20.1(b) carries the amendment note.

**The precondition the algebra needs, stated because the brief missed it:**
the cancellation assumes **uniform spacing**, which [Sampled] guarantees and
`Enumerable<Double>` does not. Over an `Enumerable` the same expression is the
centroid **of the enumerated points as discrete mass** — duplicates counted
twice, §16.3's σ-count semantics arriving at a second operation — which is the
honest answer for a discrete carrier and a density-biased one for a caller
hand-rolling a non-uniform "grid" as an `Enumerable`. One question, two
carriers, both answers stated in the KDoc.

#### 22.2 The §20.9 case — and the gap it exposes in §21.3

**§20.9 predicted this module's worst bug cleanly, one slice in advance, no
bending.** `MOM = mean(maximalGradeSet(over))` inherits the *virtual* `height`
through a default body two calls deep — exactly the shape §20.9 named — and for
every fuzzy number whose peak misses the grid, the inherited analytic `1.0`
empties the filter:

    T(-1, 0.5, 2) over 512 points:
    filter f(x) >= height(over)      → 0 elements    (mean of the empty set)
    filter f(x) >= the fold's own max → 2 elements    → mean = 0.5   (true MOM: 0.5)

That is **the normal case for the module's main input**, not a corner.

**Decided: the defuzzifiers filter against the fold's own maximum — question
and filter at one fidelity.** That set is never empty (a nonempty grid attains
its fold max, and §16.3 guarantees nonempty), and it converges to the true
answer. Rejected: the tolerance band `f ≥ height − ε` — `alphaCutTouchesWindow`'s
ghost (§19.6), ε unmotivated, and unnecessary once the fidelities are coherent.
Rejected: §18.3's name-both — the return type holds the true answer fine and
nobody asks the grid-maxima question standalone, so it stays a private
computation. **The two-questions structure appeared; the return-type detector
did not fire; not a sixth arrival.** (Bisector-over-grid vs over-ℝ: also null —
a converging sample of one question, [Sampled]'s standing caveat.)

**The reason under the fix is a gap in §21.3, and the gap is the finding.**
"Mixes fidelities it cannot control" — why *cannot*? By §21.3's own rule, MOM
should be a **member with overrides**: `FuzzyNumber` earns its keep
independently (§20.2) and the closed forms are sitting there — `T(l,m,r) → m`,
`Tz(a,b,c,d) → (b+c)/2`, `Gaussian → mean` — with MOM's universe supplied, not
fixed, so §20.8's dispatch applies unchanged. It cannot be a member because
**§11a put it in a downstream module**. §21.3's rule was derived from two cases
that both lived in the same module as their type; this is the first query
*downstream of the type it queries*, and the rule is silent there — a gap, not
a contradiction. The rule that fills it, with its boundary stated:

> **A query that cannot host its own overrides must not consume a virtual
> answer — it can neither match the fidelity nor correct it.** It computes at
> the one fidelity it owns: the fold's.

**And this prices something §11a did not.** §11a bought boundary legibility;
the bill, now itemised: five queries permanently denied the override path,
settling for an O(n) converging answer where an O(1) exact one exists. **§11a
holds** — the answers converge, the cost is small, and the boundary is the
thing this project exists to draw — but it is a cost paid, not a cost absent.
The knob, recorded in advance: if it ever bites, `FuzzyNumber` gains an
analytic `meanOfMaxima` **in `fuzzy-number`** — additive, no graph change, no
§11a reversal.

#### 22.3 Zero found mass: the operations throw — and the epistemic status splits by carrier

Every operation here divides by a fold — `Σf` for centroid and bisector, the
maximal degree for the maxima family — and when the fold finds nothing, the
quotient is not a point of X. IEEE's answer is `NaN` (verified: `0.0/0.0`);
`0.0` is a plausible-looking `x`; §11a says this value reaches an actuator.

**An earlier draft called the zero fold "definitive rather than epistemic",
and over a [Sampled] domain that is backwards.** `Σf = 0` on a grid means no
mass was found *at the sampled points*: a spike narrower than the spacing —
`T(0.5000001, 0.5000002, 0.5000003)` over `Sampled(0, 1, 1024)`, constructible
today — has mass on the window while every grid point reads zero. That is a
**negative finding from a sampled search**, and the record's most-arrived-at
rule (§16.4, §19.7(2), §20.7, §21.5's null) already governs it: *positive
findings are facts; negative findings are not.* Over an **`Enumerable`** the
universe is the element set, and `Σf = 0` **is** §II p.340's emptiness, proven.
Over a `Sampled` it is `NotRefuted`-shaped. The coherence test that catches the
wrong wording: `checkEmptiness(over)` — the operation callers are pointed at —
returns `NotRefuted` on that grid, and `height(over)` returns **`1.0`** for the
spike via §20.8's override. An exception claiming "no mass on this window"
would contradict both, from the same library, about the same set.

**Decided: throw, on the thinner reason that survives.** The fold found no
mass, **so there is no quotient to return, and returning one would invent it**
— whatever the epistemic status of the emptiness. The message reports *what was
done*, not what is true: **"no mass found on this domain"** — §20.7's shape,
once more. Rejected: `NaN` (the language's answer; a silent `0.0` with extra
steps); nullable/`Verdict`-shaped returns (§9 keeps the return primitive — and
the caller who wants the three-valued answer already owns it:
`checkEmptiness(over)` shipped in 2a, and is named in the message).

**This is the second refusing operation, not the first, and one slice after
§21.5 it is close to a rule, so it is stated as one:**

> **An operation may refuse when the refusal costs nothing and the alternative
> is a fabricated answer.** §21.5's `imageUnder` refuses on a *structural fact
> about the carrier* (an O(1) field read); these refuse on a *negative search
> finding* (a branch on the divisor already in hand). Both satisfy §4's real
> concern — no fold is added; the check is the computation — and both replace
> an invented value with a stop.

Why `height` returns `0.0` in the same situation without complaint, since
someone will ask: `height` is documented as a **lower bound**, and `0.0` is an
honest lower bound. A centroid has no lower-bound reading — a wrong point of X
is not a bound on anything, it is just wrong.

**Message vocabulary, decided rather than let in by habit:** the exception
message stays substrate-only. The KDoc — which already speaks at the §11a seam
— carries the control-layer translation (that a zero fold is the
*no-rules-fired* state a controller must expect, and `checkEmptiness` is how to
test for it first). §21.6's line, applied to strings: the control word does its
work at the boundary and stops there.

#### 22.4 Centre of sums: dropped — for the reason that survives a source arriving

Two reasons, and the durable one first. **COS's natural signature takes a
collection of per-rule output sets — control structure in a substrate
signature.** That is §11's line, not a sourcing problem, and it holds even if a
definitive formula turns up tomorrow. Second, the sourcing is also fatal today:
the formula is genuinely variant in the literature (overlap counted once or
twice; "sum" as algebraic sum or union), and no text on hand defines *any*
version — **intensification-shaped** (§17.4: the *formula* unverifiable, not
merely the name), and §17.4 says those drop.

The §18.2 table for the slice:

| operation | ships | authority |
|---|---|---|
| `centroid` | ✔ | formula self-describing (`Σxf/Σf`); **`Attributed:`** the name and the notion, control literature, not on hand |
| `bisector` | ✔ | first grid point where the cumulative reaches half the mass; **`Attributed:`** likewise |
| `meanOfMaxima`, `smallestOfMaxima`, `largestOfMaxima` | ✔ | mean/min/max of the fold-fidelity maximizing set (§22.2); **`Attributed:`** the names ("MOM"/"SOM"/"LOM" are control acronyms and are not used) |
| centre of sums | **dropped** | §11's structure objection + §17.4's formula objection, above |
| `integral` | **not shipped** | §22.1 — no consumer; §20.1(b) amended |

#### 22.5 §11a's domain restriction is **static** — the case §16.4 does not cover

§11a says these operations *"require an `Enumerable` or `Sampled` domain"*,
which reads like §21.5's runtime guard. It is not, and no mechanism ships: a
centroid needs **arithmetic on X**, so the operations live on
`DoubleMembershipFn` and take `Domain<Double>` — and `Product<A,B>` is a
`Domain<Pair<A,B>>`, which is never a `Domain<Double>`. The type system
delivers §11a's restriction with no guard, no overload, no `require`.

Recorded because §21.5 was about getting this distinction wrong in the other
direction: **exhaustiveness is computed, not declared (§16.4) — but the element
type really is static.** A runtime `require` here would re-check what the
compiler already proved; a static type for exhaustiveness would have refused
`Product(Enumerable, Enumerable)`. Each constraint goes where its knowledge
lives.

#### 22.6 Names, the seam, and why there is no `DefuzzLaws`

**The module name carries the control word deliberately; nothing inside does.**
§21.6 declined to carry "CRI" into an API; `fuzzy-defuzz` *is* the artifact
name — and that is §11a's own argument, not a contradiction: *"naming it as a
distinct artifact makes the substrate/control boundary legible in the module
graph rather than buried in a package."* The boundary word does its work at the
boundary. Inside: object `Defuzzifiers` (mirroring the artifact, house
pattern), operations `centroid`, `bisector`, `meanOfMaxima`,
`smallestOfMaxima`, `largestOfMaxima` — mathematical names, acronyms not
carried.

**Decided: no published `DefuzzLaws` suite — because there is no subject.**
This is §21.2's test one module later: `TNormLaws.verify(myTNorm)` has a
`myTNorm`; a `DefuzzLaws.verify(…)` has no argument to take, because §22.2's
gap just closed the override path — nothing here is consumer-extensible, and
every candidate law (`SOM ≤ MOM ≤ LOM`, centroid within the window) holds for
every input by arithmetic construction. **A published suite would be an
extension point's criteria with no extension point — the exact inverse of
§14.5's defect, and §7 forbids both.** The module's central facts are pinned in
`fuzzy-laws`' **own tests**, where `fuzzy-number` and `fuzzy-defuzz` are both
on the path (the accretion §10 anticipated, paying off): the **throw** for
`T(-0.5, 0.5, 1.5)` over `Sampled(2, 3)` asserted as a fact; the spike whose
`height` is `1.0` while the centroid refuses — the §22.3 wording, executable;
the §20.9 case returning `≈ 0.5` instead of crashing; the 4/3 convergence and
the h-cancellation.

**Consequence, ratified:** the `fuzzy-laws → fuzzy-defuzz` edge is
**`testImplementation`, not `api`** — the first test-scope-only edge in the
graph, and a deviation from §10's accretion note justified by that note's own
rationale: *the suites follow the modules they validate*, and here there is no
suite. No defuzz type appears in any `fuzzy-laws` signature.

Tolerances in those tests calibrate per operation and are never `EXACT`
(§14.6(a), §21.9): the centroid is a ratio of sums — arithmetic end to end —
and even the maxima family ends in a computed mean.

---

## Updated: 2026-07-16 — fuzzy-relation design

### 21. Fuzzy relations — **ALL RATIFIED 2026-07-16**

This slice finishes Zadeh 1965: the fuzzy relation (p.345), composition (p.346)
and eqs. (22)/(23) were the last unshipped equations in the paper. Designed
against the scan first, per §17.1's discipline — and the scan corrected the
brief three times before any code existed. The first draft of two decisions
below was wrong, and both corrections came from the record itself (§16.4,
§20.5), which is the record doing its job.

#### 21.1 pp.344–346 read off the scan — three corrections to folklore

**A fuzzy relation is homogeneous.** P.345, prose, no equation number: *"a
fuzzy relation **in X** is a fuzzy set in the product space **X × X**"* — with
the `x ≫ y` example *"regarded as a fuzzy set A in R²"* (`f_A(10, 5) = 0`,
`f_A(100, 10) = 0.7`, `f_A(100, 1) = 1`), and p.346's n-ary case in
`X × X × ⋯ × X`. **The "relations on X×Y" phrasing in §10 and in `Product`'s
KDoc is later convention, not the paper.** The heterogeneous signature ships —
the formula never uses `X = Y`, and the types catch argument-order mistakes for
free (§21.4) — but it is **derived**; `Source:` is the homogeneous statement.

**Composition** (p.346, prose, **no equation number**, matching §17.1's index):
*"the composition of two fuzzy relations A and B is denoted by B ∘ A and is
defined as a fuzzy relation in X whose membership function is related to those
of A and B by `f_{B∘A}(x, y) = Sup_v Min [f_A(x, v), f_B(v, y)]`"* —
associativity stated in prose, unnumbered, no proof shown. The notation names
the result right-to-left while `A` takes the first leg: the misreading the
brief predicted, and the reason for §21.4's argument order.

**Eq. (22) is pointwise, and the brief mis-filed it.** *"The inverse mapping
T⁻¹ induces a fuzzy set A in X whose membership function is defined by
`f_A(x) = f_B(y), y ∈ Y` (22), for all x in X which are mapped by T into y"* —
i.e. `f_A(x) = f_B(T(x))`: the **preimage**, a trivial total combinator. No
Sup, no preimage-set machinery, no domain, no soundness question. Only (23)
has the constrained-Sup problem the brief filed them both under.

**Eq. (23) says Max, not Sup** — `f_B(y) = Max_{x∈T⁻¹(y)} f_A(x)` — **and the
paper's Max/Sup usage is deliberate.** Zadeh writes `Sup` wherever attainment
is not in hand: `M = Sup_x f_A(x)` (p.348, and twice on p.349), composition's
`Sup_v` (p.346), the shadow's `Sup_{x₁}` (p.350), separation's
`M = Sup_x Min[…]` (p.352). He writes `Max` exactly twice, both where
attainment is established: p.349's uniqueness argument — *"if A is strongly
convex and x₀ **is attained** … which contradicts `M = Max_x f_A(x)`"* —
switching from the `Sup_x` written two paragraphs earlier *in the same
argument*, precisely at the sentence where attainment becomes a hypothesis;
and (23), whose framing is finite throughout (*"two or more distinct points"*,
*"the larger of the two grades"*). **Suggestive, not decisive** — for constant
`T`, `T⁻¹(y)` is all of X and Max would need an attainment he does not argue —
but it means §21.5's exhaustiveness guard agrees with the paper's own notation
rather than merely with our seam: his (23) selects from a set he treats as
attained, which is what an exhaustive domain delivers and a grid does not.

#### 21.2 No `FuzzyRelation` type — a relation *is* a `MembershipFn<Pair<X, Y>>`

The brief's null hypothesis, confirmed by its own test. Zadeh is unambiguous
(p.345): a fuzzy relation **is** a fuzzy set in a product space, nothing more —
so `MembershipFn<Pair<X, Y>>` already is one, and §15.1 says not to put in a
type what a parameter carries.

The test that decides it: **would anything override?** Parametric relations
with analytic answers exist in principle — `e^{−|x−y|}` is product-transitive
by the triangle inequality; a crisp order's indicator is min-transitive — but
**this module ships no parametric relation type.** Nothing like
`TriangularNumber` is waiting with a closed form. A `fun interface
FuzzyRelation` would be an abstraction bought on spec, and §15.2's lesson is
that the concept you delete is the one you were right about.

The module is therefore **operations**: `FuzzyRelations`, `@JvmStatic`, plus
the queries of §21.7 — whose placement forced §21.3.

**The knob that reverses it, recorded in advance:** if a parametric relation
ever arrives (Zadeh 1971's fuzzy orderings, should the source arrive),
introduce `FuzzyRelation<X> : MembershipFn<Pair<X, X>>` **then**, put the
queries on *it* as members whose default bodies call the statics' generic
search, and the `FuzzyNumber` precedent applies unchanged. Additive; nothing
shipped blocks it. What is **forbidden** is `instanceof` dispatch inside the
statics — that is a virtual table built by hand, and §15.3 exists to say the
language already has one.

#### 21.3 §16.5 amended: when a query is a member — and the first draft was wrong

§16.5 puts queries on `MembershipFn` as overridable members. Reflexivity,
symmetry and transitivity are queries, and cannot be members: they constrain
`X`'s *shape* (`X = Pair<Y, Y>`, the same `Y` on both sides — all three need
homogeneity: `f(x,x)`; `f(x,y)` against `f(y,x)`), and Kotlin has no
conditional members. So §16.5 needs an amendment, stated as a rule.

**An earlier draft proposed the wrong rule** — *"queries parametric in X are
members; queries that constrain X's shape are statics"* — **and shipped code
refutes it.** `findNonConvexity` constrains X's shape (`X = Double`; §15.5 put
it there), is a member of `DoubleMembershipFn`, is overridden by
`TriangularNumber`, and that override is where §20.5's `Proven` dividend came
from. The rule as drafted would have made it a static and deleted `Proven`.

The shape constraint was never the issue — it just means a member needs a
**subtype** to live on, and `DoubleMembershipFn` is one. The real question is
whether such a subtype is *justified*: `DoubleMembershipFn` earns itself on
§16.1's primitive path, and the convexity queries ride along; a
`FuzzyRelation` subtype would exist *only* to host queries nothing overrides.

**Decided — the amendment to §16.5:**

> §16.5's queries/combinators line coincided with overridable/not-overridable.
> **The coincidence is not identity. A query is a member when there is a
> subtype that both earns its keep independently and has a closed form to
> override with. Otherwise the virtual slot is decoration.**

That is §7's ethic (do not ship an extension point without its criteria) and
§15.2's (delete the concept) arriving at the same place. It collides with
nothing shipped: every §16.5 member either lives on `MembershipFn` itself or
on a subtype (`DoubleMembershipFn`, `FuzzyNumber`) that §16.1/§20.2 justified
independently, with real overrides (§20.1, §20.5, §20.8). §21.2's reversing
knob is unchanged.

> **Gap found by §22.2 (2026-07-16):** this rule was derived from cases living
> in the same module as their type, and is silent on a query **downstream** of
> the type it queries. `meanOfMaxima` meets both of the rule's conditions —
> `FuzzyNumber` is a justified subtype with closed forms — and still cannot be
> a member, because §11a put it in `fuzzy-defuzz`. The rule that fills the gap
> is §22.2's: a query that cannot host its own overrides must not consume a
> virtual answer.

#### 21.4 `compose` — sup-T by derivation, direct fold, and an `EXACT` law for any T

**Sup-min composition derives from shipped machinery.** It is the shadow of the
intersection of two cylindrical extensions — `shadow` (§19.2), `intersection`
(§II), and `f(x,y) = f_A(x)` — so §19.2's claim that `Product` "earns itself
twice" comes true, and the module is smaller than §10 implies.

**That derivation settles the sup-T question, and more cheaply than §17.3
had to.** The brief asked whether substituting `T` for `Min` in someone else's
formula differs from instantiating an owned mechanism. We do not substitute:
the `Min` in `Sup_v Min[…]` *is* the intersection of the cylindrical
extensions, `intersection` is **already parameterised by algebra** under §6's
ratified rule, and sup-T composition is the same composite with that existing
parameter left visible. **`Source:`** p.346 for the default (min);
the parameterisation inherits §6; **`Attributed:`** the name "sup-T
composition". The `Sup` itself is **not** parameterised — no source on hand
says anything about inf-S or other outer folds, and nothing asks for them.

**Ship the direct fold; assert the derivation.** The derived form allocates an
extra `Pair` per grid point; the shipped body is
`f(x, y) = Sup_v T(f_A(x, v), f_B(v, y))` directly. The identity is asserted by
`fuzzy-laws` instead of paid for at runtime — `boundedDifference`'s precedent
verbatim (§17.3).

**The agreement law is `EXACT`, for any T, and deserves its reason rather than
an assertion.** Both forms compute the same multiset of degrees
`{T(f_A(x,v), f_B(v,y)) | v}` — `T` touches the *degrees*, never the
*accumulator*. The accumulator's reducer is `max` regardless of T, and §8's
ratified point is that max is exactly associative (and commutative), so
order-independence is free and `EXACT` is sound whatever T does. That is
§14.6(a)'s "tolerance calibrates per operation, not per algebra" paying out:
the operation here is *selection*, even when its inputs were arithmetic.

**Argument order: path order, and the types enforce it.**

    compose(first: MembershipFn<Pair<X, V>>,
            second: MembershipFn<Pair<V, Y>>,
            over: Domain<V>,
            algebra: Algebra = STANDARD): MembershipFn<Pair<X, Y>>

Arguments read left-to-right along the path x→v→y. This is Zadeh's `B ∘ A`
with `first` = his A, `second` = his B; his notation names the result
right-to-left like function composition, and the KDoc says so, citing p.346.
In the heterogeneous case **passing them in the wrong order is a compile
error** — a second dividend of the derived generalisation, and the reason not
to fear the notation trap.

**Laziness is real and documented, not discovered.** The result re-folds
`Domain<V>` on every evaluation; composing k relations multiplies the cost.
Goes in the KDoc — `shadow` already documents the identical caveat.

**Associativity ships as a law** (`Source:` p.346, stated unnumbered): sound
over `Enumerable` for *any* monotone T — over a finite fold,
`T(a, max(b, c)) = max(T(a,b), T(a,c))` needs only monotonicity — with
tolerance `forTNorm` (T is applied twice in different orders; §8), collapsing
to `EXACT` for `STANDARD`, where T is min and everything is selection.

#### 21.5 Eq. (23) is unsound over a non-exhaustive domain — and the first cure proposed was the wrong one

**The brief's suspicion is confirmed, and it is worse than the brief says.**
`f_B(y) = Max_{x∈T⁻¹(y)} f_A(x)` is a Sup constrained to a *selected* subset,
and over a `Sampled` X both halves fail:

- **The selection is empty.** Membership in `T⁻¹(y)` is the comparison
  `T(x) == y` — an exact comparison meeting a *computed* value, §20.2(iii)'s
  shape exactly. A grid almost surely contains no point of `T⁻¹(y)`, the fold
  runs over nothing, and its `initial` — `0.0` — comes back as *the* image.
  §16.3's lie, arriving where construction cannot reject it.
- **Refinement never fixes it.** `T⁻¹(y)` is typically measure-zero, so a
  finer grid still misses it — forever. This is *not* `height`'s "lower bound
  that converges": **it converges to nothing.**

Over an **exhaustive** domain neither problem exists: the preimage within the
enumerated universe is exact selection, the fold is Zadeh's own finite Max
verbatim, and an empty preimage honestly means "no x maps to y" — a fact, not
a fold artifact — for which `0.0` is the right image.

**An earlier draft restricted the signature to `Enumerable<X>` — and §16.4 had
already rejected that by name.** Exhaustiveness is *computed, not declared*:
`Product.isExhaustive` is a runtime conjunction, and the case a static type
cannot express is `Product(Enumerable, Enumerable)` — which §16.4 calls the
*motivating case* for `Product`, which §15.4 justified by *this module*, and
which is exactly the domain a relation on X×X analyses over. The
`Enumerable`-only signature would have had the paper-finishing operation
refuse the module's own domain. (§16.4's boolean overload is no counter-model:
it is sugar *alongside* a general `Verdict` method, honest because the general
mechanism exists. Here the restricted signature would have *been* the API.
And reaching past §16.4 back to §3's compile-error ambition was reaching past
the section that settled how exhaustiveness is actually known.)

**Decided: `imageUnder(a, mapping, over: Domain<X>)` with
`require(over.isExhaustive)` — it throws.** §16.3's cure, taken with its
precedent: the diagnosis was §16.3's, so the fix is too. This does not breach
§4's "operations do not validate": §4 governs *values* (a γ, a λ) and protects
the hot path from per-call folds — this is a one-time O(1) field read at
construction of the lazy result, and unlike `separationDegree`'s convexity
precondition (§19.3, a fold the caller may already have done), **the caller
cannot know better than the domain does** — `isExhaustive` *is* the domain
answering.

No `Enumerable<X>` sugar overload: §16.4's sugar pays for itself by changing
the *return type* to the boolean that is honest there. Here the return type
would be `MembershipFn<Y>` either way — a second spelling of the same call,
not sugar. If a caller wants a compile-time guarantee they pass an
`Enumerable`; the signature does not need to say it twice.

**The pattern-watch verdict (the brief asked for an honest null):** the
*detector* fired a fifth time — a fold whose `initial` comes back as the
answer is a return value that cannot carry the truth. But the *resolution* is
not §18.3's "name both": the grid-restricted image is not a second question
anyone wants (it is almost surely the zero function). One question, one
carrier that cannot answer it → guard the carrier. Fifth arrival of the
detector; **null result on the name-both resolution.** And it is not §20.8
wearing a hat: the universe is supplied and read correctly; the defect is that
the *selection within it* is empty for reasons the caller cannot see.

#### 21.6 CRI — §10 vs §11 adjudicated: §10's *word* was wrong, not its substance

§14.5's model, applied. §10 lists "CRI" in this module; §11 makes fuzzy
control a non-goal; CRI is what Mamdani inference is built from. The
inconsistency is real and the resolution is that **§10 named a substrate
operation by its control-layer name.**

What sits under "CRI" is the **relational image**:

    f_B(y) = Sup_x T(f_A(x), f_R(x, y))

which is `shadow(intersection(cylindricalExtension(a), r), over)` — fully
derived from read sources, like §21.4 — and which **generalises eq. (23)**:
when R is the crisp graph of a function T, it reduces to (23) exactly.

**Decided:** ship it as `imageUnderRelation` (with `cylindricalExtension`,
whose mathematics is the trivial `f(x,y) = f_A(x)` and whose *name* is
attributed — 1975 vocabulary, not in the paper). The KDoc notes that the
control literature builds the compositional rule of inference from this
composition (**`Attributed:`** Zadeh 1973, **not on hand**) and the name CRI
is not carried into the API. That is §11a's own move: the seam ships, the
control vocabulary does not.

**The bypass, said out loud rather than found by a reader:**
`imageUnderRelation` with a crisp graph *is* eq. (23), so the silent `0.0` is
one call away from the operation that refuses it, and the line had better be
defensible. It is, and the KDoc draws it: `imageUnder` *selects* — over a
grid its answer degenerates for **every** non-constant mapping, which is why
it is guarded — while `imageUnderRelation` *folds degrees* with no selection
anywhere: over a grid it is an honest converging lower bound (`height`'s
standing caveat, §18.2) for any continuous R, and degenerates only when R is
indicator-like, concentrated on a measure-zero set — at which point the caller
has hand-encoded a selection as a relation, and the KDoc says that doing so
reintroduces exactly the failure `imageUnder`'s guard exists to stop.

#### 21.7 The queries: three `Verdict` statics; "similarity" dropped; a question deferred

`findNonReflexivity`, `findNonSymmetry`, `findNonTransitivity` — statics on
`FuzzyRelations` per §21.3, each returning a `Verdict` per §16.4, each with the
formula stated in its own KDoc:

| query | checks | witness |
|---|---|---|
| `findNonReflexivity` | `f(x, x) = 1` ∀x | the `x` — one call re-derives it |
| `findNonSymmetry` | `f(x, y) = f(y, x)` ∀x,y | the `(x, y)` — two calls re-derive it |
| `findNonTransitivity` | `T(f(x,v), f(v,y)) ≤ f(x,y)` ∀x,v,y | a witness class: `(x, via, y)` **plus the two degrees** — the composed degree is *arithmetic* for a non-min T, so it is carried rather than recomputed, `ConvexityWitness`'s reason (§19.1, §19.7) |
| "similarity" | — | **dropped, below** |

**Authority:** the formulas are stated in our own KDoc and are unambiguous,
checkable properties; the **names** are `Attributed:` — Zadeh 1971 is **not on
hand**, and variants exist in the literature (ε-reflexivity, weak
reflexivity). The `dilation` precedent (§17.4): naming a function is not a
claim about mathematics, and each of these is a correct, self-describing
operation whatever 1971 turns out to call it.

**The comparisons are IEEE comparisons, and the KDoc says so** in
`checkEquality`'s stance (§18.2): for a non-min T the composed degree is
computed, so a `Refuted` attests the floating-point inequality — the only one
the machine can attest. The witness carries both degrees so the law suite can
reproduce it exactly (§19.7(3)'s self-consistency), rather than re-deriving
through arithmetic that might round differently.

`findNonTransitivity` searches triples **directly** rather than via
`compose`-then-`checkContainment`: same O(n³), but the witness is the triple a
reader re-derives by hand, and — §20.9's rule, applied in advance — a law
defined through `compose`'s default body would inherit `compose`'s guards. The
direct search's law is witness self-consistency, owed to nobody.

**"Similarity relation" is dropped, and the reason is §19.6's, not
"wrongness".** An earlier draft argued a wrong bundle has no residual value —
overreach: reflexive ∧ symmetric ∧ T-transitive is a well-defined predicate
whatever it is called, and by §17.5 misattribution is the *smaller* failure.
The argument that holds is thinner and sharper: **every conjunct is already
shipped and already checkable, so the bundle's only content is the claim that
this conjunction is what Zadeh 1971 means — which is precisely the
unverifiable part.** `power(0.5)` earns its place independently and the name
rides along; a `checkSimilarity` would be an attribution wearing a function's
clothes — `alphaCutTouchesWindow`'s objection (§19.6) with the nouns changed.
When Zadeh 1971 arrives, shipping it is one function and zero unknowns.

**Noticed, deliberately not answered here: does `Verdict` want a
conjunction?** The three queries return `Verdict<Double>`-, `Verdict<Pair>`-
and `Verdict<Witness>`-shaped answers, and three witness types do not conjoin
— a caller combining them writes a `when`, not an `&&`, and `Verdict<W>`
cannot unify them without a sum type. Nothing in this slice needs it; a
"similarity" bundle would have, which is one more reason it was the wrong
thing to ship first. Recorded now rather than met at module nine.

#### 21.8 Module wiring, laws, and the audit

`fuzzy-relation → fuzzy-set` (§10). `fuzzy-laws → fuzzy-relation` — the edge
§10's note already anticipated; still acyclic, still test-scope-consumable.

`RelationLaws` ships what is sound and nothing else (§19.7's standard):

1. **Derivation agreement** — `compose` equals its shadow/intersection/
   cylindrical-extension derivation, `EXACT` for any T (§21.4's reasoning).
   Same for `imageUnderRelation`.
2. **Associativity** over `Enumerable` factors — tolerance `forTNorm`,
   `EXACT` for `STANDARD`.
3. **Witness self-consistency** — a returned transitivity witness must
   reproduce: re-evaluating both degrees matches what it carries, and the
   inequality holds. §19.7(3)'s shape.
4. **Eq. (23) agreement** — `imageUnder(a, T, over)` equals
   `imageUnderRelation(a, graph(T), over)` over an exhaustive domain, which
   pins the §21.6 bypass line as an executable fact.

**Test-of-the-test (§7):** a crisp finite order (`≤` as an indicator over an
`Enumerable`) is min-transitive → `Proven` — reached through
`Product(Enumerable, Enumerable)`, finally exercising §16.4's motivating case,
which nothing had; a deliberately intransitive fixture → `Refuted` with a
reproducing witness; `imageUnder` over a `Sampled` domain → **throws**,
asserted as a fact.

**The audit.** With this module built, §11a's claim — *"faithful Zadeh 1965 and
nothing else"* — is checkable for the first time, so here is the check: every
numbered equation (1)–(32), every unnumbered definition and theorem, each
marked with what happened to it and where. Statuses: **shipped** (code),
**law** (a published `fuzzy-laws` suite), **pinned** (`fuzzy-laws`' own tests,
where a law would be unsound — §19.7), **dropped**/**out** (with the section
that decided it). Everything cites code that exists today; nothing below is
from memory.

**§II — Definitions (pp.339–341)**

| | item | status | where |
|---|---|---|---|
| — | `f_A(x)`, grade of membership, p.339 | shipped | `MembershipFn.apply` / `DoubleMembershipFn.applyAsDouble` |
| — | *empty* (prose, p.340) | shipped | `MembershipFn.checkEmptiness` → `Verdict`; `FuzzySets.constant(0.0)` is the set itself |
| — | *equal* (prose, p.340) | shipped | `MembershipFn.checkEquality` → `Verdict` |
| (1) | complement `1 − f_A` | shipped | `FuzzySets.complement`, default `Negations.STANDARD` |
| (2) | containment `f_A ≤ f_B` | shipped | `MembershipFn.checkContainment` → `Verdict` (§16.4) |
| (3) | union `Max` | shipped | `FuzzySets.union`, default `Algebra.STANDARD` (§6) |
| (4) | union, abbreviated `∨` | notation | same operation as (3) |
| (5) | intersection `Min` | shipped | `FuzzySets.intersection`, default `Algebra.STANDARD` |
| (6) | intersection, abbreviated `∧` | notation | same operation as (5) |

**§III — Properties (pp.342–343)**

| | item | status | where |
|---|---|---|---|
| (7)/(8) | De Morgan | law | `ZadehSetLaws` (set level); `DeMorganLaws` (degree level) |
| (9)/(10) | distributivity | law | `ZadehSetLaws`; **fails for `PRODUCT` by design**, asserted (§7) |
| (11)/(12) | the membership-function identities of (9)/(10) | law | the pointwise form is literally what `ZadehSetLaws` evaluates at each `x` |
| (13) | the sieve example, Fig. 3 | example | nothing to ship — its content (composing `∨`/`∧` expressions) is the combinators |
| — | lattice remark, p.343 | law | `StandardLaws` — the tier that is min/max-only (§7) |

**§IV — Algebraic operations (pp.344–346)**

| | item | status | where |
|---|---|---|---|
| (14) | algebraic product `f_A f_B` | shipped | `FuzzySets.algebraicProduct` — and it **is** the Product t-norm (§6); agreement asserted in `fuzzy-laws` |
| (15) | `AB ⊂ A ∩ B` | law | `ZadehSetLaws` |
| (16) | algebraic sum, partial | shipped | `FuzzySets.algebraicSum`, faithful to the proviso; `checkAlgebraicSumDefined` makes the side-condition executable |
| — | absolute difference (prose, p.344) | shipped | `FuzzySets.absoluteDifference` |
| — | footnote 4: `A ⊕ B = A + B − AB` | shipped | the Product conorm — `union(a, b, Algebra.PRODUCT)` (§6) |
| (17)/(18) | convex combination, **Λ a fuzzy set** | shipped | `FuzzySets.convexCombination` (§17.2) |
| (19) | `A ∩ B ⊂ (A,B;Λ) ⊂ A ∪ B` | law | `ZadehSetLaws` |
| (20) | `Min ≤ λf_A + (1−λ)f_B ≤ Max` | shipped/law | the scalar `convexCombination` overload is its λ; its content is what the eq. (19) law checks pointwise |
| (21) | recovering Λ: `(f_C − f_B)/(f_A − f_B)` | **dropped** | an existence remark, partial where `f_A(x) = f_B(x)` (0/0); nothing asks for it; one lambda away if anything ever does |
| — | fuzzy relation (prose, p.345) | shipped | as the *judgment* `MembershipFn<Pair<X, Y>>` — deliberately no type (§21.2); homogeneous in the paper (§21.1) |
| — | n-ary relation (prose, p.346) | expressible | nested `Pair`/`Product`; no dedicated API — §15.4's wall is the reason to not encourage it |
| — | composition `Sup_v Min` (prose, p.346) | shipped | `FuzzyRelations.compose`, sup-T per §21.4; derivation agreement is a `RelationLaws` law, `EXACT` |
| — | associativity of ∘ (prose, p.346) | law | `RelationLaws`, tolerance `forTNorm`; a non-associative "T" fails it, asserted (§7) |
| (22) | preimage `f_A = f_B ∘ T` | shipped | `FuzzyRelations.preimageUnder` — pointwise, total (§21.1) |
| (23) | image `Max_{x∈T⁻¹(y)} f_A(x)` | shipped, **guarded** | `FuzzyRelations.imageUnder` — throws unless `isExhaustive` (§21.5); generalised by `imageUnderRelation` (§21.6); agreement a `RelationLaws` law at `forTNorm` (§21.9) |

**§V — Convexity (pp.347–353)**

| | item | status | where |
|---|---|---|---|
| (24) | α-cut `Γ_α` | shipped | `MembershipFn.alphaCut`; exact interval form `FuzzyNumber.alphaCutInterval` (§20.2) |
| (25) | convexity | shipped | `DoubleMembershipFn.findNonConvexity` → `Verdict<ConvexityWitness>` (§19.1); ℝ¹ per §15.5 |
| (26)–(30) | the intersection-theorem proof steps | proof internals | subsumed by the theorem row below |
| — | *"If A and B are convex, so is their intersection"*, p.347 | pinned | `ZadehConvexityTest` — a law would blame the sampler (§19.7); the coarse-vs-fine grid demonstration is pinned alongside |
| — | boundedness, p.348 | **dropped** | §19.6 — unsamplable in either direction; documented as `separationDegree`'s unchecked precondition |
| — | `M = Sup_x f_A(x)`, p.348 | shipped | `MembershipFn.height` (§18.2); override law `≥`/`==` per §20.7 |
| — | the ε-neighbourhood Lemma, p.348 | out | topological, needs Eⁿ (§18.3) — the reason `maximalGradeSet` drops *"essentially"* |
| — | strict convexity, p.349 | **dropped** | §19.5 — vacuous in ℝ¹: every convex `Γ_α ⊆ ℝ` is strictly convex |
| — | strong convexity, p.349 | shipped | `DoubleMembershipFn.findNonStrongConvexity`; strong ⟹ convex is a `ConvexityLaws` law (§19.7(1)) |
| — | `C(A)`, the core, p.349 | shipped | `maximalGradeSet` (de-topologised, §18.3) alongside the modern `core` |
| — | *"core of a convex set is convex"*, p.349 | pinned | `ZadehConvexityTest` (§19.7) |
| — | p.350 corollary (E¹ uniqueness) | pinned | `ZadehConvexityTest` — pinned as **not grid-checkable**: two grid points tie across a peak (§19.7) |
| — | shadow, p.350 | shipped | `FuzzySets.shadow` — domain-generic (§19.2), a slice early |
| — | shadow preserves convexity, p.350 | out | a theorem about Eⁿ convexity; ℝⁿ is out (§19.4) |
| — | `S_H(A) = S_H(B) ∀H ⇒ A = B`, p.350 | out | ranges over arbitrary hyperplanes (§19.4) |
| (31)/(32) | separation `D = 1 − M̃`, `M̃ = Inf_H M_H` | **not implementable** | the definition is an Inf over every hypersurface; what ships is p.352's **theorem** |
| — | p.352 theorem: `D = 1 − M` for bounded convex sets | shipped | `DoubleMembershipFn.separationDegree`, preconditions in KDoc, unchecked per §4 (§19.3); pinned in `ZadehConvexityTest`, not a law — checking it against its own implementation would be circular (§19.7) |

**Every equation in the paper is now accounted for.** Nothing shipped lacks a
row; every drop names the section that decided it. The two entries a reader
should notice: (21) is the only *equation* dropped for want of a consumer
rather than for unsoundness, and (31)/(32) is the only definition the paper
states that no computer can implement — which is why §19.3 ships the theorem
and says so.

#### 21.9 First test run: the boundary axiom is **arithmetic** in Łukasiewicz — §14.6(a)'s twin

One failure on the first run, and it was the suite catching its own author
again. The eq. (23) agreement law shipped at `EXACT`, on the claim that
`T(d, 1) = d` and `T(d, 0) = 0` are *"boundary selections, not arithmetic"* in
every shipped t-norm. `verifyImage(Algebra.LUKASIEWICZ)` refuted it while both
operations under test were correct.

`T(a, 1) = a` is the t-norm boundary axiom — an identity of ℝ.
`TNorms.LUKASIEWICZ` computes `max(0, a + b − 1)` literally, and `(a + 1) − 1`
transits the neighbourhood of 1, where a double resolves only to
`ulp(1) = 2.2e-16`:

    a = 0.1    (a + 1) − 1 = 0.10000000000000009     Δ = 8.3e-17
    a = 0.9    (a + 1) − 1 = 0.8999999999999999      Δ = 1.1e-16
    min(a, 1)  ·  a × 1  ·  max(0, a + 0 − 1)        all exact

**This is §14.6(a) with the signs changed** — there `1 − (1 − a)`, here
`(a + 1) − 1`, both an exactness claim over-applied to arithmetic near 1, both
caught by the suite failing for a correct subject on its first run. The
distinction that survives: whether the boundary axiom's *implementation*
selects or computes is **a fact about each t-norm, not about t-norms** —
`min(a, 1)` selects, `a × 1` happens to be exact by IEEE multiplication, and
Łukasiewicz rounds. There was no way to know but running it.

**Decided:** the agreement law calibrates per §14.6(a) — `forTNorm(T)`, which
collapses to `EXACT` for `STANDARD` and lands Łukasiewicz on `ARITHMETIC`
(1e-14 ≫ 1.1e-16). The **derivation** laws stay `==`, deliberately: they
compare identical arithmetic in identical fold order, a claim about the fold
that no t-norm can break — and the broken-t-norm test proves the suite keeps
the two apart, failing associativity for a non-associative "T" while the
derivation law survives it.

The over-claim never reached this record — §21.8 promised the agreement as an
executable fact, not an exact one — but the *reason* belongs here: **wherever
a law crosses a t-norm's boundary axiom, the law inherits the t-norm's
arithmetic, not the axiom's exactness.** That is §20.9's inheritance rule and
§14.6(a)'s calibration rule arriving at the same sentence.

---

## Updated: 2026-07-16 — fuzzy-number design

### 20. Fuzzy numbers — **ALL RATIFIED 2026-07-16**

This slice exists to test §15.3, which is an unproven claim: nothing in
`fuzzy-set` overrides anything. Designing `TriangularNumber` against it produced
a sharper answer than "it works" or "it doesn't".

#### 20.1 §15.3 needs exactly one qualifier — and it predicts §18.3

**The headline.** §15.3 says closed forms are *"overrides on the function"*, a
`TriangularNumber` overriding `height` *"with the analytic answer"*. Taking every
analysis member of `MembershipFn` in turn, §15.3 splits **two** ways — an earlier
draft said three, and the correction is the finding.

**(a) It works — the closed form answers the same question.**

| override | `TriangularNumber(l, m, r)` |
|---|---|
| `height(over)` | `1.0` — normal by construction |
| `isNormal(over)` | `true` — follows `height` |
| `findNonConvexity(over)` | **`Verdict.Proven`** — see §20.5 |
| `findNonStrongConvexity(over)` | `Verdict.Refuted`, with an analytic witness: any two points outside `(l, r)` have `f = 0`, and `0 > 0` is false |

Each ignores the domain entirely, exactly as §15.3 promised.

**An earlier draft listed `maximalGradeSet` → `[m]` and `core` → `[m]` here. Both
are wrong, and §20.7 is why:** those return *elements of the domain*, and `m` need
not be one. An override handing back a point the domain does not contain is
answering a different question — group (b), not (a).

**(b) The return type cannot carry it — which is not a failure. It is a
detection.**

`support(over): List<X>` and `alphaCut(over, α): List<X>` cannot express a
triangle's `Γ_α = [l + α(m−l), r − α(r−m)]`, an uncountable interval.
`sigmaCount(over): Double` *can* carry a number, but `(r−l)/2` is the integral,
not `Σ_x f(x)` over the domain — a σ-count over 1024 grid points is ~256× one over
4, and neither is `∫f dx`.

Those look like two different problems and are one:

- `alphaCut` asks *"which grid points are in `Γ_α`"*; `alphaCutInterval` asks
  *"what **is** `Γ_α`"*. **Two questions.**
- `sigmaCount` asks *"`Σ` over this domain"*; the integral asks *"`∫f dx`"*.
  **Two questions.**

Neither is §15.3 failing. **Both are §15.3 detecting that one name is covering
two operations** — and in both cases the resolution is §18.3's: name both, put
each where it belongs. §20.2 does it for `alphaCut`; sending the integral to
`fuzzy-defuzz` (§10 — *"centroid… `∫x·f(x)dx / ∫f(x)dx`"*) does it for
`sigmaCount`.

> **Amended by §22.1 (2026-07-16):** the *split* stands — `Σ` and `∫` are two
> questions — but the *placement* was wrong. In every defuzzifier the integral
> appears only in a **ratio**, and the grid spacing `h` cancels exactly
> (verified numerically), so `fuzzy-defuzz` has no use for a standalone
> `∫f dx` and none ships. The second question turned out to have no consumer.

**Decided — §15.3, narrowed by one qualifier:**

> **Closed forms are overrides on the function, where the operation means the
> same thing.** Where the return type cannot express the closed form, that is not
> a barrier — **it is the signal that you have two operations, not one operation
> at two fidelities.** Name both (§18.3).

One qualifier, not two, and it now *predicts* §18.3 rather than sitting beside
it. **That the same pattern has now surfaced three times independently — `core`
vs `maximalGradeSet` (§18.3), `alphaCut` vs `alphaCutInterval` (§20.2),
`sigmaCount` vs the integral — is the sign it is structural rather than a series
of coincidences.** A return type that cannot hold the exact answer is a
name-collision detector.

#### 20.7 The agreement law must be **one-directional** — and 2a already said so

**Found by building §20.1(a)'s table, and it changes shipped code.**

`MembershipFnLaws` asserts `fn.height(over) == generic.height(over)` — the
override agrees with the fold. Give it a `TriangularNumber(-0.5, 0.5, 1.5)` over
a grid of `{0.0, 1.0}` — a peak that is *off-grid*:

    generic fold  height(over) = 0.5
    override      height(over) = 1.0     ← the true Sup, and correct
    the law asserts equality             → FAILS, while the override is right

So the law that exists to make §15.3 true would reject §15.3's own worked example.

**The discriminator was already written into 2a's KDocs**, which is the reason to
trust it rather than invent one:

| operation | its own KDoc says | verdict |
|---|---|---|
| `height` | *"Over a `Sampled` domain this is a **lower bound** on the true supremum"* | **one question, two fidelities** → override valid |
| `sigmaCount` | *"a grid sum, **not an integral**: it scales with the point count"* | **two questions** → §20.1(b), name both |

`height` was *always* documented as approximating a target it could not reach. An
override that reaches it answers the same question, better. `sigmaCount` was
always documented as being about the grid itself.

**Decided (proposed): where §15.3's override is valid, the law is not equality —
it is that the override may not be *worse* than the fold.**

> **`height`: `override ≥ fold`.** The fold *found* an `x` with `f(x) = 0.5`, so
> the true Sup is at least `0.5` — that finding is **absolute**. An override
> claiming `0.3` is lying and is caught. An override claiming `1.0` may simply
> know more.

**This is the same one-directional shape for the third time**, and at this point
it is plainly structural rather than coincidence:

- §16.4 — a **witness** refutes absolutely; the absence of one proves nothing.
- §19.7(2) — `Proven` ⟹ the generic search finds no witness; the *witness* is the
  absolute half.
- §20.7 — the fold's `height` is a **lower bound it achieved at a real point**;
  that half is absolute, the other is not.

In each, the sampled search's **positive findings are facts** and its **negative
findings are not**. A law may lean on the first and never on the second. That is
§7's ethic in one sentence, and it keeps arriving from different directions.

**Consequence for `MembershipFnLaws` (shipped):** its `height` and `sigmaCount`
checks must change — `height` to `≥`, `sigmaCount` to *not overridable* (§20.1(b),
so its check becomes vacuous and should be dropped rather than weakened). The
`support`/`core`/`maximalGradeSet`/`alphaCut` checks stay as equality **because
§20.1(b) forbids overriding them at all** — there, equality is exactly the right
assertion, and it is what stops someone trying.

**Tighten it with `isExhaustive` — the tightening is the half that catches bugs.**

An earlier draft proposed keeping `≥` uniformly, on the grounds that one law sound
everywhere beats two each sound somewhere. That is wrong, and giving up the
tightening gives up the only part with teeth. Over an **`Enumerable`** the fold
visits *every* element, so the fold **is** the Sup over that domain — not a lower
bound on anything. Equality is sound there, and it is exactly what catches an
override ignoring a question-defining domain.

    override ≥ fold                        always
    override == fold   when over.isExhaustive

That is `Domain.isExhaustive`'s entire job (§16.4), doing it once more.

**And note what no law can catch**, which is why §20.8 matters more than this
section: over a **`Sampled`** window the fold is genuinely a lower bound, so
`override ≥ fold` passes for `TriangularNumber.height(Sampled(2, 3)) = 1.0` — an
answer that is simply wrong. **No law recovers that.** The override has to be
right. §20.7 fixes a symptom; §20.8 fixes the cause.

#### 20.8 Is the universe **fixed by the operation** or **supplied by the caller**?

**The rule §15.3 needed and did not have, and the reason §20.7 alone is not
enough.**

§20.7's law is sound and lets the real bug through:

    TriangularNumber(-0.5, 0.5, 1.5).height(Sampled(2, 3, 1024))
      the triangle is identically ZERO on [2,3] — its support is (-0.5, 1.5)
      fold      → 0.0     ← correct: the Sup over [2,3] IS 0
      override  → 1.0     ← "ignores the domain entirely", per §15.3
      §20.7:  1.0 ≥ 0.0   → PASSES

So the defect is upstream. **§15.3's example is wrong, and it inherited that from
§3's `Parametric`** — the very concept §15.3 was deleting.

**The disanalogy §15.3 missed.** It said "reuse `TNorm.residuum`'s pattern"
without noticing where that pattern's authority comes from: **`residuum`'s search
space is fixed; `height`'s is a parameter.** `TNorm.residuum` bisects `[0,1]` —
always, by the definition of a t-norm. Its override can ignore nothing, because
there is nothing to ignore. `height` searches whatever you hand it, and
`Sampled(-1,2)` and `Sampled(2,3)` are **different questions with different
answers**. An override that ignores the argument answers neither.

**The rule:**

> **Is the universe fixed by the operation, or supplied by the caller?**

| | universe | override |
|---|---|---|
| `findNonConvexity` | **ℝ — fixed.** `over` only supplies candidate endpoints `x₁, x₂` | valid; may ignore the domain |
| `findNonStrongConvexity` | ℝ — fixed | valid; may ignore the domain |
| `height` | **supplied** | **must read the domain** |
| `isNormal` | supplied (derives from `height`) | must read it |
| `sigmaCount`, `support`, `core`, `maximalGradeSet`, `alphaCut` | supplied | §20.1(b) — two questions; name both |

**`findNonConvexity → Proven` survives untouched.** Convexity is a property of `f`
on ℝ; the grid only proposes `x₁` and `x₂`. That is *why* `Proven` is honest there
and nowhere else in this group — §20.5's dividend is real, and now it has a reason
rather than a coincidence.

**The fix is not "do not override". It is "read the carrier, do not fold over
it."** `Domain` is **sealed** (§15.4), so an override can dispatch on it:

- **`Sampled(lo, hi)`** → the analytic Sup over `[lo, hi]`, in **O(1)**: `1.0` if
  `m ∈ [lo, hi]`; `0.0` if the window misses `(l, r)` entirely; otherwise `f` at
  the nearer endpoint, by unimodality.
- **`Enumerable`** → **fall back to the fold.** No analytic shortcut exists; there
  the universe *is* the element set, and folding it is not an approximation of
  anything.

**Verified — and the override still wins on both counts:**

    window            analytic   fold(1024 pts)
    [-1.0,  2.0]      1.000000     0.998534     ← analytic is MORE ACCURATE: 0.5 is not a grid point
    [ 2.0,  3.0]      0.000000     0.000000     ← the case that broke "ignores the domain"
    [ 1.0,  2.0]      0.500000     0.500000
    [-3.0, -2.0]      0.000000     0.000000

O(1) instead of O(n), and **exact where the grid only approximates** — `[-1, 2]`
is §15.3's promise delivered. It just reads `(lo, hi)` instead of pretending they
are not there.

**This is the fourth arrival at the same shape**, and it is worth saying plainly:
`core`/`maximalGradeSet` (§18.3), `alphaCut`/`alphaCutInterval` (§20.2),
`sigmaCount`/integral (§20.1), and now `height`-over-a-window vs `height`-over-ℝ.
Every one is a name or a signature covering **two questions**, and every one was
found by trying to implement it rather than by re-reading it.

#### 20.9 The `isExhaustive` guard belongs to **anything derived from `height`**

**Found by running §20.7's own law against §20.8's own type**, which is the third
time this slice that building the thing said something re-reading it did not.

§20.7 guards `height`: `override ≥ fold` always, `override == fold` when
`over.isExhaustive`. That is stated as a fact about `height`. It is not. It is a
fact about **every operation defined in terms of `height`**, and the library has
exactly one:

    maximalGradeSet(over) = over.filter { apply(x) >= height(over) }
                                                    ^^^^^^^^^^^^^^
                                                    the VIRTUAL height

So overriding `height` silently changes `maximalGradeSet` — **in a type that does
not override `maximalGradeSet` and is forbidden by §20.1(b) from trying.** An
override reached through a default body it never mentions.

**The disagreement it produces is correct, and §18.3 ratified it in advance:**

> *"Over a `Sampled` they can differ — the true supremum may be approached between
> grid points and attained nowhere on the grid, which is [Sampled]'s standing
> caveat, not a new one."*

`T(-1, 0.5, 2)` over 512 points has an analytic `height` of `1.0` that **no grid
point attains**, so its `maximalGradeSet` is legitimately **empty** while the
fold's is `{0.499…}`. Both right; different questions; §18.3 said so.

**Decided: `maximalGradeSet`'s agreement law takes `height`'s guard.** The line is
mechanical, and worth stating because it predicts rather than describes:

| member | body | equality sound? |
|---|---|---|
| `support`, `core`, `alphaCut`, `strongAlphaCut` | filters on `f` **alone** | **yes, over any domain** — a `height` override cannot reach them |
| `maximalGradeSet` | filters on `f` **against `height(over)`** | **only when `isExhaustive`** |
| `isNormal` | `height(over) == 1` | derived; no standalone law |

**The rule:** *a law's soundness is inherited through the default bodies it calls,
not declared at the member it names.* §20.7 guarded the member it was looking at
and missed the one downstream of it — which is the same shape as §15.3 guarding
`residuum`'s pattern and missing that `height`'s universe was a parameter (§20.8).
Both times, the defect was one call-edge away from where the thinking stopped.

#### 20.10 The degenerate member of a family is where clause order stops being cosmetic

Two shipped bugs, same shape, caught by §20.9's normality law within minutes of it
existing. Recorded because the *class* is predictable and the instances were not.

    TriangularNumber.crisp(1.0)          = T(1,1,1)     f(1.0) = 0.0     ← wrong
    TrapezoidalNumber.crispInterval(-1,1) = Tz(-1,-1,1,1) f(-1.0) = 0.0  ← wrong

Both were written **feet-first**:

    x <= l || x >= r -> 0.0        // fires first
    x == m -> 1.0                  // unreachable when l == m == r

For a proper triangle every clause is fine. For `T(m, m, m)` **the feet are the
peak**, `m <= m` is true, and the degenerate member of the family evaluates to
**zero everywhere including at its own peak**. Fixed by testing the peak/plateau
first, which is correct for both the general and the degenerate case.

**Why this is worth a section rather than a commit message:** `crisp` and
`crispInterval` are the *first* constructors a reader reaches for — "does this
library do the obvious thing when nothing is fuzzy?" — and both answered no. The
general case was right and the boundary of the family was not, which is §14.6's
lesson arriving at ordinary control flow rather than at arithmetic: **the
reasoning was sound about the shape and wrong about its degenerate limit, and only
running it said so.**

#### 20.2 `Interval`, and what a fuzzy number actually is

§20.1(b) needs an answer, and the module needs the same thing for a different
reason, so one type serves both.

**A fuzzy number is a fuzzy set whose α-cuts are closed bounded intervals** —
i.e. normal + convex, with bounded `Γ_α` for `α ∈ (0,1]`. That is not a
convenience; it is the definition, and it is *why* α-cut arithmetic works.

**Proposed hierarchy:**

    DoubleMembershipFn                      (fuzzy-set, §9 — the primitive path)
      └── FuzzyNumber
            alphaCutInterval(α ∈ (0,1]): Interval    ← the defining operation, exact, no Domain
            + §20.1(a)'s overrides
            ├── TriangularNumber(l, m, r)
            ├── TrapezoidalNumber(a, b, c, d)
            ├── GaussianNumber(mean, sigma)
            └── AlphaCutNumber(cuts)                 ← what arithmetic returns (§20.3)

`Interval` needs an **order** on X, which `Domain<X>` does not supply — the same
shape of constraint as §15.5's vector space, and the same resolution: it lives on
the ℝ tier. `alphaCutInterval` is restricted to `α ∈ (0,1]`, matching eq. (24)'s
own restriction, and for a reason: **`Γ_0` is all of ℝ for every fuzzy set**,
since every degree is `≥ 0`. Zadeh's `(0,1]` is not fastidiousness.

This does **not** change 2a's API. `alphaCut(over, α): List<Double>` stays as the
grid sample it always was; `alphaCutInterval(α)` is new, exact, and needs no
domain. Two names, two questions — §18.3's move, and §20.1's rule saying in
advance that it would be needed.

##### (i) `AlphaCutNumber` carries a **function**, not a collection

`AlphaCutNumber(cuts: (Double) -> Interval)`. **α is continuous**: there is no map
over uncountably many levels, and a `Map<Double, Interval>` or `List<AlphaLevel>`
would quietly re-introduce the sampling §20.3 exists to avoid. §20.3's phrase
"α-cut map" reads as *function*; the constructor signature is where that gets
silently wrong, so it is stated here.

(Contrast `MembershipFn.decompose`, which *does* return a `List<AlphaLevel>` —
correctly, because it is explicitly a **sample** at named levels, and §18.2
already records that its round trip is exact only at the levels cut.)

##### (ii) Nestedness is an unverifiable precondition — §19.6, relocated

`AlphaCutNumber` is a fuzzy number **only if its cuts are nested**
(`α₁ < α₂ ⟹ Γ_{α₂} ⊆ Γ_{α₁}`), and §20.3's `f(x) = sup{α | x ∈ Γ_α}` bisection
**requires that monotonicity** — hand it non-nested cuts and it returns a
plausible number computed from a meaningless search.

It cannot be checked at construction: uncountably many α, so only sampled.

**But it is not §19.6's case, and the difference decides the API.** Boundedness
was unsamplable in *both* directions. Nestedness is refutable in one: a witness
`(α₁ < α₂, x ∈ Γ_{α₂} \ Γ_{α₁})` **disproves it absolutely** (§16.4 — refutation
is exact even where proof is not). That makes it convexity-shaped, not
boundedness-shaped, and convexity-shaped things get a `Verdict` and a law suite.

**Nguyen's theorem** — which §20.6 correctly marks not-on-hand — is exactly what
says arithmetic *preserves* nestedness, so **anything `×`, `+` or `−` produces is
safe by construction**. The exposure is confined to hand-built instances.

**Decided (proposed): the constructor is public, the precondition is documented
and unchecked, and `fuzzy-laws` samples it.** The precedent is exact and already
shipped — `TConorms.dualOf`:

> *"Involutivity of `negation` is required and **not checked** here — it is a
> property of a function, not of a value, so it cannot be checked at
> construction, only sampled. That is exactly what `DeMorganLaws` is for."*

Same sentence, different property. §4's "construction validates" governs
*values* (a `γ`, a `λ`); a function's behaviour over an uncountable domain was
never in its scope.

**Rejected: internal-only, constructible solely by arithmetic.** Safe, and it
costs more than it looks. `AlphaCutNumber`'s actual service is *"give me α-cuts
and I will derive your membership function"* — a user implementing `FuzzyNumber`
by hand must supply **both** `applyAsDouble` and `alphaCutInterval`, and keep them
consistent. That is the very duplication this type removes. Closing it would
protect users from a precondition by denying them the tool that has it.

##### (iii) The agreement law **will** fail at the boundary — predicted, not discovered

`alphaCutInterval(α)` for a Gaussian is `m ± σ√(2 ln(1/α))`. Evaluating `f` at
that endpoint round-trips through `sqrt`/`ln`/`exp` and lands at `α ± ε`. So a
grid point sitting *exactly* on the boundary is inside the interval and outside
the α-cut, and the law fails while **both sides are correct**.

**Verified, and it is not even systematic:**

    α = 0.9    endpoint 0.45904360502642089    f = 0.89999999999999991    f ≥ α ?  FALSE
    α = 0.5    ...                             f = 0.50000000000000000    f ≥ α ?  TRUE
    α = 0.25   ...                             f = 0.25000000000000006    f ≥ α ?  TRUE
    α = 0.01   endpoint 3.03485425877029291    f = 0.01000000000000000    f ≥ α ?  FALSE

This is **§14.6(a) exactly**: an exactness claim over-applied to an operation that
is *arithmetic* rather than *selection*. `alphaCut`'s comparison `f(x) ≥ α` is
selection and is exact; `alphaCutInterval`'s endpoint is computed and is not.

**Decided (proposed):** state the law on grid points **strictly interior** to the
interval, and give the boundary itself a `Tolerance` from `fuzzy-laws` (§8 — the
one place tolerances live). Not `EXACT`: the endpoint is arithmetic.

Predicted here rather than met as a bug, because the same mistake has now been
made three times (§14.6, §19.7's Gaussian, this) and the pattern is legible:
**wherever an exact comparison meets a computed value, the exactness belongs to
the comparison and not to the value.**

#### 20.3 `×` — ship the exact answer, not the triangle

**Verified, not assumed.** `T(1,2,3) × T(1,2,3)`: the α-cut of `T(1,2,3)` is
`[1+α, 3−α]`, so the product's is `[(1+α)², (3−α)²]` — **quadratic in α**, where
a triangle's is linear. It is not a triangle.

    α       exact product      the triangle T(1,4,9)     error
    0.0    [1.000, 9.000]        [1.000, 9.000]          0.000
    0.25   [1.562, 7.562]        [1.750, 7.750]          0.188
    0.5    [2.250, 6.250]        [2.500, 6.500]          0.250
    0.75   [3.062, 5.062]        [3.250, 5.250]          0.188
    1.0    [4.000, 4.000]        [4.000, 4.000]          0.000

**Note where the error is: nowhere you would look.** The approximation is exact
at `α = 0` and `α = 1` — the support and the peak, the two things anyone spot-
checks — and wrong by 0.25 through the entire interior. That is why every
mainstream library ships it and why nobody notices.

**The decisive fact: the exact product is still a fuzzy number.** Its α-cuts are
nested (verified over 101 levels), so it is exactly representable *by its α-cut
map*, and its membership function follows from eq. (24) by the same near-tautology
§18.2 recorded: `f(x) = sup{α | x ∈ Γ_α}`, recoverable by bisection on α since the
cuts are monotone.

So the choice is not "approximate or refuse". It is "approximate or be right".

**Recommendation: option 2.** `times` returns a `FuzzyNumber` — an
`AlphaCutNumber` carrying the exact α-cut map. It is exact, it **composes** (the
result is a `FuzzyNumber` and can be multiplied again, which a triangle
approximation cannot do without compounding its error), and it is §7's thesis
applied to the one place the whole field looks away. §19.3 is the precedent: we
shipped the separation *theorem* and said what its preconditions were, rather
than shipping eq. (31)'s uncomputable definition or a plausible lie.

Cost: evaluating `f(x)` on a product costs a bisection over α (~50 evaluations of
the cut map) instead of an arithmetic expression. Real, and the honest trade.

**And here the exact answer is not merely representable — it is writable.**
Inverting both branches of `Γ_α = [(1+α)², (3−α)²]` (`α ≤ √y − 1` and
`α ≤ 3 − √y`):

    μ(y) = min(√y − 1, 3 − √y)      on [1, 9]

**Verified: it round-trips the exact α-cut at all 101 sampled levels.**
`μ(4) = 1`, `μ(1) = μ(9) = 0`, `μ(2.25) = 0.5` — against the triangle's
`0.4167` at the same point, and the triangle's own `μ(2.5) = 0.5`. Not a
triangle; still a fuzzy number; and closed-form.

Symbolic inversion in general is out of scope, so `AlphaCutNumber` stays the
representation. But this settles that it **represents something real** rather
than papering over a hole — the bisection is recovering a function that exists,
not manufacturing one.

**Rejected — (1) a "loudly typed" approximating triangle.** Better than silence,
but it still answers the wrong question, and a type that says
`ApproximateTriangle` invites exactly the reasoning §7 exists to prevent: *"it is
labelled, so it must be fine."* If an approximation is wanted it should be an
explicit, named, lossy projection **of** the exact answer — `approximateAsTriangle()`
on the result — not the thing `×` hands you by default.

**Rejected — (3) no `×`.** Fuzzy arithmetic without multiplication is not fuzzy
arithmetic, and the exact answer is available.

#### 20.4 `X ⊖ X` is not crisp zero — and a reader will meet it

**Verified.** For `X = T(1,2,3)`:

    α = 0.0   X_α = [1.00, 3.00]   X_α − X_α = [−2.00, +2.00]
    α = 0.5   X_α = [1.50, 2.50]   X_α − X_α = [−1.00, +1.00]
    α = 1.0   X_α = [2.00, 2.00]   X_α − X_α = [ 0.00,  0.00]

`X ⊖ X` is a fuzzy number spread symmetrically about zero, width `2(r−l)` at the
support. Only at `α = 1` is it `{0}`. α-cut interval arithmetic **has no
cancellation** — the same trap ordinary interval arithmetic has, for the same
reason: the two `X`s are treated as independent quantities that merely happen to
share a range.

This is not a bug and it is the first thing a user will report as one.
**Attributed/derived, per §18.2** — Zadeh 1965 has no fuzzy arithmetic — and it
goes in `FuzzyNumber.minus`'s KDoc, where a reader meets it, as well as here.

#### 20.5 The dividend: `Proven` from a closed-form proof

§19.1 gave `Verdict` three values so that an **exhaustive enumeration** could
report `Proven` where a grid could only report `NotRefuted`. §15.3 gave functions
the right to override analysis with closed forms. Neither knew about the other.

A `TriangularNumber` is convex **by construction** — its α-cuts are intervals by
similar triangles, no search required. So it can override `findNonConvexity` and
return `Verdict.Proven` **honestly**, from an analytic proof, over *any* domain
including a `Sampled` one — the case §15.6 exists to forbid guessing about.

`Proven` turns out to mean *"a proof exists"*, not *"the domain was exhaustive"*.
Exhaustive enumeration was only ever **one way** to have one. That the type
already said the right thing, for a reason nobody had in mind, is the kind of
evidence §12 was after: two decisions made for unrelated reasons paying off
together.

**Caveat, and the fix is not the obvious one.** `ConvexityLaws` currently asserts
*"a sampled ∀ is never `Proven`"* (§19.7). A `TriangularNumber` over a `Sampled`
domain fails it today, so the law must change — but **not** to *"never `Proven`
unless the subject supplied a proof"*. **That is unverifiable.** A suite cannot
see whether an override has a proof; it sees a `Proven` come back. Such a law
could only be *believed*, which is precisely what §7 exists not to do.

**Decided (proposed): the checkable law is the cross-check §19.7(2) already
ships.**

> **`Proven` ⟹ the generic sampled search finds no witness.**

A witness is absolute (§16.4). So if a `TriangularNumber` claims `Proven` and a
grid produces a counterexample, the override is lying — and *that* the suite
catches, without ever asking how the claim was reached.

**This is slice 1's pattern, exactly.** `ResiduumLaws` never asks how a residuum
was computed; it asserts the adjunction holds whatever produced it — closed form,
bisection, or luck. **Do not test the mechanism; test that the closed form agrees
with the fold it replaces.** That is also what `MembershipFnLaws` does, and what
§15.3 needs in order to be true rather than asserted.

§19.7's original claim survives intact where it belongs: as a **test of the
default**, run against a black-box lambda that overrides nothing. That is a
test-of-the-test, not a law — the same category as `StandardLaws` failing for
Product.

#### 20.6 Provenance: this module is `Attributed:` almost throughout

Checked before writing, per §17.1's rule. **Bergmann 2008 contains zero
occurrences of** "fuzzy number", "LR representation", "α-cut", "Dubois", "Prade",
"interval arithmetic", or "extension principle". §17.1 is about the *shapes* of
membership functions in a linguistic argument — Black's curves, Goguen's
`1/(1+x)` — not fuzzy numbers as an arithmetic type. **Zadeh 1965 has no fuzzy
arithmetic at all.**

So, honestly:
- **`Source:`** — only where it reduces to what we have read: `Γ_α` is eq. **(24)**
  (§V p.347); `f(x) = sup{α | x ∈ Γ_α}` is derivable from it (§18.2); convexity is
  eq. **(25)**; Zadeh's `(0,1]` restriction on α is his.
- **`Attributed:`** — the fuzzy-number concept, LR representation (Dubois & Prade
  1978, **not on hand**), α-cut interval arithmetic, and the theorem that makes it
  valid for continuous monotone operations (Nguyen 1978, **not on hand**).

§17.5's rule stands: an unconsultable source cannot arbitrate, so none is cited as
though it could. Nothing here is claimed as verified that is not.

---

## Updated: 2026-07-15 — Slice 2b design

### 19. Zadeh §V — **ALL RATIFIED 2026-07-15**

§15.5 and §15.6 designed 2b before 2a existed. Read back against the built seam
and against §18.1's verified §V index, three things need deciding. §15.5's
*central* finding — that §V needs a vector space, not a domain — is verbatim
correct (p.347: *"we assume for concreteness that X is a real Euclidean space
Eⁿ"*) and is not in question.

#### 19.1 §15.6's mechanism is superseded by §16.4

§15.6 decided `findNonConvexity(over:): Counterexample?`, and its reasoning is
right: convexity quantifies over uncountably many `x` **and** `λ`, so sampled it
can only report *"no counterexample found"*, and *"returning `true` asserts a
proof we did not perform."*

But §16.4 later settled that exact question for containment and gave it a
better answer: a nullable witness **conflates `Proven` with `NotRefuted`**. Over
an `Enumerable` a ∀ is a proof; a `Counterexample?` cannot say so, and the caller
must go and consult `isExhaustive`. §15.6 predates that, so its *mechanism* is
stale by the same argument that §16.4 already won.

**Proposed:** `findNonConvexity` returns a `Verdict`, exactly as containment
does.

**The wrinkle:** convexity's witness is a **triple** `(x₁, x₂, λ)`, not a point,
while `Verdict<X>` ties the witness type to the domain's element type via
`Verdict.of(witness: X?, domain: Domain<X>)`. `noWitness` reads nothing from the
domain but `isExhaustive`, so the fix is small and additive:

    Verdict.of(witness: W?, exhaustive: Boolean): Verdict<W>       // new
    Verdict.noWitness(exhaustive: Boolean): Verdict<W>             // new
    // existing Domain overloads stay, as the sugar they already are

Then `findNonConvexity(over: Sampled): Verdict<ConvexityWitness>`, where the
witness carries `x₁`, `x₂`, `λ` and the two degrees that break eq. (25) — which
is what a reader needs to re-derive the failure by hand.

#### 19.2 Shadow is **domain-generic** — §15.5 appears to have mis-classified it

§15.5 lists `shadow` under "vector-space-bound". Reading p.350, it is not:

> *"For notational simplicity, the notion of the shadow (projection) of A on a
> hyperplane H will be defined below **for the special case where H is a
> coordinate hyperplane**, e.g., `H = {x | x₁ = 0}`. Specifically, the shadow of
> A on H is defined to be a fuzzy set `S_H(A)` in `E^{n−1}` with
> `f_{S_H(A)}(x₂, …, xₙ) = Sup_{x₁} f_A(x₁, …, xₙ)`."*

**Zadeh only ever defines the coordinate case, and it is a `Sup` over one
coordinate — no scalar multiplication, no addition on X.** That is exactly
`Product<A, B>` plus a fold, both of which 2a already ships:

    fun <A, B> shadow(a: MembershipFn<Pair<A, B>>, over: Domain<A>): MembershipFn<B>

He even flags it: *"this definition is consistent with (23)"* — eq. (23) being
the mapping-induced set, i.e. the extension principle. The shadow **is** the
extension principle along a projection.

What *does* need the vector space is the shadow's **theorems** — *"If A is a
convex fuzzy set, then its shadow on any hyperplane is also a convex fuzzy set"*,
and `S_H(A) = S_H(B) for all H ⇒ A = B` (which ranges over *arbitrary* H, not
coordinate ones).

This is precisely the distinction §15.5 already drew for separation — *"the
computation is domain-generic; only its meaning needs the vector space"* — and
did not apply to its neighbour three lines up.

**Proposed:** ship `shadow` domain-generically. It makes §15.4's claim that
`Product` "earns itself twice" come true a slice early, and it is the same
mechanism `fuzzy-relation` will want for eq. (23).

**Against:** it lands `shadow` in a module whose remit is domain-generic Zadeh,
while its *theorems* live in 2b — so the operation and its properties separate.
That is already true of `height` (a query in 2a, whose §V theorems are 2b), so
it is not a new seam, but it is worth saying out loud rather than discovering.

#### 19.3 Separation ships a **theorem**, not eq. (31)'s definition

Sharper than §15.5 implies, and it changes what the KDoc must say.

The **definition** (p.351, eqs. **31**/**32**) is `D = 1 − M̃` where
`M̃ = Inf_H M_H`, an infimum over **every hypersurface H in Eⁿ**. That is not
computable, and not approximable by sampling.

What is computable is the **theorem** (p.352):

> *"Let A and B be **bounded convex** fuzzy sets in Eⁿ, with maximal grades M_A
> and M_B … Let M be the maximal grade for the intersection A ∩ B
> (`M = Sup_x Min[f_A(x), f_B(x)]`). Then **D = 1 − M**."*

So `1 − height(intersection(a, b))` is **not the definition of D** — it is a
theorem's right-hand side, and it is equal to `D` **only when both sets are
bounded and convex**. Ship it as the definition and it silently returns a number
for non-convex sets that is not their degree of separation.

**Proposed:** `separationDegree(a, b, over)` computes `1 − M`, and its KDoc says
plainly that this is p.352's theorem, names its preconditions, and points at
`findNonConvexity`/`findUnboundedness` to check them. Preconditions unchecked at
call time, per §4's split — the check is a fold, and the caller may already know.

**Alternative considered:** return `Verdict`-like "cannot vouch for this unless
convex". Rejected — the number is well-defined arithmetic; it is its *meaning*
that has a precondition, and §4 says operations do not validate.

#### 19.5 Strict convexity is **vacuous in ℝ¹** — **RATIFIED: dropped**

§15.5 puts strict convexity in 2b's ℝ¹ tier. In ℝ¹ it is not a distinct property
at all.

Zadeh, p.349: *"A is **strictly convex** if the sets `Γ_α`, `0 < α ≤ 1` are
strictly convex (that is, if the midpoint of any two distinct points in `Γ_α`
lies in the **interior** of `Γ_α`)."*

**In ℝ¹, every convex set is strictly convex.** A convex `Γ_α ⊆ ℝ` is an
interval. Take distinct `x₁ < x₂` in `[a, b]`; the midpoint `m` satisfies
`a ≤ x₁ < m < x₂ ≤ b`, so `a < m < b` — the interior. Degenerate and empty cuts
are vacuous. So `strictly convex ⟺ convex`, and `findNonStrictConvexity` would
return exactly what `findNonConvexity` returns.

It is a genuine `ℝ^{n≥2}` notion: in ℝ² the square `[0,1]²` is convex and **not**
strictly convex — the midpoint of `(0,0)` and `(1,0)` is `(0.5, 0)`, on the
boundary. A disc is strictly convex. The distinction needs a dimension to
work in.

**Proposed: drop it from 2b**, and record why. Zadeh's caveat — *"strong
convexity does not imply strict convexity or vice-versa"* — is a statement about
Eⁿ, and it stops being informative at n=1. §15.5's ℝ¹ decision is not wrong;
strict convexity is simply not something ℝ¹ can show you.

#### 19.6 Boundedness is **unsamplable** — **RATIFIED: dropped**

Worse than vacuous: not checkable in either direction.

Zadeh, p.348: *"A fuzzy set A is **bounded** if and only if the sets
`Γ_α = {x | f_A(x) ≥ α}` are bounded for all `α > 0`; that is, for every `α > 0`
there exists a finite `R(α)` such that `‖x‖ ≤ R(α)` for all x in `Γ_α`."*

A `Sampled` domain is a grid on a **bounded window** `[L, U]`. So:

- **Cannot prove it.** `Γ_α ∩ [L, U]` is bounded because `[L, U]` is; that says
  nothing about `f_A` at `x = 10⁶`.
- **Cannot disprove it either** — and this is the part that makes it different
  from convexity. A convexity *witness* is a genuine refutation (§16.4:
  refutation is exact even where proof is not). An unboundedness witness would
  have to be an unbounded `Γ_α`, which no bounded window can exhibit.

The best a window can report is *"the α-cut touches the edge, so the set may
extend"* — evidence, and not even one-directional evidence, since `f_A` may drop
to zero immediately outside.

So boundedness is the one §V property where **[Verdict] has nothing to say**:
not `Proven`, not `Refuted`, and `NotRefuted` would overstate what was looked at.

**Proposed: drop `findUnboundedness` from 2b**, and document boundedness as a
**precondition** of §19.3's separation theorem that this library cannot check —
which is honest, and which is what §19.3's KDoc already has to say about
convexity anyway.

**Alternative:** ship `alphaCutTouchesWindow(over, α): Boolean` under a name that
claims only what it detects. Rejected unless asked for: it is a heuristic wearing
a law suite's clothes, and §7 exists to keep those apart.

#### 19.7 Most of §V's theorems are **not soundly checkable** — **RATIFIED**

§19.4 promised "the §V theorems as `fuzzy-laws` suites". Building them, three of
the four cannot be shipped as laws without lying about what they establish.

The problem is structural, not incidental. **Every §V theorem is conditional on
convexity, and over a `Sampled` grid convexity is never `Proven`** — only
`NotRefuted` (§19.1). So the antecedent is never established, and a failure of the
consequent does not indict anything:

- **p.347, *"If A and B are convex, so is their intersection"*.** Suppose the
  grid refutes `A ∩ B`. By the contrapositive, `A` or `B` is genuinely non-convex
  — and the grid simply missed it. That is a **sampling gap, not a bug**, and a
  suite reporting it as a violation would be blaming the code for the sampler.

  **Now demonstrated rather than argued.** `min(gaussian, bimodal)` is genuinely
  non-convex — it dips near `x ≈ 0.9`, where the Gaussian is still falling and the
  bimodal set's valley has not recovered. A **5-point grid reports `NotRefuted`;
  17 points report `Refuted`**. Same set, only the grid changed. A law reading
  `NotRefuted` as "convex" would have believed the coarse grid, which says the
  opposite of the truth. `fuzzy-laws`' own tests pin both readings.
- **p.349, *"If A is a convex fuzzy set, then its core is a convex set"*.**
  Same shape, same objection.
- **p.350's corollary**, *"If X = E¹ and A is strongly convex, then the point at
  which M is essentially attained is unique"* — **worse**: it fails on a grid for
  genuinely strongly convex sets. A triangle peaked *between* two grid points has
  two grid points tied for the maximal grade, so `maximalGradeSet` has two
  elements and the corollary "fails" while the set is impeccably strongly convex.
  The culprit is "essentially attained" — §18.3 already dropped that adverb as
  topological, and this is where the amputation shows.
- **p.352's separation theorem**, `D = 1 − M` — is `separationDegree`'s
  *implementation* (§19.3). Checking it would be circular. Eq. (31)'s actual
  definition, `Inf` over every hypersurface, is not computable.

**What IS soundly checkable, and ships:**

1. **strong convexity ⟹ convexity.** Any convexity refutation has
   `f[at] < Min[…]`, which is also a strong-convexity refutation; and `x₁ = x₂`
   cannot refute convexity, since there `at = x₁` and `f(x₁) ≥ f(x₁)`. So a
   `Refuted` from `findNonConvexity` **forces** one from
   `findNonStrongConvexity`.

   **That second clause is a theorem about ℝ, and the code had to be made to
   honour it.** Written as the textbook does — `λx₁ + (1 − λ)x₂` — it is *false*
   in IEEE 754: at `x₁ = x₂ = 3.0, λ = 0.2` it yields `3.0000000000000004`, so
   `f(at) < f(x₁)` and convexity is refuted at a point where eq. (25) is
   trivially true. A **convex Gaussian was refuted this way**, and this law is
   what caught it. `findNonConvexity` now interpolates `x₂ + λ(x₁ − x₂)` —
   algebraically identical, exact when the endpoints coincide — and
   `ConvexityWitness.at` matches it, or the witness would not reproduce.

   Recorded because the pattern is by now familiar: the reasoning was sound
   about the reals and the arithmetic was not, and only running it said so.
2. **The §15.3 override guard.** An analytic `findNonConvexity` override must not
   claim convexity where the generic search **found a witness** — a witness is
   absolute (§16.4), so this direction is sound. The converse is not: an override
   refuting where the generic search does not may simply be better informed.
3. **Witness self-consistency.** A returned `ConvexityWitness` must actually
   witness: `atSegment < minEndpoints`, and re-evaluating `f` at `at` must
   reproduce `atSegment`.

**Proposed:** `ConvexityLaws` ships (1)–(3). The four theorems go to `fuzzy-laws`'
**own tests**, against fixtures whose convexity we control *by construction* (a
triangle, a Gaussian) rather than by sampling — where the premise is known rather
than guessed, so the theorem is a real check.

The line is §7's: **a suite must not report as a violation something that is
merely a sampling gap.** That would be a heuristic in a law suite's clothes, and
it is the same objection that killed `alphaCutTouchesWindow` in §19.6.

#### 19.4 Scope for 2b, after §19.1–§19.6

Domain-generic (`Domain`, any X):
- `FuzzySets.shadow` — §19.2, **ratified**.

ℝ¹-bound (`DoubleMembershipFn` over a `Sampled` interval), per §15.5:
- `findNonConvexity` — eq. **(25)**, p.347 → `Verdict<ConvexityWitness>` (§19.1).
- `findNonStrongConvexity` — p.349, same shape with strict `>`.
- `separationDegree` — §19.3, p.352's **theorem**, preconditions in KDoc.

`fuzzy-laws` suites for the §V theorems:
- *"If A and B are convex, so is their intersection"* — p.347.
- *"If A is a convex fuzzy set, then its core is a convex set"* — p.349, where
  "core" is Zadeh's `C(A)`, i.e. our `maximalGradeSet` (§18.3).
- p.350's corollary: *"If X = E¹ and A is strongly convex, then the point at
  which M is essentially attained is unique."* — the one theorem in §V that is
  **stated for E¹ specifically**, so 2b is exactly where it can be checked.
- p.352's separation theorem: `D = 1 − M` for bounded convex sets.

**Dropped, with reasons recorded:** strict convexity (§19.5, vacuous in ℝ¹);
boundedness (§19.6, unsamplable).

**Out:** `VectorSpace<X>` (§15.5, an explicit non-goal); ℝⁿ convexity; the
`S_H(A) = S_H(B) ⇒ A = B` theorem (ranges over arbitrary H).

---

## Updated: 2026-07-15 — Zadeh 1965 read directly

### 18. The paper, read end to end — **ALL RATIFIED 2026-07-15**

All 16 pages now read. §17 indexed §II–§IV; this indexes §V and audits 2a's
scope against the whole paper. The headline: **over half of the brief's 2a
analysis list is not in Zadeh 1965**, and two of the entries that *are* in it do
not mean what their modern names mean.

#### 18.1 §V's index, and what §15.5 got right — **RATIFIED**

**(24)** `Γ_α = {x | f_A(x) ≥ α}`, p.347 — **the α-cut**, introduced for
`α ∈ (0,1]` inside the convexity definition · **(25)** convexity
`f_A[λx₁+(1−λ)x₂] ≥ Min[f_A(x₁), f_A(x₂)]`, p.347 · **(26)**–**(30)** the
intersection proof · boundedness p.348 · **`M = Sup_x f_A(x)`, "the maximal
grade", p.348** · strict/strong convexity p.349 · **`C(A)`, "the core", p.349**
· shadow p.350 *(no eq. number)* · **(31)** `D = 1 − M̃` · **(32)**
`M̃ = Inf_H M_H`.

§15.5 is vindicated verbatim: p.347 does say *"we assume for concreteness that X
is a real Euclidean space Eⁿ"*, convexity **is** eq. (25), and p.350's corollary
**is** *"If X = E¹ and A is strongly convex…"*. §15.5 was written from the paper.

#### 18.2 What is *not* in the paper — **RATIFIED**

Read end to end (Received: November 30, 1964; references: Birkhoff 1948, Halmos
1960, Kleene 1952). The paper contains **no**: `height` (it is "maximal grade") ·
`support` · strong α-cut as a named definition · decomposition/representation
theorem · σ-count · normality · bounded difference · concentration/dilation/
intensification.

**The rule that has settled out**, consistent with §17.3 (bounded difference) and
§17.4 (dilation vs intensification):

> **Ship what a source on hand verifies, or what is *derivable* from one. Name it
> honestly — `Source:` for the mathematics, `Attributed:` for a name we have not
> checked. Drop anything whose *mathematics* needs a source we lack.**

Intensification was dropped because its **formula** was unverifiable. σ-count is
not: `Σ f(x)` is unambiguous arithmetic, and only the *name* is attribution.
Those are different failures and get different answers.

Applied to 2a's analysis list:

| operation | ships | authority |
|---|---|---|
| `height` | ✔ | **`Source:`** §V p.348 — Zadeh's `M = Sup_x f_A(x)`, "maximal grade". **`Attributed:`** the name "height" |
| `alphaCut` | ✔ | **`Source:`** §V eq. (24), p.347 — `Γ_α`, verbatim |
| `strongAlphaCut` | ✔ | **derived** from (24) with `>` for `≥`. The paper itself uses `{x | f_A(x) > M}` ad hoc at p.353. Name attributed |
| `support` | ✔ | **derived**: `strongAlphaCut(0)`. Zadeh p.342: *"it is not meaningful to speak of a point x 'belonging' to a fuzzy set A except in the trivial sense of `f_A(x)` being positive"* — the notion, not the name |
| `decompose` | ✔ | **derived from (24)**, near-tautologously: `sup{α | x ∈ Γ_α} = sup{α | f_A(x) ≥ α} = f_A(x)`. Needs no source beyond (24); name attributed |
| `sigmaCount` | ✔ | **`Attributed:`** — the arithmetic `Σ f(x)` is certain, the name and the notion of fuzzy cardinality are not ours to date |
| `checkContainment` | ✔ | **`Source:`** §II eq. (2), p.340 |
| `checkEquality` | ✔ | **`Source:`** §II p.340 *(no eq. number)* |
| `checkEmptiness` | ✔ | **`Source:`** §II p.340 *(no eq. number)* |

#### 18.3 `core` — Zadeh's is not the modern one — **RATIFIED: ship both**

**Decided (option 3):** both sets ship, each under a name that does not lie.

| ships | is | authority |
|---|---|---|
| `core(over)` | `{x \| f(x) = 1}` | **`Attributed:`** later nomenclature. Explicitly **not** Zadeh's `C(A)`, and says so |
| `maximalGradeSet(over)` | `{x \| f(x) = height(over)}` | **`Source:`** §V p.349 — Zadeh's `C(A)`, de-topologised (see below). "Maximal grade" is his own term, p.348 |
| `isNormal(over)` | `height(over) == 1` | **`Attributed:`** — the concept that reconciles the two. Not in the paper; derives from `height` |

The two coincide **exactly when `isNormal`**, and that is the whole content of
the distinction — so the library ships the predicate that names it rather than
leaving a reader to discover it.

Neither name is a lie, which is what (1) and (2) each could not manage alone:
(1) would have silently redefined Zadeh's word, (2) would have surprised every
reader who expects `{f = 1}`. Shipping both costs one function and one
predicate, and turns a conflict into a distinction the API teaches.

**On "essentially attained".** Zadeh's `C(A)` is the set where `M` is
*essentially* attained — a topological notion resting on p.348's
ε-neighbourhood Lemma, which needs Eⁿ. `maximalGradeSet` drops "essentially"
and takes the set where `M` **is** attained, which is domain-generic and is what
`§15.5` can support in 2a. Over an `Enumerable` the two agree (a finite set
attains its supremum). Over a `Sampled` they can differ — the true supremum may
be approached between grid points and attained nowhere on the grid, which is
[Sampled]'s standing caveat, not a new one. Zadeh's full `C(A)`, with the
topology, is 2b's business if anything ever wants it.

---

*Original analysis, retained — the reasoning is why we ship two.*

The one genuine conflict, and it is a semantic choice rather than an attribution.

**Zadeh, p.349:** *"let `C(A)` be the set of all points in X at which **M** is
essentially attained. This set will be referred to as the **core** of A."* — with
`M = Sup_x f_A(x)`. So Zadeh's core is **where the maximal grade is reached**.

**Modern usage:** `core(A) = {x | f_A(x) = 1}`.

They coincide **iff the set is normal** (`M = 1`) and diverge for every
subnormal set — where Zadeh's core is non-empty and the modern one is empty.

Worse for §15.5's split: Zadeh's *"essentially attained"* is topological (the
p.348 Lemma's ε-neighbourhoods), so `C(A)` as he defines it needs Eⁿ and belongs
to **2b**, while §15.5 lists `core` as domain-generic **2a**.

**Options:**
1. **`core(over)` = `{x | f = 1}`**, documented as later nomenclature and
   explicitly *not* Zadeh's `C(A)`, which is named as 2b's business. No claim to
   implement the source, so §2 is not violated — the same move as `dilation`.
2. **`core(over)` = `{x | f = height(over)}`** — Zadeh's set, de-topologised
   (dropping "essentially", which is the part needing Eⁿ). Faithful to p.349's
   *intent*, domain-generic, and surprising to every reader who expects `{f = 1}`.
3. **Ship both**: `core` (modern, `{f = 1}`) and e.g. `maximalGradeSet` (Zadeh's,
   de-topologised). Most honest, one more name.

**Recommendation: (1).** `core` is a word with a settled modern meaning, and
quietly giving it Zadeh's 1965 meaning would mislead far more people than it
would please. §2 governs claims about sources, and (1) makes no such claim. If
`fuzzy-number` later wants Zadeh's `C(A)`, (3) is a superset and nothing blocks
it.

Worth noting either way: **normality** is the concept that reconciles them, and
it is not in the paper either. It falls out of what we have — `height(over) == 1`
— so it can ship `Attributed:` if wanted.

### 17. The equation index, verified — **RATIFIED**

A copy of the paper is now on hand (`prod/Zadeh-1965-Fuzzy-Sets.pdf`, a scan —
16 pages, pp.338–353, no text layer, so it is read visually). Everything below
is from the paper itself, not from memory. §2 says the sources are the spec;
until now the *index* to those sources was folklore, and one entry was wrong.

#### 17.1 The map, and the citation bug it caught — **RATIFIED**

**§II — Definitions** (pp.339–341). Definitions live here, **not** §III:

| | | |
|---|---|---|
| — | `f_A(x)`, grade of membership | p.339 |
| — | *empty*: `f_A ≡ 0` on X | p.340, **no eq. number** |
| — | *equal*: `f_A(x) = f_B(x) ∀x` | p.340, **no eq. number** |
| **(1)** | complement `f_A' = 1 − f_A` | p.340 |
| **(2)** | containment `A ⊂ B ⟺ f_A ≤ f_B` | p.340 |
| **(3)** | union `f_C(x) = Max[f_A(x), f_B(x)]` | p.340 |
| **(4)** | union, abbreviated `f_C = f_A ∨ f_B` | p.340 |
| **(5)** | intersection `f_C(x) = Min[f_A(x), f_B(x)]` | p.341 |
| **(6)** | intersection, abbreviated `f_C = f_A ∧ f_B` | p.341 |

**§III — Some Properties of ∪, ∩ and Complementation** (pp.342–343):
**(7)**/**(8)** De Morgan · **(9)**/**(10)** distributive laws · **(11)**,
**(12)** their membership-function identities · **(13)** the sieve example ·
*"fuzzy sets in X constitute a distributive lattice with a 0 and 1"* p.343.

**§IV — Algebraic Operations** (pp.344–346): **(14)** algebraic product
`f_AB = f_A f_B` · **(15)** `AB ⊂ A ∩ B` · **(16)** algebraic sum `f_{A+B} =
f_A + f_B`, *"provided the sum ≤ 1"* · absolute difference `f_{|A−B|} = |f_A −
f_B|` **(no eq. number)** · **(17)**/**(18)** convex combination ·
**(19)** `A ∩ B ⊂ (A,B;Λ) ⊂ A ∪ B` · **(20)** the Min/Max inequality ·
**(21)** recovering Λ · composition `f_{B∘A}(x,y) = Sup_v Min[f_A(x,v),
f_B(v,y)]` p.346 **(no eq. number)** · **(22)**/**(23)** sets induced by
mappings. **Footnote 4, p.344**: `A ⊕ B = (A'B')' = A + B − AB`.

**The bug.** Slice 1 cited union as "§III eq. (2)" and intersection as
"§III eq. (3)". Both are wrong twice — wrong section (they are §II
*definitions*, not §III *properties*) and wrong number. §III p.342 settles it in
Zadeh's own prose: *"With the operations of union, intersection, and
complementation defined as in **(3)**, **(5)**, and **(1)**…"*. §3's "eq. 2" for
containment was right all along; the two claims were in direct contradiction in
the shipped artifact and nobody had put them side by side.

Corrected in `fuzzy-algebra`, `fuzzy-laws` and in §2 above. Everything else
verified sound: De Morgan (7)/(8), distributivity (9)/(10), p.343's lattice
remark, §IV footnote 4, the §II complement citations, §3's `Sup_v Min` at p.346
and its extension principle at (23).

**The lesson worth keeping.** §2's *"where a source and our code disagree, the
source wins"* is only operable if someone reads the source. A citation nobody
checks is decoration, and a *confidently wrong* one is worse than none — it is
the single thing a reader will not re-derive. Citations are now verified against
the scan, and unverifiable ones are not written.

#### 17.2 Two consequences for 2a's API — **RATIFIED**

**Convex combination takes a fuzzy set, not a scalar.** Eq. (17) is
`(A, B; Λ) = ΛA + Λ'B` where **Λ is an arbitrary fuzzy set**; eq. (18) reads
`f_{(A,B;Λ)}(x) = f_Λ(x)f_A(x) + [1 − f_Λ(x)]f_B(x)`. The scalar λ appears only
in eq. (20)'s proof, and in the introductory sentence about combining *vectors*
— which is where a scalar reading comes from. So the signature is
`convexCombination(a, b, lambda: MembershipFn<X>)`, three sets. A scalar
overload is fine as sugar (Λ constant) but is not the definition.

Note this vindicates the brief's parenthesis: it is pointwise arithmetic on
degrees and needs nothing from §15.5's vector space.

**Two more laws fall out**, both free for `fuzzy-laws`: eq. **(15)**
`AB ⊂ A ∩ B` — the algebraic product is contained in the intersection — and eq.
**(19)** `A ∩ B ⊂ (A,B;Λ) ⊂ A ∪ B` for all Λ.

#### 17.4 Bergmann read too: the hedges are not the ones we assumed — **RATIFIED**

`prod/An Introduction to Many-Valued and Fuzzy Logic….pdf` is now on hand
(Bergmann 2008, CUP; a Calibre conversion **with** a text layer, so it is
greppable — unlike the Zadeh scan).

**§2's Bergmann index verified sound:** §11.2 Łukasiewicz · §11.7 "T-norms,
T-conorms, and implication in fuzzy logic" · §11.8 Gödel · §11.9 Product ·
§12.1 "More on MV-algebras" · §12.2 "Residuated lattices and BL-algebras" ·
§16.1 "Fuzzy qualifiers: hedges" · §17.1 "Defining membership functions". Every
section §2 names exists and says what §2 says it says.

**But §16.1 is not the hedge family we assumed.** In full, Bergmann's hedges are:

| | Bergmann §16.1 | |
|---|---|---|
| `very` | `I(very)(n) = n²` | *"the particular function that's usually used for `very` is the square function"* |
| `very very` | `n⁴` | iterated application |
| `close-to` | **`I(close-to)(n) = min(1, n + .1)`** | a **linear shift**, not `f^0.5` |
| `not` | `I(not)(n) = 1 − n` | negation as a qualifier |

The whole book contains **zero** occurrences of "dilation", "concentration" or
"square root", and no CON/DIL/INT nomenclature at all. §16.1's own sources are
**Zadeh 1975** and **Lakoff 1973** (n.1: *"The term was coined by the linguist
George Lakoff (1973)"*) — so even "hedge" as a word is now a checked citation.

**Decided — the hedge set collapses, and improves:**

| ships | is | on whose authority |
|---|---|---|
| `power(p)` | `f^p` | **`Source:`** Bergmann §16.1 — `n²` and `n⁴` both shown, hedges treated exactly as we need them: functions `[0,1] → [0,1]`, composed by iteration |
| `concentration()` | `power(2)` | **`Source:`** Bergmann §16.1 — this *is* his `very` |
| `dilation()` | `power(0.5)` | **`Source:`** for the mathematics — an instance of the same verified operation. **Only the name** is Zadeh's, and the KDoc says so |

Three functions, one mechanism, nothing unverified. Naming a function is not a
claim about mathematics, so `dilation` can carry a name we have not checked
provided the code does not pretend otherwise.

**Intensification: dropped.** `INT(f) = 2f²` below `0.5`, `1 − 2(1−f)²` above —
**piecewise, not a power**, so unlike `dilation` it cannot be recovered from
anything §16.1 covers. It genuinely needs a source we do not have, and shipping
it would be precisely what §2 forbids. The formula above is *probably* right,
which is exactly the confidence that produced the union/intersection bug and
three wrong Yager diagnoses.

**`close-to` (`min(1, n + 0.1)`): not shipped.** Bergmann has it and it is
verified, so it is available if a non-power hedge is ever wanted — but nothing
asks for one, and "verified" is not the same as "wanted".

#### 17.5 An unconsultable source cannot arbitrate — **RATIFIED**

Klement, Mesiar & Pap (2000) is not on hand. Slice 1 cited it for
`TNorms.DRASTIC`, `NILPOTENT_MINIMUM`, `hamacher`, `ordinalSum`,
`TConorms.DRASTIC`, `NILPOTENT_MAXIMUM`, `Negations.sugeno`, `Negations.yager`,
and the universal t-norm laws.

**The defect is not "those citations are unverified". It is that §2's rule —
"where a source and our code disagree, the source wins" — is an *arbitration*
rule, and an unconsultable source can't arbitrate.** Citing KMP as though it
could was a promise the project cannot keep. Marking it "unverified" would have
treated a broken promise as a documentation gap.

Two things shrink the exposure, and neither is an excuse:
1. **`fuzzy-laws` verifies the mathematics independently of who gets credit.**
   The risk here is *misattribution*, not incorrectness — a different failure,
   and a much smaller one.
2. **KMP is a *secondary* source for nearly all of it.** The primaries are more
   useful to a reader anyway.

**Decided:**
- **Re-anchor each construction to its primary**: Hamacher 1978, Sugeno 1977,
  Yager 1980, Fodor 1995, Mostert–Shields 1957. (§6 already cites Mostert–Shields
  by name rather than through KMP — the pattern was already there.)
- **Mark them `Attributed:`, not `Source:`** — §2's convention. The code stops
  rendering "checked" and "believed" identically.
- **Leave KMP in §2's bibliography as a general reference with no specific claim
  hanging on it**, which is all it honestly is.

Checked first, since a re-anchor onto a text on hand would have been better than
an attribution: **Bergmann covers none of it** — zero occurrences of Hamacher,
Sugeno, Yager, Fodor, nilpotent, ordinal sum, Mostert, Shields or drastic. So
`Attributed:` is the true state, not a convenience.

Reversible the moment KMP arrives: the markers are the knob, and each already
names the primary to check.

#### 17.3 Bounded difference is **not in Zadeh 1965** — **RATIFIED**

The 2a brief lists the pointwise algebra as *"Zadeh §II–IV: complement, union,
intersection, algebraic product, algebraic sum, **bounded difference**, absolute
difference, convex combination"*. §IV is pp.344–346 and has been read in full.
It contains no bounded difference. §IV's operations are exactly: algebraic
product, algebraic sum, absolute difference, convex combination, fuzzy relation,
composition, sets induced by mappings.

Bounded difference `f_{A⊖B} = Max[0, f_A − f_B]` is later vocabulary (Zadeh's
1970s work; standard in the t-norm literature). §2 forbids inventing a citation
for it.

It is also **already reachable, and is not a new mechanism**:

    A ⊖ B  =  T_Łukasiewicz(A, B')      since max(0, f_A + (1−f_B) − 1) = max(0, f_A − f_B)

which is §6's thesis exactly — one parameterised mechanism, of which Zadeh's
operations are instantiations.

**Options:** (a) ship it, cited to Klement/Mesiar/Pap 2000 and documented as the
Łukasiewicz t-norm of `A` and `B'`, noting it is *not* in the 1965 paper;
(b) drop it from 2a as out of scope for a module whose remit is "faithful Zadeh
1965 and nothing else" (§11a's phrasing);
(c) drop it and let users write `intersection(a, complement(b), Algebra.LUKASIEWICZ)`,
which is the same thing spelled in the vocabulary the library already has.

**Decided: (a)** — ship it, cited honestly.

**Amendment after §17.5.** The ratified citation was "Klement, Mesiar & Pap
2000", which is not on hand and therefore cannot be checked. It does not need to
be: the *mathematics* rests on sources we have read —

    A ⊖ B = T_Łukasiewicz(A, B')     Bergmann §11.2 (t-norm) + Zadeh eq. (1) (complement)

— and only the *name* "bounded difference" is attributed to the wider t-norm
literature. The KDoc says so. That keeps §17.1's rule intact: the mathematics is
verified, the nomenclature is attributed, and the two are not conflated.

---

## Updated: 2026-07-15 — Slice 2a, the seam under construction

### 16. Decisions §15 did not cover — **ALL RATIFIED 2026-07-15**

Surfaced building §15's seam. §15 survives contact with code essentially intact
— §15.1's "domain at the call site", §15.2's deletion of `Opaque` and §15.3's
demotion of `Parametric` to overrides all hold, and each removed a concept
rather than needing one back. These are the gaps.

#### 16.1 `DoubleMembershipFn.applyAsDouble` carries its own name

§9 requires that `DoubleMembershipFn` "avoid boxing the argument too". The
obvious spelling is to override `apply` at `Double`:

    fun interface DoubleMembershipFn : MembershipFn<Double> {
        override fun apply(x: Double): Double        // primitive? or boxed?
    }

Whether Kotlin emits `apply(D)D` plus a bridge, or `apply(Ljava/lang/Double;)D`,
decides whether §9's promise is kept — and it is the compiler's business, not
ours to assert.

**Decided:** the primitive entry point is a **separate method**,
`applyAsDouble(x: Double): Double`, which is not an override of anything generic
and is therefore `(D)D` outright, guaranteed. `apply` remains, defaulting to it,
so a `DoubleMembershipFn` is still a `MembershipFn<Double>` and the whole
domain-generic API accepts one with no adapter.

Rejected: a standalone type not extending `MembershipFn<Double>` (the JDK's
`DoubleUnaryOperator` shape). It would split the API in two and need `.boxed()`
adapters at every call site; Kotlin can express the bridge that Java cannot, so
there is no reason to inherit Java's limitation.

The name follows `java.util.function.DoubleUnaryOperator.applyAsDouble`, so it
reads as idiom from Java rather than invention. Cost: two names on one type.

#### 16.2 `Domain.elements()` — the seam needs an element source

§15.4 specifies `Product(Domain<A>, Domain<B>) : Domain<Pair<A,B>>`, and §15
describes the seam's job as folding and searching. Those are inconsistent: a
product cannot *form* a pair without asking a factor what is in it, and no
fold-over-degrees or search-by-predicate will tell it.

**Decided:** `Domain<X>.elements(): List<X>` joins the seam. It is also what the
element-valued operations (support, core, α-cuts) are filters of.

The hot path is untouched — `reduceDegrees` never calls it, and all three cases
override `firstWhere`/`filter` to avoid materialising. `Product.reduceDegrees`
materialises only the *outer* factor (`O(|A|)`), threading the accumulator
through the inner factor's own fold rather than building the product.

#### 16.3 An empty `Enumerable` is rejected at construction

`height` over an empty domain is a supremum over nothing: `−∞` mathematically,
`0.0` by the fold's `initial`. Both are lies, and neither is a degree.

**Decided:** `Enumerable.of()` throws on empty. Consistent with §4's split
(construction validates, operations do not) and with `Sampled`, which cannot be
empty by construction anyway. Duplicates, by contrast, are **kept** — σ-count
sums over what is enumerated, so silently deduplicating would change an answer
while hiding a caller's bug.

#### 16.4 A ∀ over X returns a three-valued `Verdict`, not a boolean — **RATIFIED**

§15.6 settles the ethic (a sampled ∀ reports a counterexample, because *"returning
`true` asserts a proof we did not perform"*) and not the mechanism. This is the
mechanism.

**Decided:** containment, equality and emptiness return

    sealed interface Verdict<X>
      Proven<X>                    // exhaustive domain, no witness exists
      Refuted<X>(val witness: X)   // witness found — absolute, from ANY domain
      NotRefuted<X>                // sampled domain, no witness found

plus, as **sugar where a boolean is provably honest**, a boolean overload taking
`Enumerable<X>` specifically.

**Why not the cheaper "overload on the argument type" alone** (the original
recommendation, reversed on review — the reasoning is the useful part):

*It forces you to discard something.* Four situations collapse to three answers:

| | witness found | no witness |
|---|---|---|
| `Enumerable` | Refuted | **Proven** |
| `Sampled` | Refuted | **NotRefuted** |

Note the left column. **A witness found on a grid disproves containment
absolutely** — sampling is lossy in one direction only. You cannot prove a ∀ by
checking 1024 points; a single counterexample settles it regardless. So:

- `findNonContainment(over: Domain<X>): X?` gets refutation right, but `null`
  conflates **Proven** with **NotRefuted** — precisely the distinction §15.6
  exists to draw — leaving the caller to reconstruct it from `isExhaustive`.
- `isContainedIn(over: Enumerable<X>): Boolean` gets proof right, but `false`
  **throws the witness away**, and the witness is the product (§7).

Every answer those two give is derivable from a `Verdict`; the reverse is false.

*And it repeats §15.1's mistake.* Exhaustiveness is **computed**, not declared —
`Product.isExhaustive` is a runtime conjunction of its factors. Encoding it in
the static type only works where it happens to be static, i.e. `Enumerable`, and
the case it fails on is `Product(Enumerable, Enumerable)` — the *motivating* case
for `Product` (§15.4: it "earns itself twice" in `fuzzy-relation`). §15.1's whole
argument is that capability belongs in a value you must supply, not a phantom
type. This is that lesson, applied to its own author.

**Costs, accepted:** a heavier API. Mitigated by §14.1's Java 17 floor — sealed
types and `instanceof` patterns are both final there, so
`if (v instanceof Refuted<String> r)` is idiomatic Java, not ceremony. One
`Verdict<X>` serves all three operations, since each is "∀ over X with an X
witness". The boolean overload on `Enumerable` recovers the ergonomics where they
are honest.

**Rejected:** counterexample-only (`X?`) — one method, but `null` means two
different things.

#### 16.5 Analysis operations are **members** of `MembershipFn`, not extensions

§15.1 sketches `fun <X> FuzzySet<X>.height(over: Domain<X>): Double` — extension
syntax. Taken literally that breaks two ratified rules, and the sketch loses:

- **§9**: *"Extension functions are sugar only, never core operations (they read
  as statics from Java)."*
- **§15.3**, decisively: closed forms are *"**overrides on the function**"* —
  *"a `TriangularNumber` overrides it with the analytic answer and ignores the
  domain entirely."* **An extension function cannot be overridden.** §15.3 is
  unimplementable with extensions.

**Decided:** every domain-generic analysis operation is a `MembershipFn` member
with a default body — exactly `TNorm.residuum`'s shape from slice 1, which is
what §15.3 says it should reuse. `fun interface` survives: only `apply` is
abstract, so `MembershipFn { x -> … }` still works and inherits the lot.

**Corollary — where the rest lives.** The line is *queries vs combinators*, and
it coincides exactly with *overridable vs not*:

- **queries** (height, support, core, α-cuts, σ-count, containment, equality,
  emptiness, decomposition) — ask something *about* a set; a parametric function
  may know the answer analytically → **members of `MembershipFn`**.
- **combinators** (complement, union, intersection, algebraic product/sum,
  bounded/absolute difference, convex combination, hedges) — build a *new* set
  from old ones; the result of combining parametric sets is not generally
  parametric, so there is nothing to override → **`FuzzySets`, `@JvmStatic`**.

Two homes, one principle. Keeps `MembershipFn` to the operations that have a
reason to be virtual.

#### 16.6 `fuzzy-laws` gains an edge to `fuzzy-set`, updating §10's graph

§10 gives `fuzzy-laws → fuzzy-algebra`. §15.7 requires *"`fuzzy-laws` gains suites
for 2a's own laws (De Morgan over sets, decomposition round-trip) in the same
slice"*, which needs `fuzzy-set` on its compile path.

**Decided:** the edge is added; §10's graph line for `fuzzy-laws` now reads
`→ fuzzy-algebra, fuzzy-set`. Still acyclic (`fuzzy-set → fuzzy-algebra` only),
and still §7's intent: the law suites are consumable artifacts that validate the
modules from outside, so they follow the modules they validate.

Rejected: putting set laws in `fuzzy-set`'s own test source set. It would make
them internal tests rather than a published artifact, which is exactly the
distinction §7 exists to draw.

---

## Updated: 2026-07-15 — Slice 2 design (§3 restructured)

### 15. The capability seam, corrected — **RATIFIED 2026-07-15**

**Ratified by construction.** §15 was held unratified pending contact with code —
the seam being the thing worth getting right. It has now been built and exercised
by 142 passing tests, and every load-bearing claim held:

- **§15.1** (domain at the call site) — the compile-time guarantee survives as a
  required argument, pointwise ops stayed trivially closed, and "the same set over
  two domains" is a real test rather than a type error.
- **§15.2** (`Opaque` deleted) — an opaque `X` is simply one with no `Domain`, and
  every `Sup` is unreachable. One fewer concept, guarantee intact.
- **§15.3** (closed forms are overrides) — reused `TNorm.residuum`'s shape exactly,
  and turned out to *need* `MembershipFnLaws` to guard it (§16.5, §7).
- **§15.4** (two cases + `Product`) — `Product` immediately forced `elements()`
  onto the seam (§16.2), which §15 had not anticipated.
- **§15.5** (Zadeh §V needs a vector space) — **verbatim correct against the
  paper**: p.347's Eⁿ assumption, eq. (25), p.350's E¹ corollary all check out.
  §15.5 was written from the source.
- **§15.6** (counterexamples, not booleans) — settled the ethic; §16.4 supplied the
  mechanism it did not.

What §15 did not cover became §16; what the paper corrected became §17 and §18.

§3 was written before any of it existed. Reading it back against slice 1's code,
three of its four `Domain` cases are wrong, and its central claim quietly
contradicts its own design. §15 supersedes §3 where they disagree; §3's
*motivation* (some operations need `Sup`, and asking for one you cannot compute
must be a compile error) stands unchanged and is met more cheaply here.

#### 15.1 The domain is a parameter of analysis, not part of a set

§3 says pointwise operations are *"representation-free. Work over any X,
lazily, given only a membership function. **No domain needed**."* That sentence
is the refutation of putting the domain on the set. If a fuzzy set does not need
a domain to *be* one, the domain is not part of its identity.

**Rejected** — domain in the set's type:

    class FuzzySet<X, D : Domain<X>>
    fun <X, D : Searchable<X>> FuzzySet<X, D>.height(): Double

**Decided** — domain at the call site that needs it:

    class FuzzySet<X>
    fun <X> FuzzySet<X>.height(over: Domain<X>): Double

The compile-time guarantee §3 demands is *stronger* this way, not weaker: you
cannot ask for a `Sup` without producing a `Domain<X>` to supply it. The type
system enforces it by the ordinary mechanism of a required argument.

What it buys beyond that:
- pointwise ops stay trivially closed — `union(A, B) : FuzzySet<X>`, with no
  domain algebra to invent for the result;
- one type parameter instead of two, everywhere, forever — and §9's Java
  signatures stay legible;
- **the same set, analysed over different domains**: sample coarsely, then
  finely, and watch the answers converge. §3's shape made that a type error.

#### 15.2 `Opaque` is not a type. It is the absence of a domain.

Falls out of §15.1. §3 listed `Opaque` ("pointwise only") as a `Domain` case,
which is a contradiction: it is the one case that *cannot* answer the only
question a `Domain` exists to answer.

**Decided:** delete it. Under §15.1 an opaque X is simply one you have no
`Domain` for, and every `Sup` operation is then unreachable — exactly §3's
requirement, with one fewer concept.

#### 15.3 `Parametric` was a category error. Closed forms are overrides. — **EXAMPLE SUPERSEDED by §20.8**

> **Read §20.8 before this section.** The *decision* stands and has been proven by
> `fuzzy-number`: closed forms are overrides on the function, not `Domain` cases.
> The **worked example below is wrong.** *"A `TriangularNumber` overrides it with
> the analytic answer and **ignores the domain entirely**"* — an override that
> ignores the domain answers the wrong question, because `height`'s universe is
> **supplied by the caller**, not fixed by the operation. `TriangularNumber(-0.5,
> 0.5, 1.5).height(Sampled(2, 3))` is `0.0`; the triangle is identically zero
> there. This section inherited "ignores the domain" from §3's `Parametric`, which
> is exactly the concept §15.3 was deleting. The correct rule, and the correct
> pattern — *read the carrier, do not fold over it* — is §20.8.

`Enumerable` and `Sampled` describe **the carrier**: what X is, and how to fold
over it. `Parametric` described **the membership function** — "this is a
triangle, so its Sup is analytic and I need not search at all." Those are not
the same kind of thing, and §3 conflated "what the domain is" with "how a Sup
is computed."

**Decided:** closed forms are **overrides on the function**, following the
pattern slice 1 already established. `TNorm.residuum` has a generic bisection
default that every named t-norm overrides with its closed form ("you only pay
for bisection on a t-norm you wrote yourself"). Identically: `height(over:)`
folds by default, and a `TriangularNumber` overrides it with the analytic
answer and ignores the domain entirely.

So the closed forms live on the function that knows them, not on the carrier
that does not — and slice 2 gains no new pattern, it reuses slice 1's.

#### 15.4 `Domain<X>` is generic. `Sampled` is one-dimensional.

§3's four cases collapse to **two**: `Enumerable<X>` (fold over elements) and
`Sampled` (fold over a grid; approximate), plus §15.3's overrides, plus
§15.2's "no domain".

`Sampled` is a grid over an interval of **ℝ**, not ℝⁿ. Grid-sampling is tractable
in one dimension and nowhere else: 1000 points/dim is 10⁶ evaluations in ℝ² and
10⁹ in ℝ³. That is a wall, and it is documented as one rather than papered over.

`Product(Domain<A>, Domain<B>) : Domain<Pair<A,B>>` covers X×Y. It earns itself
twice: `fuzzy-relation` (§10) needs exactly this for `Sup_v Min[...]`
(Zadeh p.346) and for the extension principle (eq. 23).

#### 15.5 Zadeh §V needs a **vector space**, not a domain

The finding that splits slice 2. Convexity is

    f_A[λx₁ + (1−λ)x₂] ≥ Min[f_A(x₁), f_A(x₂)]        (Zadeh eq. 25)

and `λx₁ + (1−λ)x₂` requires **scalar multiplication and addition on X**. A
`Domain<X>` can *search* X; it cannot form the line segment between two of its
points. Zadeh states the precondition himself (p.347): *"we assume for
concreteness that X is a real Euclidean space Eⁿ."* §V is therefore not
domain-generic — it needs strictly more structure than the rest of §3.

This cleanly divides the module:

- **domain-generic, any X** — height, support, core, α-cuts, strong α-cuts,
  decomposition, cardinality, containment, equality, emptiness.
- **vector-space-bound** — convexity, strict/strong convexity, boundedness,
  shadow, separation degree `D = 1 − M`.

**Decided:** the second group is **ℝ¹ only** in slice 2b (`DoubleMembershipFn`
over a `Sampled` interval), which is where Zadeh's own Figures 1/4/5 and his
p.350 corollary (*"If X = E¹ and A is strongly convex..."*) live. A
`VectorSpace<X>` capability is a **non-goal** until something asks for it;
raising that ceiling later is easy, and nothing in §10's graph needs ℝⁿ.

Note the separation degree's *computation* (`M = Sup_x Min[f_A, f_B]`,
`D = 1 − M`) is domain-generic; only its *meaning* — that a separating
hyperplane exists — needs the vector space. Ship it where the meaning holds.

#### 15.6 `findNonConvexity()`, not `isConvex()` — **MECHANISM SUPERSEDED by §19.1**

> **Read §19.1 before this section.** The *reasoning* below stands and was
> never in doubt — a sampled ∀ cannot return `true` without asserting a proof
> nobody performed. The **mechanism** is stale: `Counterexample?` conflates
> `Proven` with `NotRefuted`, which §16.4 had already settled better for
> containment. The shipped shape is `Verdict<ConvexityWitness>`. Do not build
> a nullable witness from this section.

Convexity quantifies over all `α ∈ (0,1]` **and** all `x` — both uncountable.
Sampled, it can only ever report *"no counterexample found"*. Returning `true`
asserts a proof we did not perform.

**Decided:** `findNonConvexity(over:): Counterexample?`. `null` means no
witness was found, which is what we actually know. This is §7's ethic — the
reason `fuzzy-laws` reports counterexamples rather than booleans — applied to
`fuzzy-set`. The same treatment applies to any sampled ∀ claim: containment and
equality included.

**Superseded (§19.1).** `null` is one value doing two jobs: over an `Enumerable`
the ∀ *is* a proof, and a nullable witness cannot say so — it forces the caller
off to consult `isExhaustive` and reassemble the answer. The three-valued
`Verdict` says it directly. The last sentence above is the part that aged best:
"the same treatment applies to any sampled ∀ claim" is exactly what §16.4 did,
and §19.1 is that decision arriving back here.

#### 15.7 Slice 2 splits: 2a and 2b

§12 sized slice 1 at two modules. `fuzzy-set` as §10 scopes it is substantially
larger. §15.5 supplies a natural seam, so:

- **2a** — `MembershipFn`/`DoubleMembershipFn`, the pointwise algebra (Zadeh
  §II–IV), hedges, `Domain` (`Enumerable`, `Sampled`, `Product`), height,
  support, core, α-cuts, decomposition, cardinality, containment/equality/
  emptiness. Domain-generic throughout.
- **2b** — Zadeh §V: convexity, boundedness, shadow, separation. ℝ¹-bound per
  §15.5.

§12's thesis still applies: `fuzzy-laws` gains suites for 2a's own
laws (De Morgan over sets, decomposition round-trip) in the same slice.

---

## Updated: 2026-07-15 — Slice 1 scaffolding

### 14. Decisions surfaced while scaffolding — **ALL RATIFIED 2026-07-15**

These are choices the founding decisions did not cover, which building slice 1
forced. Per §13 ("if a real decision surfaces that CLAUDE.md doesn't cover, add
it there and ask before coding around it") they are recorded here rather than
buried in a build file. **Each is implemented as a single-line knob**, so
ratifying or reversing any of them is a one-line change and not a refactor.

§14.1–§14.6 are all **ratified** (2026-07-15).

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

#### 14.2 Compile on 2.4.20-Beta1, publish stdlib 2.4.10 — **RATIFIED 2026-07-15**

**Decided:** the compiler and the published stdlib are separate decisions with
separate knobs. `kotlin = "2.4.20-Beta1"` and `kotlin-stdlib = "2.4.10"` in
`gradle/libs.versions.toml`; `kotlin.stdlib.default.dependency=false` in
`gradle.properties`; `api(libs.kotlin.stdlib)` declared by hand in
`fuzzy.kotlin-conventions`.

This is §14.1's split applied to a second axis: **build on the newest thing,
hand consumers the oldest thing that costs us nothing.** The toolchain/target
pair and the compiler/stdlib pair are the same shape of decision.

**The problem it solves.** Left at its default, KGP injects `kotlin-stdlib` at
the *compiler's* version. The published POM would then carry
`kotlin-stdlib:2.4.20-Beta1` at `compile` scope, and Gradle's highest-version
conflict rule would drag that beta into consumers' graphs **even if they had
pinned a stable stdlib themselves**. A beta compiler is our business; a beta in
someone else's dependency graph is not.

Reads "2.4.10 as absolute bottom" as being about the *published contract*.

**Rejected:** (a) leave it — simplest, but exports our beta; (b) 2.4.10
everywhere — stable but gives up the compiler for no gain, since the compiler
was never the thing leaking.

**Known consequence, accepted:** KGP warns that the stdlib (2.4.10) is older
than the compiler (2.4.20-Beta1). That warning is the intended state. The real
risk it points at — a beta compiler emitting references to stdlib members absent
from 2.4.10 — is theoretical for code that uses `min`/`max`/`pow`/`abs` and
`List`, and would fail loudly at our compile time rather than silently in a
consumer's runtime. Revisit when 2.4.20 goes stable, at which point both knobs
move to it and the gap closes.

**Fallout worth recording:** turning the injection off forced
`fuzzy.kotlin-conventions` to apply `java-library`, because the `api`
configuration comes from that plugin and `kotlin("jvm")` applies only plain
`java`. That surfaced a latent bug — `fuzzy-laws`' existing
`api(project(":fuzzy-algebra"))` would have failed on the first build with
"Could not find method api()". `java-library` is correct here independently: it
is what makes the api/implementation split real in the published POM.

#### 14.3 Publishing via vanniktech 0.37.0, not plain `maven-publish` — **RATIFIED 2026-07-15**

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

**Repo URL — RESOLVED 2026-07-15.** The POM `url`/`scm` guessed
`github.com/eusrbin/fuzzy-math`, inferred from the groupId rather than from the
account hosting the source. The repo is **`github.com/MikeShorter/fuzzy-math`**
(alongside `vedic-research`); `developer` is id `MikeShorter`, name
"Michael E Shorter". Corrected before the first push.

Note the groupId stays `dk.eusrbin`: a Maven coordinate derived from a domain
we own, unrelated to where the source is hosted. Only the URLs were wrong.
Central surfaces `pom.url`/`pom.scm` on every artifact page, and published
versions are immutable — a wrong URL there is wrong in public, permanently, for
that version.

#### 14.4 `fuzzy-laws`' published API is dependency-free and hand-samples — **RATIFIED 2026-07-15**

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

#### 14.5 `TConormLaws` — a seventh suite, beyond §7's six — **RATIFIED 2026-07-15**

§7's tier table names six suites. A seventh is implemented: `TConormLaws`.

Reason: §9 makes `TConorm` a **separate type** from `TNorm` precisely so the two
cannot be mixed up. That decision means a consumer who writes their own conorm
cannot check it — `TNormLaws.verify` will not accept it, and no other suite
covers it. A published extension mechanism (§7) with a type you can implement
but cannot verify is incomplete. The same reasoning that justifies the type
justifies the suite.

Trivial to drop if unwanted; nothing depends on it.

**Ratified.** This closes a genuine inconsistency *between* two founding
sections: §9 created the type, §7's table forgot to give it laws. §7 exists to
stop a published extension point shipping without its correctness criteria —
and §7's own table did exactly that. Keep the suite; §7's six-tier table is the
thing that was wrong.

#### 14.6 Numerical findings from the first test run — **RATIFIED 2026-07-15**

First `./gradlew build`: compiled clean, 72/78 tests passed, and all six failures
were real. §7's required test-of-the-test passed — `StandardLaws` does fail for
Product. Three of the six were bugs in `fuzzy-laws` itself, which is the artifact
working as intended: it caught its own author.

**(a) `1 − x` is not exactly involutive, so tolerance calibrates per OPERATION,
not per algebra.** The important one.

§8 says min/max are exactly associative and idempotent, so "the Zadeh tier is
safe with exact equality". **§8 is correct as written** — it is a claim about
min/max. The bug was in over-applying it: `Tolerance.forAlgebra(STANDARD)`
returned `EXACT`, and `DeMorganLaws`/`MVAlgebraLaws` then inherited it for laws
that go through the *negation*. But `1 − x` is arithmetic, not lattice selection:

    a = 1/3   →   1 − (1 − a) = 0.33333333333333326   (Δ = 5.55e-17)

So Zadeh's own complement, correctly implemented, failed its own suite.
`StandardLaws` at `EXACT` remains correct and still passes, because idempotence,
distributivity and absorption touch nothing but min/max.

**Decided:** `forTNorm`/`forAlgebra` calibrate the monoid side; new
`forNegation` calibrates the negation (never `EXACT`); suites spanning both
combine with `looserOf`. An expression is no more exact than its sloppiest term.

**(b) Subnormals are out of the sampling pool.** `Double.MIN_VALUE` is replaced
by `ulp(1)` ≈ 2.2e-16 as the "nearly zero" probe.

In the subnormal range IEEE 754 multiplication is not strictly monotone —
`4.9e-324 × 0.5` underflows to exactly `0.0` — so the *floating-point* Product
t-norm is not the Product t-norm, and residuation fails by a gap of `0.5` that
no tolerance can close. True of `double`; useless about anybody's t-norm. A user
checking their own work would be told their arithmetic breaks at `1e-324`, which
is not a membership degree and is not their bug.

Not swept away: the underflow is documented on `TNorm.residuum` and pinned by
its own test, which also records that bisection lands on exactly `0.5` there by
round-half-to-even.

**(c) `Negations.yager` is NOT involutive in `double` for `w ≠ 1`, anywhere.**
The claim that a Yager De Morgan triple verifies has been **withdrawn**, not
scoped. Recorded at length because it took three wrong diagnoses to get here,
and the wrong turns are the instructive part.

Mathematically `N_w(N_w(a)) = a` always. In IEEE 754:

```
|N_w(N_w(a)) − a|  ≈  5.55e-17 · a^(1−w)
```

For `w > 1` that is **unbounded as `a → 0`**. Measured at `w = 4.25`: `5.6e-17`
at `a = 0.5`, `2.1e-12` at `a = 0.032`, **`7.6e-8` at `a = 0.001`**. The cause is
representation, not arithmetic: `N_w(a) ≈ 1 − a^w/w` sits near 1, where a double
resolves only to `ulp(1) = 2.2e-16`, so once `a^w` nears that, `a` is gone. No
implementation recovers it.

**Three attempts, three wrong assumptions:**
1. *"It's catastrophic cancellation in `1 − a^w`."* Real, and fixed —
   `Negations.yager` now evaluates `(−expm1(w·ln a))^(1/w)`, `ln a` via
   `ln1p(a − 1)` above 0.5. Removes a ~0.4% error. **Kept**: it removes the
   error that is ours and leaves the one IEEE 754's. But it moved Δ only
   5.3196e-8 → 5.2995e-8, i.e. it was not the problem.
2. *"It's a boundary cliff, so sample the interior."* Added
   `Sampling.interior()`. **Wrong and reverted** — it is a gradient with no
   floor, so interior sampling relocates the failure rather than scoping it. It
   passed at `w = 2.48` and failed at `w = 4.25`, which is luck, not a scope.
   The API is **removed**: public API added to stop a suite reporting something
   true is suppression with a nicer name.
3. *"Some tolerance must work."* None does; the error is unbounded. §8's
   tolerances absorb rounding noise, not bad conditioning.

**Decided:** Yager stays (§6 asks for it, it is a legitimate negation from the
literature) and is documented as numerically unverifiable in a De Morgan triple.
`DeMorganLaws` failing for one is **correct** — De Morgan over a dual triple
reduces to involutivity, so the suite is reporting the negation, not the law —
and that failure is asserted as a fact alongside the Drastic and Product
test-of-the-test. `Negations.sugeno` is the involutive parametric negation to
reach for: its curve meets the boundary linearly, so nothing collapses into the
last few ulps, and its triples verify at every edge case.

**The general lesson, worth more than the case.** Across (a), (b) and (c) the
first instinct was always that a red suite meant a broken suite. Every time, the
arithmetic was telling the truth and an *assumption* was wrong: §8's exactness
over-applied to the negation; "subnormals are just small numbers"; "the
implementation must be fixable"; "there must be a safe interior". `fuzzy-laws`
caught its own author four times in two runs — which is precisely what §7 built
it to do, and a reason to trust it that no amount of documentation could buy.

**Corollary for §7's test-of-the-test discipline:** asserting that a suite
*fails* where it should is not a curiosity confined to `StandardLaws`/Product.
It is the mechanism that stops each of these findings from being quietly
re-broken.

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

**That is an *arbitration* rule, and it has a precondition: the source must be
consultable.** An unreachable source cannot arbitrate anything, so citing one as
though it could is a promise the project cannot keep. The code therefore
distinguishes two epistemic states, and **must not render them identically**:

| KDoc marker | Means |
|---|---|
| `Source:` | **Read and checked** against a text on hand. `"Zadeh 1965, §II eq. (3)"` is a claim anyone can falsify, and someone has. |
| `Attributed:` | **A belief about who is owed credit.** Standard in the literature, not verified here, and named so that a reader knows which kind of claim they are looking at. |

"This is Zadeh eq. (3)" and "this is the Hamacher family" are different kinds of
statement. The first is checkable and checked; the second is attribution. §17.1
records what happened when they were rendered the same way: a citation nobody
could check turned out to be wrong in the one place it *could* be, and had sat
in a shipped artifact contradicting this file.

Texts on hand (`prod/`), and therefore able to arbitrate: **Zadeh 1965** (scan)
and **Bergmann 2008**.

- **Zadeh (1965), "Fuzzy Sets"**, Information and Control 8:338–353 —
  §II definitions (including union/intersection/complement), §III their
  properties, §IV algebraic operations, §V convexity/boundedness/shadow/
  separation. The core of `fuzzy-set`. **Equation index verified against the
  paper 2026-07-15 — see §17.1 for the map and for what it corrected.**
- **Bergmann (2008), *An Introduction to Many-Valued and Fuzzy Logic*** (CUP) —
  §11.7 t-norms/conorms/**implication**, §11.2/11.8/11.9 the Łukasiewicz /
  Gödel / Product systems, §12 MV-algebras, residuated lattices, BL-algebras,
  §16.1 hedges, §17 membership functions. The core of `fuzzy-algebra` and the
  law tiers. (Skip its derivation-system chapters — we are not building a
  theorem prover.)
- **Klement, Mesiar & Pap (2000), *Triangular Norms*** — a general reference,
  **not on hand, and nothing specific hangs on it** (§17.5). It is a *secondary*
  source for nearly everything we were citing it for; the primaries are more
  useful to a reader anyway, and each construction now names its own:
  **Hamacher 1978**, **Sugeno 1977**, **Yager 1980**, **Fodor 1995** (nilpotent
  minimum), **Mostert–Shields 1957** (ordinal sums — which §6 already cites by
  name rather than through KMP). All of those are `Attributed:`, none is
  `Source:`, and the mathematics is verified independently by `fuzzy-laws`
  regardless of who is owed the credit. Misattribution is the exposure; being
  wrong is not.

Note Zadeh's own footnote 3 (p.339): interpreting f_A(x) as truth values makes
fuzzy sets a many-valued logic on [0,1]. Set operations and logical connectives
are the same functions in different hats. This is *why* `fuzzy-algebra` is a
standalone module: it is the connective layer, useful with no set theory at all.

### 3. The capability seam (central design fact) — **MECHANISM SUPERSEDED by §15**

> **Read §15 before this section.** §3's *motivation* is the founding insight of
> the library and stands untouched: some operations need a `Sup` over X, you
> cannot take one over an uncountable X given only a black-box function, and
> asking for one you cannot compute must be a **compile error**. §15 meets that
> requirement more cheaply.
>
> **The mechanism below is wrong in three of its four cases**, and each was
> deleted by building it:
>
> - **`Opaque` is not a `Domain`** (§15.2). It is the one case that cannot answer
>   the only question a `Domain` exists to answer. An opaque X is simply one you
>   have no `Domain` for — and then every `Sup` is unreachable, which is exactly
>   §3's requirement with one fewer concept.
> - **`Parametric` is a category error** (§15.3). `Enumerable` and `Sampled`
>   describe the **carrier**; `Parametric` described the **membership function**.
>   Closed forms are overrides on the function — and, per §20.8, ones that *read*
>   the carrier rather than ignoring it.
> - **The domain is not part of a set's type** (§15.1). It is a parameter of the
>   operations that need it.
>
> The shipped seam is `Enumerable` | `Sampled` | `Product`, supplied at the call
> site. **Do not build `Opaque` or `Parametric` from this section.**

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
| Universal (dual) | any t-conorm | the same, dualised — see §14.5 |
| De Morgan | dual (T,S,N) triple | eqs. 7, 8 |
| Residuated / BL | left-continuous T + residuum | residuation adjunction, divisibility, prelinearity |
| MV | Łukasiewicz | MV-algebra axioms |
| Zadeh/Gödel only | min/max | idempotence, **distributivity** (eqs. 9, 10), absorption |

**Decision: publish the law suites as a consumable artifact — `fuzzy-laws`.**
Not internal tests. A user writing their own t-norm adds `fuzzy-laws` in test
scope and calls `TNormLaws.verify(myTNorm)`. This operationalises "the sources
are the spec", makes the library trustworthy in a way docs cannot, and ships
the extension mechanism *with its correctness criteria attached*. `StandardLaws`
must **fail** for Product — provably, as a test of the test.

> **Corrected — see §14.5.** The tier table above originally omitted the
> t-conorm row, which left `TConorm` (a separate type by §9) implementable but
> unverifiable — a published extension point with no correctness criteria,
> i.e. precisely the failure this section exists to prevent, committed by this
> section. `TConormLaws` closes it.
>
> Also: the suite is named `StandardLaws`, per §6's ratified naming. Earlier
> drafts of this section called it `ZadehLaws`; the code is right and the name
> here was stale.

### 8. Floating-point reality (tolerances live in `fuzzy-laws`)

The algebraic laws do **not** hold exactly in IEEE 754:

- min/max are exactly associative and idempotent → the Zadeh tier is safe with
  exact equality.
- Product t-norm associativity `(a·b)·c` vs `a·(b·c)` is **not** exact.
- Łukasiewicz suffers cancellation.

**Decision:** law tiers acquire a numerical dimension. Tolerances are
calibrated in one place inside `fuzzy-laws`, never scattered through test
files. (Kazakov's alternative — discretising [0,1] to a fixed `Resolution` — is
noted and **rejected**: it trades the whole continuum, and the
parametric/analytic path, for exactness we can get with tolerances.)

> **Superseded in part — see §14.6(a).** This section originally said
> tolerances calibrate **per algebra**. They calibrate **per operation**.
>
> The bullets above are correct as written: they are claims about min/max, and
> min/max *are* exactly associative and idempotent. The error was over-applying
> them to a whole *algebra*. `Standard` is min/max **and `1 − x`** — and
> `1 − x` is arithmetic, not lattice selection, so it is not exact
> (`a = 1/3 → 1 − (1 − a) = 0.33333333333333326`). Calibrating `EXACT` for the
> algebra made Zadeh's own complement fail its own suite.
>
> So: `forTNorm`/`forAlgebra` calibrate the monoid side, `forNegation` the
> negation (never `EXACT`), and suites spanning both combine with `looserOf`.
> An expression is no more exact than its sloppiest term. `StandardLaws` stays
> at `EXACT` and still passes — idempotence, distributivity and absorption
> touch nothing but min/max.

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
                         → fuzzy-algebra, fuzzy-set, fuzzy-number, fuzzy-relation
                           (test-scope consumable)
                           [fuzzy-set edge added in slice 2a — §16.6; fuzzy-number
                            in slice 3 — §20; fuzzy-relation in slice 4 — §21.8.
                            The suites follow the modules they
                            validate; still acyclic.
                            Note the shape: fuzzy-laws accretes an edge to every
                            module it publishes laws for, so it will end up
                            depending on most of the graph. That is the price of
                            §7's decision that the laws are a CONSUMABLE artifact
                            rather than internal tests. It is paid in test scope
                            only. Recorded here rather than rediscovered at module
                            nine.]

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
                           [Built in slice 4 — §21, which corrects this entry
                            three ways: Zadeh's relations are in X×X, "X×Y" is
                            later convention (§21.1); "similarity" is dropped,
                            the bundle being the attributed thing (§21.7); "CRI"
                            was a control-layer name for the relational image,
                            which ships as imageUnderRelation (§21.6).]

    fuzzy-number         FuzzyNumber, Interval, triangular / trapezoidal /
                         Gaussian / AlphaCutNumber, exact α-cut arithmetic
                         → fuzzy-set
                           [LR representation not shipped — §20.6: Dubois & Prade
                            1978 is not on hand, and the concept needs it.]

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
                           [Built in slice 6 as an UNPUBLISHED conformance
                            harness instead — §23.4: the experiment found zero
                            adapters needed, so the published sugar module
                            deleted itself (§15.2). The arrow to BOM is gone:
                            wrong kind of arrow, and deps.edn cannot consume a
                            BOM at all (§23.5). Real dependencies: test-scope
                            on every published module.]

    fuzzy-bom            version alignment for consumers
                           [Built in slice 6 — §23.5: constraints on the six
                            published modules. Serves Maven and Gradle
                            consumers only; tools.deps has no BOM import, so
                            deps.edn consumers pin per-artifact versions.]

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
