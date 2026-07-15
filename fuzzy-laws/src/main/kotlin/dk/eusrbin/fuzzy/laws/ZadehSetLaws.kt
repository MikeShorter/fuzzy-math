package dk.eusrbin.fuzzy.laws

import dk.eusrbin.fuzzy.algebra.Algebra
import dk.eusrbin.fuzzy.set.Domain
import dk.eusrbin.fuzzy.set.FuzzySets
import dk.eusrbin.fuzzy.set.MembershipFn
import kotlin.math.max
import kotlin.math.min

/**
 * **Zadeh's own set-level claims**, made executable.
 *
 * Every law here is a sentence in the 1965 paper, at a verified equation number
 * (CLAUDE.md §17.1, §18.1):
 *
 * ```
 * (A ∪ B)' = A' ∩ B'                         De Morgan          (7)   §III
 * (A ∩ B)' = A' ∪ B'                                            (8)   §III
 * C ∩ (A ∪ B) = (C ∩ A) ∪ (C ∩ B)            distributive       (9)   §III
 * C ∪ (A ∩ B) = (C ∪ A) ∩ (C ∪ B)                               (10)  §III
 * AB ⊂ A ∩ B                                                    (15)  §IV
 * A ∩ B ⊂ (A, B; Λ) ⊂ A ∪ B    for all Λ                        (19)  §IV
 * ```
 *
 * ## What this adds over the degree-level suites — and what it does not
 *
 * Worth being straight about, because the honest answer is "less than it looks".
 *
 * Every law above is **pointwise**, so each reduces to a law about *degrees*
 * that `fuzzy-laws` already checks over a far wider sample: eq. (7) at `x` is
 * exactly [DeMorganLaws]' `N(S(a,b)) = T(N(a),N(b))` at `a = f_A(x)`,
 * `b = f_B(x)`. Zadeh says as much himself — eq. (19), he notes, *"is an
 * immediate consequence of the inequalities"* of eq. (20), which is the
 * degree-level statement.
 *
 * [DeMorganLaws] samples 32 degrees and every pair of them. This samples only
 * the degree pairs two particular sets happen to produce on one domain. As a
 * check of the **mathematics**, it is strictly weaker.
 *
 * What it *does* check is the **lifting** — that `FuzzySets.union` really is the
 * algebra's conorm applied pointwise, that `complement` really is its negation,
 * that nothing was transposed on the way from §11.7 to §II. A degree law cannot
 * see that; only running the set operations can. So:
 *
 * - to check an **algebra**, use [DeMorganLaws] and [StandardLaws];
 * - to check that **your sets behave**, use this;
 * - to check a **membership function's overrides**, use [MembershipFnLaws],
 *   which is the one that guards something nothing else can.
 *
 * ## Tiering carries over exactly
 *
 * CLAUDE.md §7's tiers are about the algebra, and lifting them to sets changes
 * nothing: eqs. **(9)** and **(10)** are the distributive laws, so they hold for
 * `Algebra.STANDARD` and **fail for [Algebra.PRODUCT]** — at the set level for
 * precisely the reason they fail at the degree level. `fuzzy-laws`' own tests
 * assert that failure, per §7's test-of-the-test.
 *
 * Eqs. (7), (8), (15) and (19) hold for any De Morgan triple, any t-norm, and any
 * Λ respectively — they are not tier-bound.
 */
public object ZadehSetLaws {

    private const val DE_MORGAN = "Zadeh 1965, §III eqs. (7), (8)"
    private const val DISTRIBUTIVE = "Zadeh 1965, §III eqs. (9), (10)"
    private const val PRODUCT_CONTAINED = "Zadeh 1965, §IV eq. (15), p.344"
    private const val CONVEX_BETWEEN = "Zadeh 1965, §IV eq. (19), p.345 (via eq. (20))"

    /**
     * Checks the laws for `a`, `b`, `c` over [over] and **throws** on failure.
     *
     * @param lambda the Λ of eq. (19) — **a fuzzy set**, not a scalar (§17.2).
     *   Defaults to a constant `0.5`, which is the special case eq. (20)'s proof
     *   uses; pass a varying Λ to exercise the general form.
     */
    @JvmStatic
    @JvmOverloads
    public fun <X> verify(
        a: MembershipFn<X>,
        b: MembershipFn<X>,
        c: MembershipFn<X>,
        over: Domain<X>,
        algebra: Algebra = Algebra.STANDARD,
        lambda: MembershipFn<X> = FuzzySets.constant(0.5),
        tolerance: Tolerance = Tolerance.looserOf(
            Tolerance.forAlgebra(algebra),
            Tolerance.forNegation(algebra.negation),
        ),
    ) {
        check(a, b, c, over, algebra, lambda, tolerance).assertHolds()
    }

