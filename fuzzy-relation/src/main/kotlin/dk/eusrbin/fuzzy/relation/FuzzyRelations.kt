package dk.eusrbin.fuzzy.relation

import dk.eusrbin.fuzzy.algebra.Algebra
import dk.eusrbin.fuzzy.algebra.TNorm
import dk.eusrbin.fuzzy.set.Domain
import dk.eusrbin.fuzzy.set.MembershipFn
import dk.eusrbin.fuzzy.set.Product
import dk.eusrbin.fuzzy.set.Verdict
import kotlin.math.max

/**
 * **Fuzzy relations** — composition, the sets induced by mappings, and the
 * relation queries. This module finishes Zadeh 1965.
 *
 * ## There is no `FuzzyRelation` type, on the paper's authority
 *
 * > *"In the context of fuzzy sets, a fuzzy relation in X is a fuzzy set in the
 * > product space X × X."*
 *
 * **Source:** Zadeh 1965, §IV p.345 (prose, no equation number) — with his
 * example `x ≫ y` *"regarded as a fuzzy set A in R²"*. A relation **is** a
 * `MembershipFn<Pair<X, Y>>`; there is nothing else to carry, so there is no
 * type (CLAUDE.md §21.2). Nothing here has a closed form to override, so
 * §21.3's amendment to §16.5 puts the queries here as statics rather than
 * members: *a query is a member when there is a subtype that both earns its
 * keep independently and has a closed form to override with.*
 *
 * Note the paper's relations are **homogeneous** — fuzzy sets in `X × X`. The
 * heterogeneous signatures below (`Pair<X, V>`, `Pair<V, Y>`) are this
 * library's generalisation, **derived**: the formulas never use `X = Y`, and
 * the types then catch argument-order mistakes at compile time (§21.4). Where
 * a query genuinely needs homogeneity — reflexivity, symmetry, transitivity —
 * its signature says `Pair<X, X>`.
 *
 * ## Cost model
 *
 * Every combinator here is lazy, like [dk.eusrbin.fuzzy.set.FuzzySets]': it
 * allocates one object and computes nothing until asked. The Sup-shaped ones
 * ([compose], [imageUnderRelation]) then pay **one fold over their domain per
 * evaluation** — composing k relations multiplies the cost k-fold, and §15.4's
 * wall applies. Cache results you will ask twice.
 */
public object FuzzyRelations {

    // ---- Zadeh §IV p.346: composition ---------------------------------------

    /**
     * The **composition** of [first] and [second] — Zadeh's `B ∘ A`.
     *
     * **Source:** Zadeh 1965, §IV p.346 (prose, **no equation number**):
     *
     * > *"the composition of two fuzzy relations A and B is denoted by B ∘ A and
     * > is defined as a fuzzy relation in X whose membership function is related
     * > to those of A and B by `f_{B∘A}(x, y) = Sup_v Min [f_A(x, v), f_B(v, y)]`."*
     *
     * ## Argument order: the path, not the notation
     *
     * Arguments read **left-to-right along the path x → v → y**: [first] is
     * Zadeh's `A` (the leg taking `(x, v)`), [second] is his `B` (the leg taking
     * `(v, y)`), and the result is his `B ∘ A` — his notation names the result
     * right-to-left, like function composition, and would be misread at a call
     * site (CLAUDE.md §21.4). In the heterogeneous case the types enforce the
     * path order: passing the legs backwards does not compile.
     *
     * ## sup-T, not just sup-min — and why that is not someone else's formula
     *
     * The `Min` in Zadeh's formula is the **intersection** of two cylindrical
     * extensions — this operation *is* `shadow(intersection(…, …), over)`, every
     * piece already shipped — and intersection is already parameterised by
     * [Algebra] under §6's rule (*"min/max are not a special mechanism"*). So the
     * [algebra] parameter is not a substitution into p.346; it is the same
     * composite with intersection's existing parameter left visible (§21.4).
     * **Source:** p.346 for the default; **Attributed:** the name "sup-T
     * composition". The outer `Sup` is *not* parameterised — no source on hand
     * defines any other outer fold.
     *
     * The shipped body is the direct fold rather than the derivation — the
     * derived form allocates an extra `Pair` per grid point — and `fuzzy-laws`'
     * `RelationLaws` asserts they agree **exactly**, for any t-norm:
     * [boundedDifference][dk.eusrbin.fuzzy.set.FuzzySets.boundedDifference]'s
     * precedent (§17.3, §21.4).
     *
     * ## Associativity, and cost
     *
     * P.346 states `A ∘ (B ∘ C) = (A ∘ B) ∘ C`; `RelationLaws` checks it
     * (tolerance `forTNorm` — the t-norm is applied twice in different orders).
     * The result re-folds [over] on **every evaluation**, so a chain of k
     * compositions costs `|V|^(k-1)` evaluations per point. That is the honest
     * price of §15.1 keeping domains out of the result's identity; cache what
     * you will ask twice.
     *
     * @param first the leg `X × V` — Zadeh's `A`.
     * @param second the leg `V × Y` — Zadeh's `B`.
     * @param over the domain of the **middle** coordinate, the one the `Sup`
     *   folds away. Over a [dk.eusrbin.fuzzy.set.Sampled] middle the result is a
     *   lower bound on the true composition, per `height`'s standing caveat.
     */
    @JvmStatic
    @JvmOverloads
    public fun <X, V, Y> compose(
        first: MembershipFn<Pair<X, V>>,
        second: MembershipFn<Pair<V, Y>>,
        over: Domain<V>,
        algebra: Algebra = Algebra.STANDARD,
    ): MembershipFn<Pair<X, Y>> = compose(first, second, over, algebra.tNorm)

