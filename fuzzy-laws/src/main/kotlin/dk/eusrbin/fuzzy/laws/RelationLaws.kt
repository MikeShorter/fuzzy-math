package dk.eusrbin.fuzzy.laws

import dk.eusrbin.fuzzy.algebra.Algebra
import dk.eusrbin.fuzzy.algebra.TNorm
import dk.eusrbin.fuzzy.relation.FuzzyRelations
import dk.eusrbin.fuzzy.relation.Mapping
import dk.eusrbin.fuzzy.set.Domain
import dk.eusrbin.fuzzy.set.FuzzySets
import dk.eusrbin.fuzzy.set.MembershipFn
import dk.eusrbin.fuzzy.set.Product

/**
 * The laws of fuzzy-relation — what is soundly checkable, and nothing else
 * (CLAUDE.md §19.7's standard, §21.8's list).
 *
 * ```
 * compose = shadow(intersection(cyl(A), cyl(B)))      derivation, EXACT      §21.4
 * A ∘ (B ∘ C) = (A ∘ B) ∘ C                           p.346, forTNorm        §21.4
 * a transitivity witness reproduces                    §19.7(3)'s shape       §21.7
 * imageUnder = imageUnderRelation over a crisp graph   eq. (23), forTNorm     §21.6, §21.9
 * imageUnderRelation = shadow(intersection(cyl(a), R)) derivation, EXACT      §21.6
 * ```
 *
 * ## Why the derivation laws are `EXACT` for **any** t-norm
 *
 * Not an assertion — a reason (CLAUDE.md §21.4). Both sides of each derivation
 * compute the same multiset of degrees `T(…, …)` over the same domain in the
 * same fold order: **T touches the degrees, never the accumulator.** The
 * accumulator's reducer is `max` regardless of T, and §8's ratified point is
 * that max is exactly associative — selection, not arithmetic. So
 * order-independence is free and `EXACT` is sound whatever T does: §14.6(a)'s
 * *"tolerance calibrates per operation, not per algebra"* paying out. The
 * derivation laws therefore compare with `==` and ignore the suite tolerance.
 *
 * **Associativity is different**: the t-norm is applied twice in different
 * orders (`T(T(·,·),·)` against `T(·,T(·,·))` inside the folds), which is
 * arithmetic for every t-norm but min — hence the suite tolerance defaults to
 * [Tolerance.forTNorm], collapsing to `EXACT` for [Algebra.STANDARD] where
 * everything is selection. It is sound over any monotone T and any fixed
 * domain: over a finite fold, `T(a, max(b, c)) = max(T(a,b), T(a,c))` needs
 * only monotonicity.
 *
 * ## What is deliberately NOT here
 *
 * No "a transitive relation composes into itself" and no similarity suite:
 * the antecedent (transitivity of the subject) is never *established* over a
 * sampled domain, only not-refuted, and §19.7 forbids reporting a sampling gap
 * as a violation. The witness self-consistency law is the sound direction —
 * a returned witness is checkable whatever the domain, because refutation is
 * absolute (§16.4).
 */
public object RelationLaws {

    private const val COMPOSITION =
        "Zadeh 1965, §IV p.346 (composition — prose, no equation number); derivation CLAUDE.md §21.4"
    private const val ASSOCIATIVITY =
        "Zadeh 1965, §IV p.346 — \"the operation of composition has the associative property\""
    private const val WITNESS =
        "CLAUDE.md §19.7(3), §21.7 — a witness must reproduce"
    private const val IMAGE =
        "Zadeh 1965, §IV eq. (23), p.346; the crisp-graph reduction CLAUDE.md §21.6"
    private const val IMAGE_DERIVATION =
        "CLAUDE.md §21.6 — imageUnderRelation = shadow(intersection(cylindricalExtension(a), R))"

    /**
     * Checks the composition laws for the relations `a`, `b`, `c` — fuzzy sets
     * in `X × X`, Zadeh's own setting (p.345) — and **throws** on failure.
     */
    @JvmStatic
    @JvmOverloads
    public fun <X> verify(
        a: MembershipFn<Pair<X, X>>,
        b: MembershipFn<Pair<X, X>>,
        c: MembershipFn<Pair<X, X>>,
        over: Domain<X>,
        algebra: Algebra = Algebra.STANDARD,
        tolerance: Tolerance = Tolerance.forTNorm(algebra.tNorm),
    ) {
        check(a, b, c, over, algebra.tNorm, tolerance).assertHolds()
    }

    /** [verify] with a raw [TNorm] — for a t-norm you wrote yourself, §7's whole point. */
    @JvmStatic
    @JvmOverloads
    public fun <X> verify(
        a: MembershipFn<Pair<X, X>>,
        b: MembershipFn<Pair<X, X>>,
        c: MembershipFn<Pair<X, X>>,
        over: Domain<X>,
        tNorm: TNorm,
        tolerance: Tolerance = Tolerance.forTNorm(tNorm),
    ) {
        check(a, b, c, over, tNorm, tolerance).assertHolds()
    }