    /** Checks the laws and **returns** a [LawReport]. Never throws. */
    @JvmStatic
    @JvmOverloads
    public fun <X> check(
        a: MembershipFn<X>,
        b: MembershipFn<X>,
        c: MembershipFn<X>,
        over: Domain<X>,
        algebra: Algebra = Algebra.STANDARD,
        lambda: MembershipFn<X> = FuzzySets.constant(0.5),
        tolerance: Tolerance = Tolerance.looserOf(
            Tolerance.forAlgebra(algebra),
            Tolerance.forNegation(algebra.negation),
        ),
    ): LawReport {
        val checker = LawChecker("ZadehSetLaws", "$algebra over $over", tolerance, Sampling.DEFAULT)

        val union = FuzzySets.union(a, b, algebra)
        val intersection = FuzzySets.intersection(a, b, algebra)

        // (7) — (A ∪ B)' = A' ∩ B'
        val leftSeven = FuzzySets.complement(union, algebra.negation)
        val rightSeven = FuzzySets.intersection(
            FuzzySets.complement(a, algebra.negation),
            FuzzySets.complement(b, algebra.negation),
            algebra,
        )
        checker.lawOverDomain("De Morgan: (A ∪ B)' = A' ∩ B'  [eq. 7]", DE_MORGAN, over) { x ->
            compare(checker, x, leftSeven, rightSeven, a, b, "(A ∪ B)'", "A' ∩ B'")
        }

        // (8) — (A ∩ B)' = A' ∪ B'
        val leftEight = FuzzySets.complement(intersection, algebra.negation)
        val rightEight = FuzzySets.union(
            FuzzySets.complement(a, algebra.negation),
            FuzzySets.complement(b, algebra.negation),
            algebra,
        )
        checker.lawOverDomain("De Morgan: (A ∩ B)' = A' ∪ B'  [eq. 8]", DE_MORGAN, over) { x ->
            compare(checker, x, leftEight, rightEight, a, b, "(A ∩ B)'", "A' ∪ B'")
        }

        // (9) — C ∩ (A ∪ B) = (C ∩ A) ∪ (C ∩ B).   Fails for PRODUCT, by design.
        val leftNine = FuzzySets.intersection(c, union, algebra)
        val rightNine = FuzzySets.union(
            FuzzySets.intersection(c, a, algebra),
            FuzzySets.intersection(c, b, algebra),
            algebra,
        )
        checker.lawOverDomain("distributive: C ∩ (A ∪ B) = (C ∩ A) ∪ (C ∩ B)  [eq. 9]", DISTRIBUTIVE, over) { x ->
            compare(checker, x, leftNine, rightNine, a, b, "C ∩ (A ∪ B)", "(C ∩ A) ∪ (C ∩ B)")
        }

        // (10) — C ∪ (A ∩ B) = (C ∪ A) ∩ (C ∪ B).  Fails for PRODUCT, by design.
        val leftTen = FuzzySets.union(c, intersection, algebra)
        val rightTen = FuzzySets.intersection(
            FuzzySets.union(c, a, algebra),
            FuzzySets.union(c, b, algebra),
            algebra,
        )
        checker.lawOverDomain("distributive: C ∪ (A ∩ B) = (C ∪ A) ∩ (C ∪ B)  [eq. 10]", DISTRIBUTIVE, over) { x ->
            compare(checker, x, leftTen, rightTen, a, b, "C ∪ (A ∩ B)", "(C ∪ A) ∩ (C ∪ B)")
        }

        // (15) — AB ⊂ A ∩ B. Zadeh states it against the MIN intersection
        // ("Clearly, AB ⊂ A ∩ B", p.344), so this uses Min regardless of the
        // algebra under test — it is his claim, not a claim about the bundle.
        val algebraicProduct = FuzzySets.algebraicProduct(a, b)
        checker.lawOverDomain("AB ⊂ A ∩ B  [eq. 15]", PRODUCT_CONTAINED, over) { x ->
            val product = algebraicProduct.apply(x)
            val meet = min(a.apply(x), b.apply(x))
            val detail = checker.leq(product, meet, "f_AB(x)", "Min[f_A(x), f_B(x)]")
            detail?.let { Counterexample(doubleArrayOf(a.apply(x), b.apply(x)), "x = $x — $it") }
        }

        // (19) — A ∩ B ⊂ (A, B; Λ) ⊂ A ∪ B, for all Λ. Zadeh derives it from
        // eq. (20)'s Min ≤ λf_A + (1−λ)f_B ≤ Max, which is a Min/Max statement,
        // so this too is against Min/Max rather than the bundle.
        val combination = FuzzySets.convexCombination(a, b, lambda)
        checker.lawOverDomain("A ∩ B ⊂ (A, B; Λ) ⊂ A ∪ B  [eq. 19]", CONVEX_BETWEEN, over) { x ->
            val fa = a.apply(x)
            val fb = b.apply(x)
            val mid = combination.apply(x)
            val lower = checker.leq(min(fa, fb), mid, "Min[f_A(x), f_B(x)]", "f_(A,B;Λ)(x)")
            val upper = checker.leq(mid, max(fa, fb), "f_(A,B;Λ)(x)", "Max[f_A(x), f_B(x)]")
            (lower ?: upper)?.let {
                Counterexample(doubleArrayOf(fa, fb, lambda.apply(x)), "x = $x — $it")
            }
        }

        return checker.report()
    }

    /** `null` if the two sets agree at [x] within tolerance; otherwise a [Counterexample]. */
    private fun <X> compare(
        checker: LawChecker,
        x: X,
        left: MembershipFn<X>,
        right: MembershipFn<X>,
        a: MembershipFn<X>,
        b: MembershipFn<X>,
        leftExpr: String,
        rightExpr: String,
    ): Counterexample? {
        val detail = checker.eq(left.apply(x), right.apply(x), leftExpr, rightExpr) ?: return null
        return Counterexample(doubleArrayOf(a.apply(x), b.apply(x)), "x = $x — $detail")
    }
}