    /** [compose] with a raw [TNorm] — the escape hatch of §6, minus the bundle's guarantees. */
    @JvmStatic
    public fun <X, V, Y> compose(
        first: MembershipFn<Pair<X, V>>,
        second: MembershipFn<Pair<V, Y>>,
        over: Domain<V>,
        tNorm: TNorm,
    ): MembershipFn<Pair<X, Y>> = MembershipFn { xy ->
        val x = xy.first
        val y = xy.second
        over.reduceDegrees(
            MembershipFn { v -> tNorm.apply(first.apply(x to v), second.apply(v to y)) },
            0.0,
        ) { accumulator, degree -> max(accumulator, degree) }
    }

    // ---- Zadeh §IV p.346: fuzzy sets induced by mappings --------------------

    /**
     * The fuzzy set in `X` induced by [b] through the **inverse** of [mapping] —
     * eq. (22): `f_A(x) = f_B(T(x))`.
     *
     * **Source:** Zadeh 1965, §IV eq. **(22)**, p.346 — *"The inverse mapping
     * T⁻¹ induces a fuzzy set A in X whose membership function is defined by
     * `f_A(x) = f_B(y), y ∈ Y`, for all x in X which are mapped by T into y."*
     *
     * **Pointwise, total, and needs no domain** — the half of "fuzzy sets
     * induced by mappings" with no soundness question attached (CLAUDE.md
     * §21.1). Contrast [imageUnder], the forward direction, which is a
     * constrained Sup and carries a precondition.
     */
    @JvmStatic
    public fun <X, Y> preimageUnder(b: MembershipFn<Y>, mapping: Mapping<X, Y>): MembershipFn<X> =
        MembershipFn { x -> b.apply(mapping.apply(x)) }

