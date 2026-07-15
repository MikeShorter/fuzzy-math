# fuzzy-math — Decision Record

Convention: decisions live here and are ratified before code. Newest-first.
This file is the spine of the project; if code and this file disagree, this
file is wrong and should be fixed deliberately, not silently.

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

#### 15.3 `Parametric` was a category error. Closed forms are overrides.

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

#### 15.6 `findNonConvexity()`, not `isConvex()`

Convexity quantifies over all `α ∈ (0,1]` **and** all `x` — both uncountable.
Sampled, it can only ever report *"no counterexample found"*. Returning `true`
asserts a proof we did not perform.

**Decided:** `findNonConvexity(over:): Counterexample?`. `null` means no
witness was found, which is what we actually know. This is §7's ethic — the
reason `fuzzy-laws` reports counterexamples rather than booleans — applied to
`fuzzy-set`. The same treatment applies to any sampled ∀ claim: containment and
equality included.

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
                         → fuzzy-algebra, fuzzy-set (test-scope consumable)
                           [fuzzy-set edge added in slice 2a — §16.6. The suites
                            follow the modules they validate; still acyclic.]

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