    /** Checks the composition laws and **returns** a [LawReport]. Never throws. */
    @JvmStatic
    @JvmOverloads
    public fun <X> check(
        a: MembershipFn<Pair<X, X>>,
        b: MembershipFn<Pair<X, X>>,
        c: MembershipFn<Pair<X, X>>,
        over: Domain<X>,
        algebra: Algebra = Algebra.STANDARD,
        tolerance: Tolerance = Tolerance.forTNorm(algebra.tNorm),
    ): LawReport = check(a, b, c, over, algebra.tNorm, tolerance)

    /**
     * [check] with a raw [TNorm].
     *
     * This overload is where the suite earns its keep for a consumer: §9 makes
     * `TNorm` implementable, so its composition laws must be checkable
     * (§14.5's rule — an extension point ships with its correctness criteria).
     * Associativity of sup-T composition needs the t-norm's **own** axioms —
     * associativity and monotonicity — so a hand-written "t-norm" that lacks
     * them fails here, and `fuzzy-laws`' tests assert that failure (§7's
     * test-of-the-test).
     */
    @JvmStatic
    @JvmOverloads
    public fun <X> check(
        a: MembershipFn<Pair<X, X>>,
        b: MembershipFn<Pair<X, X>>,
        c: MembershipFn<Pair<X, X>>,
        over: Domain<X>,
        tNorm: TNorm,
        tolerance: Tolerance = Tolerance.forTNorm(tNorm),
    ): LawReport {
        val checker = LawChecker("RelationLaws", "$tNorm over $over", tolerance, Sampling.DEFAULT)
        val pairs = Product.of(over, over)

        // -- 1. compose agrees with its derivation, EXACT (class KDoc for why).
        //
        // The derived form: extend each leg cylindrically onto V × (X × Y) —
        // coordinate order reshuffled so that `shadow`, which projects out the
        // FIRST coordinate, folds away the middle one — then intersect, then
        // shadow. Every piece is shipped 2a machinery (§19.2's promise).
        val composed = FuzzyRelations.compose(a, b, over, tNorm)
        val cylFirst = MembershipFn<Pair<X, Pair<X, X>>> { vxy ->
            a.apply(vxy.second.first to vxy.first)
        }
        val cylSecond = MembershipFn<Pair<X, Pair<X, X>>> { vxy ->
            b.apply(vxy.first to vxy.second.second)
        }
        val derived = FuzzySets.shadow(FuzzySets.intersection(cylFirst, cylSecond, tNorm), over)
        checker.lawOverDomain(
            "compose = shadow(intersection(cyl(A), cyl(B)))  [EXACT, any T]",
            COMPOSITION,
            pairs,
        ) { xy ->
            val direct = composed.apply(xy)
            val viaShadow = derived.apply(xy)
            if (direct == viaShadow) {
                null
            } else {
                Counterexample(
                    doubleArrayOf(direct, viaShadow),
                    "at $xy — direct fold = $direct, derived = $viaShadow " +
                        "(must be identical: same degrees, and the reducer is max — selection, not arithmetic)",
                )
            }
        }

        // -- 2. associativity, p.346 — suite tolerance (forTNorm by default).
        val leftAssoc = FuzzyRelations.compose(
            FuzzyRelations.compose(a, b, over, tNorm),
            c,
            over,
            tNorm,
        )
        val rightAssoc = FuzzyRelations.compose(
            a,
            FuzzyRelations.compose(b, c, over, tNorm),
            over,
            tNorm,
        )
        checker.lawOverDomain("(A ∘ B) ∘ C = A ∘ (B ∘ C)", ASSOCIATIVITY, pairs) { xy ->
            val lhs = leftAssoc.apply(xy)
            val rhs = rightAssoc.apply(xy)
            checker.eq(lhs, rhs, "((A∘B)∘C)(x,y)", "(A∘(B∘C))(x,y)")?.let {
                Counterexample(doubleArrayOf(lhs, rhs), "at $xy — $it")
            }
        }

        // -- 3. witness self-consistency, per subject. Sound over ANY domain:
        // a witness is absolute (§16.4), so if one comes back it must reproduce
        // — re-evaluating the degrees it names must give the degrees it carries,
        // and they must actually break the inequality. No witness → holds
        // vacuously, and deliberately so: "no witness found" is not a claim this
        // law can indict (§19.7).
        for ((name, relation) in listOf("A" to a, "B" to b, "C" to c)) {
            checker.law0("transitivity witness for $name reproduces", WITNESS) {
                val witness = FuzzyRelations.findNonTransitivity(relation, over, tNorm)
                    .witness ?: return@law0 null
                val composedAgain =
                    tNorm.apply(
                        relation.apply(witness.x to witness.via),
                        relation.apply(witness.via to witness.y),
                    )
                val directAgain = relation.apply(witness.x to witness.y)
                val detail = when {
                    composedAgain != witness.composed ->
                        "re-evaluating T(f(x,via), f(via,y)) gives $composedAgain, " +
                            "but the witness carries ${witness.composed}"
                    directAgain != witness.direct ->
                        "re-evaluating f(x,y) gives $directAgain, but the witness carries ${witness.direct}"
                    witness.composed <= witness.direct ->
                        "the witness does not witness: composed ${witness.composed} ≤ direct ${witness.direct}"
                    else -> null
                }
                detail?.let {
                    Counterexample(doubleArrayOf(witness.composed, witness.direct), "$witness — $it")
                }
            }
        }

        return checker.report()
    }