    /**
     * The fuzzy set in `Y` induced by [a] under [mapping] — eq. (23):
     * `f_B(y) = Max_{x ∈ T⁻¹(y)} f_A(x)`.
     *
     * **Source:** Zadeh 1965, §IV eq. **(23)**, p.346 — including the `Max`:
     * he writes `Max`, not `Sup`, and his usage elsewhere distinguishes the two
     * deliberately (CLAUDE.md §21.1). Over an exhaustive domain this fold *is*
     * his finite Max, verbatim.
     *
     * ## Requires an exhaustive domain — this operation is unsound on a grid
     *
     * The Sup here is constrained to the **preimage** `T⁻¹(y)`, selected by
     * `T(x) == y`. Over a [dk.eusrbin.fuzzy.set.Sampled] domain that selection
     * is almost surely **empty** — the grid contains no point of `T⁻¹(y)` — so
     * the fold would return its `initial`, `0.0`, as *the* image: §16.3's lie,
     * with no error bar. And refining never helps: `T⁻¹(y)` is typically
     * measure-zero, so a finer grid still misses it. This is not `height`'s
     * lower bound that converges — **it converges to nothing** (CLAUDE.md
     * §21.5).
     *
     * So this throws unless [Domain.isExhaustive] — a one-time O(1) field read,
     * not §4's per-call fold, and unlike `separationDegree`'s preconditions the
     * caller cannot know better than the domain does: `isExhaustive` *is* the
     * domain answering. Note the check is a **runtime** one because
     * exhaustiveness is computed, not declared (§16.4):
     * `Product(Enumerable, Enumerable)` is exhaustive and welcome here — it is
     * exactly the domain a relation on `X × X` analyses over — and no static
     * type could admit it while refusing `Sampled` (§21.5).
     *
     * Over an exhaustive domain an **empty preimage is honest**: no enumerated
     * `x` maps to [a] given `y`, so `0.0` — no membership in the image — is the
     * right answer, not a fold artifact.
     *
     * If what you have is a *fuzzy* relation rather than a crisp mapping, use
     * [imageUnderRelation], which folds degrees instead of selecting and is
     * sound over any domain. For the extension principle over ℝ specifically,
     * `fuzzy-number`'s α-cut arithmetic is the sound route (§20.3).
     *
     * @throws IllegalArgumentException if [over] is not exhaustive.
     */
    @JvmStatic
    public fun <X, Y> imageUnder(
        a: MembershipFn<X>,
        mapping: Mapping<X, Y>,
        over: Domain<X>,
    ): MembershipFn<Y> {
        require(over.isExhaustive) {
            "imageUnder needs an exhaustive domain (Enumerable, or a Product of " +
                "exhaustive factors), but was given $over. Over a sampled grid the " +
                "preimage T⁻¹(y) is almost surely empty, and the fold would return " +
                "0.0 as the image — the wrong answer with no error bar, and refining " +
                "the grid never fixes it (CLAUDE.md §21.5). For fuzzy relations use " +
                "imageUnderRelation, which is sound over any domain."
        }
        // Snapshot once: the domain is immutable, and Product.elements() is the
        // expensive call §15.4 warns about — pay it at construction, not per y.
        val elements = over.elements()
        return MembershipFn { y ->
            var supremum = 0.0
            for (i in elements.indices) {
                val x = elements[i]
                if (mapping.apply(x) == y) {
                    supremum = max(supremum, a.apply(x))
                }
            }
            supremum
        }
    }

    // ---- Derived: cylindrical extension and the relational image ------------

    /**
     * The **cylindrical extension** of [a] to the product space:
     * `f(x, y) = f_A(x)` — membership blind to the second coordinate.
     *
     * **Derived:** the formula is its own proof; nothing in it needs a source.
     * **Attributed:** the name, which is 1975 vocabulary and not in the paper
     * (CLAUDE.md §21.6).
     *
     * The building block of §21.4's and §21.6's derivations: [compose] is the
     * shadow of the intersection of two of these (modulo coordinate order), and
     * [imageUnderRelation] literally is
     * `shadow(intersection(cylindricalExtension(a), r), over)` — `fuzzy-laws`
     * asserts both. To extend along the *first* coordinate instead
     * (`f(x, y) = f_A(y)`), write the one-line lambda; this ships the
     * orientation the derivations use.
     */
    @JvmStatic
    public fun <X, Y> cylindricalExtension(a: MembershipFn<X>): MembershipFn<Pair<X, Y>> =
        MembershipFn { xy -> a.apply(xy.first) }

    /**
     * The image of [a] under the fuzzy [relation] —
     * `f_B(y) = Sup_x T(f_A(x), f_R(x, y))`.
     *
     * **Derived**, from pieces read and shipped: it is
     * `shadow(intersection(cylindricalExtension(a), relation), over)` —
     * [shadow][dk.eusrbin.fuzzy.set.FuzzySets.shadow] (§V p.350),
     * [intersection][dk.eusrbin.fuzzy.set.FuzzySets.intersection] (§II eq. (5),
     * parameterised per §6), and [cylindricalExtension]. The shipped body is
     * the direct fold; `RelationLaws` asserts the identity exactly (§21.6).
     *
     * It **generalises eq. (23)**: when [relation] is the crisp graph of a
     * mapping `T`, this is [imageUnder] — `T(a, 1) = a` and `T(a, 0) = 0`, so
     * the fold reduces to the Max over the preimage. That identity is a fact
     * about ℝ; in IEEE 754 the Łukasiewicz boundary *computes* `(a + 1) − 1`
     * and rounds (CLAUDE.md §21.9), so `fuzzy-laws` pins the agreement at
     * `forTNorm` rather than exactly.
     *
     * ## Why this is sound over a [dk.eusrbin.fuzzy.set.Sampled] domain when [imageUnder] is not
     *
     * There is **no selection here** — every grid point contributes a degree to
     * the fold — so over a grid the result is an honest lower bound that
     * converges under refinement, `height`'s standing caveat and nothing worse.
     * The exception is a [relation] that is indicator-like, concentrated on a
     * measure-zero set of the product space: hand-encoding a crisp graph as a
     * relation reintroduces exactly the failure [imageUnder]'s exhaustiveness
     * guard exists to stop, and the grid's lower bound is then vacuously `0.0`
     * (CLAUDE.md §21.6). Crisp graphs belong in [imageUnder], which is honest
     * about what they need.
     *
     * **Attributed:** the control literature builds the *compositional rule of
     * inference* from this composition (Zadeh 1973, **not on hand**); the name
     * is not carried into this API — §11a ships the seam, not the vocabulary
     * (CLAUDE.md §21.6).
     *
     * Costs one fold over [over] per evaluation, like [compose].
     */
    @JvmStatic
    @JvmOverloads
    public fun <X, Y> imageUnderRelation(
        a: MembershipFn<X>,
        relation: MembershipFn<Pair<X, Y>>,
        over: Domain<X>,
        algebra: Algebra = Algebra.STANDARD,
    ): MembershipFn<Y> = imageUnderRelation(a, relation, over, algebra.tNorm)

    /** [imageUnderRelation] with a raw [TNorm] — §6's escape hatch. */
    @JvmStatic
    public fun <X, Y> imageUnderRelation(
        a: MembershipFn<X>,
        relation: MembershipFn<Pair<X, Y>>,
        over: Domain<X>,
        tNorm: TNorm,
    ): MembershipFn<Y> = MembershipFn { y ->
        over.reduceDegrees(
            MembershipFn { x -> tNorm.apply(a.apply(x), relation.apply(x to y)) },
            0.0,
        ) { accumulator, degree -> max(accumulator, degree) }
    }

    // ---- The relation queries (§21.7) ----------------------------------------
    //
    // Statics, not members, by §21.3's amendment to §16.5: these constrain X's
    // shape (Pair<X, X>, the same X on both sides — reflexivity needs f(x,x),
    // symmetry compares f(x,y) with f(y,x)), so a member would need a subtype,
    // and no subtype here has anything to override. The formulas are stated
    // below and are unambiguous; the NAMES are Attributed: — Zadeh 1971 is not
    // on hand, and the literature has variants (ε-reflexivity, weak
    // reflexivity). Naming a function is not a claim about mathematics (§17.4).
    //
    // All comparisons are exact IEEE comparisons of double degrees, in
    // checkEquality's stance (§18.2): for a non-min t-norm the composed degree
    // is computed, so a Refuted attests the floating-point inequality — the
    // only one the machine can attest (§21.7).

    /**
     * Searches for an `x` with `f_R(x, x) < 1` — a witness against
     * **reflexivity**, the property `f_R(x, x) = 1` for all `x`.
     *
     * **Attributed:** the name and the choice of `1` as the threshold (Zadeh
     * 1971, **not on hand** — CLAUDE.md §21.7). The formula checked is exactly
     * the one stated here.
     *
     * One call re-derives a witness: evaluate `f_R(x, x)` and see it fall short.
     */
    @JvmStatic
    public fun <X> findNonReflexivity(
        relation: MembershipFn<Pair<X, X>>,
        over: Domain<X>,
    ): Verdict<X> =
        Verdict.of(over.firstWhere { x -> relation.apply(x to x) < 1.0 }, over)