    /**
     * Checks the image laws for a set [a] and a crisp [mapping] — eq. (23)'s
     * operation against §21.6's generalisation — and **throws** on failure.
     */
    @JvmStatic
    @JvmOverloads
    public fun <X, Y> verifyImage(
        a: MembershipFn<X>,
        mapping: Mapping<X, Y>,
        overX: Domain<X>,
        overY: Domain<Y>,
        algebra: Algebra = Algebra.STANDARD,
    ) {
        checkImage(a, mapping, overX, overY, algebra).assertHolds()
    }

    /**
     * Checks the image laws and **returns** a [LawReport]. Never throws — but
     * note [FuzzyRelations.imageUnder]'s own precondition applies: [overX] must
     * be exhaustive, or the *operation* throws before any law runs. That is the
     * operation's contract (CLAUDE.md §21.5), not this suite's to soften.
     *
     * Two laws, at two calibrations — and the first run is why (CLAUDE.md
     * §21.9):
     *
     * 1. **eq. (23) = the relational image over the crisp graph** — pinning
     *    §21.6's bypass line as an executable fact: `imageUnder(a, T)` and
     *    `imageUnderRelation(a, graph(T))` are the same operation where both
     *    are honest. At **[Tolerance.forTNorm]**, not `EXACT`: the identity
     *    rests on the boundary axiom `T(d, 1) = d`, which is a fact about ℝ —
     *    whether an *implementation* of it selects or computes is a fact about
     *    the t-norm. `min(d, 1)` selects; `d × 1` happens to be exact by IEEE;
     *    but Łukasiewicz computes `max(0, d + 1 − 1)`, and `d + 1` transits the
     *    neighbourhood of 1, where a double resolves only to `ulp(1)` —
     *    §14.6(a)'s `1 − (1 − a)` with the signs changed, caught the same way:
     *    the suite failed for a correct operation on its first run (§21.9).
     * 2. **imageUnderRelation agrees with its derivation** —
     *    `shadow(intersection(cylindricalExtension(a), graph))`, §21.6. This
     *    one **is** `EXACT`, for any T: both sides compute identical arithmetic
     *    in identical fold order, a claim about the fold rather than about T.
     */
    @JvmStatic
    @JvmOverloads
    public fun <X, Y> checkImage(
        a: MembershipFn<X>,
        mapping: Mapping<X, Y>,
        overX: Domain<X>,
        overY: Domain<Y>,
        algebra: Algebra = Algebra.STANDARD,
    ): LawReport {
        // forTNorm, not EXACT — §21.9. Collapses to EXACT for STANDARD, where
        // the boundary axiom is implemented by selection.
        val checker = LawChecker(
            "RelationLaws.image",
            "$algebra over $overX → $overY",
            Tolerance.forTNorm(algebra.tNorm),
            Sampling.DEFAULT,
        )

        // The crisp graph of the mapping, as a fuzzy relation.
        val graph = MembershipFn<Pair<X, Y>> { xy ->
            if (mapping.apply(xy.first) == xy.second) 1.0 else 0.0
        }

        val image = FuzzyRelations.imageUnder(a, mapping, overX)
        val viaRelation = FuzzyRelations.imageUnderRelation(a, graph, overX, algebra)
        checker.lawOverDomain(
            "imageUnder(a, T) = imageUnderRelation(a, graph(T))  [eq. 23 vs §21.6]",
            IMAGE,
            overY,
        ) { y ->
            val lhs = image.apply(y)
            val rhs = viaRelation.apply(y)
            checker.eq(lhs, rhs, "imageUnder(y)", "imageUnderRelation(y)")?.let {
                Counterexample(doubleArrayOf(lhs, rhs), "at y = $y — $it")
            }
        }

        val derivedImage = FuzzySets.shadow(
            FuzzySets.intersection(FuzzyRelations.cylindricalExtension(a), graph, algebra),
            overX,
        )
        checker.lawOverDomain(
            "imageUnderRelation = shadow(intersection(cyl(a), R))  [EXACT, any T]",
            IMAGE_DERIVATION,
            overY,
        ) { y ->
            val lhs = viaRelation.apply(y)
            val rhs = derivedImage.apply(y)
            if (lhs == rhs) {
                null
            } else {
                Counterexample(doubleArrayOf(lhs, rhs), "at y = $y — direct fold = $lhs, derived = $rhs")
            }
        }

        return checker.report()
    }
}