    /**
     * Searches for a pair with `f_R(x, y) ≠ f_R(y, x)` — a witness against
     * **symmetry**.
     *
     * **Attributed:** the name (Zadeh 1971, **not on hand**). The comparison is
     * exact equality of degrees, as [checkEquality]
     * [dk.eusrbin.fuzzy.set.MembershipFn.checkEquality]'s is (§18.2).
     *
     * The search runs over [Product]`(over, over)` — §16.4's motivating case
     * doing its job: the product of exhaustive factors is exhaustive, so a
     * symmetric relation over an [dk.eusrbin.fuzzy.set.Enumerable] earns
     * [Verdict.Proven] through a runtime conjunction no static type could
     * express. Two calls re-derive a witness.
     */
    @JvmStatic
    public fun <X> findNonSymmetry(
        relation: MembershipFn<Pair<X, X>>,
        over: Domain<X>,
    ): Verdict<Pair<X, X>> {
        val pairs = Product.of(over, over)
        return Verdict.of(
            pairs.firstWhere { xy -> relation.apply(xy) != relation.apply(xy.second to xy.first) },
            pairs.isExhaustive,
        )
    }

    /**
     * Searches for a path `x → via → y` with
     * `T(f_R(x, via), f_R(via, y)) > f_R(x, y)` — a witness against
     * **T-transitivity**, the property `R ∘ R ⊆ R`.
     *
     * The property is [compose] plus containment (eq. (2)), both `Source:`;
     * **Attributed:** the name "T-transitivity" (Zadeh 1971, **not on hand** —
     * CLAUDE.md §21.7).
     *
     * ## Searched directly, not through [compose]
     *
     * `checkContainment(compose(r, r, over), r, …)` would find the same
     * failures at the same O(|X|³) cost, but its witness is an `(x, y)` — the
     * *path* that broke transitivity is exactly what it discards, and the
     * witness is the product (§7). The direct search returns the triple, with
     * both degrees carried so it reproduces exactly ([TransitivityWitness]).
     * It also keeps this query's laws free of [compose]'s guards — §20.9's
     * rule: a law's soundness is inherited through the default bodies it
     * calls, and this calls none.
     *
     * Note there is **no "similarity" bundle** shipping alongside this
     * (CLAUDE.md §21.7): every conjunct — reflexive, symmetric, T-transitive —
     * is already here and checkable, so a bundle's only content would be the
     * claim that the conjunction is what Zadeh 1971 means, which is precisely
     * what cannot be checked against a source not on hand.
     *
     * Costs `O(|X|³)` relation evaluations and materialises the element list
     * once. [Verdict.Proven] is reachable over an
     * [dk.eusrbin.fuzzy.set.Enumerable]: a ∀ over `X³` of exhaustive factors is
     * a proof.
     */
    @JvmStatic
    @JvmOverloads
    public fun <X> findNonTransitivity(
        relation: MembershipFn<Pair<X, X>>,
        over: Domain<X>,
        algebra: Algebra = Algebra.STANDARD,
    ): Verdict<TransitivityWitness<X>> = findNonTransitivity(relation, over, algebra.tNorm)

    /** [findNonTransitivity] with a raw [TNorm] — §6's escape hatch. */
    @JvmStatic
    public fun <X> findNonTransitivity(
        relation: MembershipFn<Pair<X, X>>,
        over: Domain<X>,
        tNorm: TNorm,
    ): Verdict<TransitivityWitness<X>> {
        val elements = over.elements()
        for (x in elements) {
            for (via in elements) {
                // Hoisted: f_R(x, via) is constant across the inner loop.
                val left = relation.apply(x to via)
                for (y in elements) {
                    val composed = tNorm.apply(left, relation.apply(via to y))
                    val direct = relation.apply(x to y)
                    if (composed > direct) {
                        return Verdict.Refuted(TransitivityWitness(x, via, y, composed, direct))
                    }
                }
            }
        }
        return Verdict.noWitness(over.isExhaustive)
    }
}
