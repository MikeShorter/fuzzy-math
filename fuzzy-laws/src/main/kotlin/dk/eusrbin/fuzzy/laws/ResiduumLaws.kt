package dk.eusrbin.fuzzy.laws

import dk.eusrbin.fuzzy.algebra.TNorm
import dk.eusrbin.fuzzy.algebra.TNorms
import kotlin.math.abs

/**
 * **The residuated tier.** The residuation adjunction.
 *
 * Source: Bergmann 2008, §11.7 and §12 (residuated lattices).
 *
 * ```
 * T(a, z) ≤ b   ⟺   z ≤ (a ⇒ b)              residuation
 * (a ⇒ b) = 1   ⟺   a ≤ b                    the induced order
 * ```
 *
 * CLAUDE.md §7's third tier: holds for a **left-continuous** t-norm together
 * with its residuum. CLAUDE.md §5 states the adjunction in categorical terms —
 * `T(a,−) ⊣ (a ⇒ −)`: the residuum is the right adjoint of conjunction. That is
 * not decoration. Right adjoints are unique when they exist, which is *exactly*
 * why §5 can say the residuum is "determined" and refuse to accept one as a
 * parameter. There is nothing to configure because there is nothing to choose.
 *
 * ## Left-continuity is a real precondition
 *
 * The supremum `sup { z | T(a,z) ≤ b }` is computable for *any* t-norm —
 * [TNorm.residuum] will happily return one. The adjunction is what fails when
 * the norm is not left-continuous, because the supremum is not attained.
 *
 * **[TNorms.DRASTIC] must fail this suite**, and its failure is a fact rather
 * than a bug:
 *
 * - `T(0.5, z) = 0 ≤ 0.3` for every `z < 1`, so `(0.5 ⇒ 0.3) = 1`.
 * - `z = 1 ≤ 1 = (0.5 ⇒ 0.3)` — the right side of the adjunction holds.
 * - `T(0.5, 1) = 0.5 > 0.3` — the left side does not.
 *
 * No epsilon rescues that; the gap is `0.2`. It is the cheapest demonstration
 * available that CLAUDE.md §5's "for a left-continuous t-norm" is load-bearing.
 * `fuzzy-laws`' own tests assert this failure, in the same spirit as CLAUDE.md
 * §7's requirement that `StandardLaws` provably fail for Product.
 *
 * [TNorms.NILPOTENT_MINIMUM] is the instructive contrast: left-continuous but
 * not continuous, so it **passes** this suite and fails [BLAlgebraLaws].
 *
 * ## On checking an ⟹ with a tolerance: never soften the hypothesis
 *
 * Each direction is an implication, and the two halves are treated differently
 * **on purpose**: the hypothesis is tested with exact `≤`, the conclusion with
 * the tolerant one. Softening the hypothesis instead widens the set of cases the
 * conclusion must cover, and where `T(a, ·)` is nearly flat, an `ε` of slack in
 * `b` corresponds to an unbounded slack in `z`.
 *
 * That is not hypothetical — [Sampling]'s edge set contains a pair that
 * demonstrates it. For the Product t-norm at `a = Double.MIN_VALUE`, `b = 0`:
 *
 * - `T(a, 1) = 4.9e-324`, which a tolerant hypothesis reads as "`≤ 0`";
 * - so the conclusion demands `1 = z ≤ (a ⇒ b) = b/a = 0`, and fails.
 *
 * Nothing is wrong with the Product t-norm. The test was wrong. Tested strictly,
 * `4.9e-324 > 0` and the case is simply not one the law speaks about.
 *
 * The cost is that a handful of near-boundary triples go unchecked. The benefit
 * is that a pass means something. Real violations are untouched by the choice —
 * Drastic misses by `0.2`, not by an ulp.
 */
public object ResiduumLaws {

    private const val CITATION = "Bergmann 2008, §11.7, §12 (residuated lattices); CLAUDE.md §5"

    /**
     * Checks [tNorm] against its own residuum and **throws** on any failure.
     *
     * There is no parameter for the residuum. That is the point — CLAUDE.md §5:
     * *"The API computes it; it is not a free parameter."* This suite verifies
     * a t-norm against the residuum it already carries ([TNorm.residuum]).
     *
     * @throws LawViolationException if either direction of the adjunction fails.
     */
    @JvmStatic
    @JvmOverloads
    public fun verify(
        tNorm: TNorm,
        tolerance: Tolerance = Tolerance.forTNorm(tNorm),
        sampling: Sampling = Sampling.DEFAULT,
    ) {
        check(tNorm, tolerance, sampling).assertHolds()
    }

    /** Checks [tNorm] against its own residuum and **returns** a [LawReport]. Never throws. */
    @JvmStatic
    @JvmOverloads
    public fun check(
        tNorm: TNorm,
        tolerance: Tolerance = Tolerance.forTNorm(tNorm),
        sampling: Sampling = Sampling.DEFAULT,
    ): LawReport {
        val checker = LawChecker("ResiduumLaws", tNorm.toString(), tolerance, sampling)

        // T(a,z) ≤ b  ⟹  z ≤ (a ⇒ b)
        //
        // The hypothesis is tested STRICTLY and the conclusion tolerantly. This
        // asymmetry is deliberate and load-bearing — see the class KDoc.
        checker.law3("residuation →: T(a,z) ≤ b ⟹ z ≤ (a⇒b)", CITATION) { a, b, z ->
            if (tNorm.apply(a, z) > b) {
                null
            } else {
                checker.leq(z, tNorm.residuum(a, b), "z", "(a⇒b)")
            }
        }

        // z ≤ (a ⇒ b)  ⟹  T(a,z) ≤ b
        // This is the direction Drastic fails: its supremum is not attained.
        checker.law3("residuation ←: z ≤ (a⇒b) ⟹ T(a,z) ≤ b", CITATION) { a, b, z ->
            if (z > tNorm.residuum(a, b)) {
                null
            } else {
                checker.leq(tNorm.apply(a, z), b, "T(a,z)", "b")
            }
        }

        // The order induced by the residuum must be the order on [0,1].
        // A corollary of residuation, and a much cheaper thing to read in a
        // failure report than a rejected triple.
        checker.law2("induced order: (a⇒b) = 1 ⟺ a ≤ b", CITATION) { a, b ->
            // An ⟺ between a tolerant predicate and a discontinuous one cannot be
            // reconciled by making both tolerant — the pool contains pairs that
            // break it in each direction:
            //
            //   a = 1.0, b = 1 − ulp(1)  — Product gives (a⇒b) = b/a = 1 − ulp,
            //       which IS "≈ 1" at ε = 1e-14, while `a ≤ b` is strictly false.
            //   a = MIN_VALUE, b = 0.0   — `a ≤ b` is TRUE at any ε > 0, while
            //       (a⇒b) = b/a = 0 is emphatically not 1.
            //
            // Both are correct behaviour from a correct t-norm. So skip the band
            // where the two halves genuinely disagree about what "equal" means,
            // and judge strictly outside it. At ε = 0 (the Zadeh tier) the band
            // is empty and nothing is skipped.
            if (a != b && abs(a - b) <= tolerance.epsilon) return@law2 null

            val residuum = tNorm.residuum(a, b)
            val isOne = tolerance.eq(residuum, 1.0)
            val isBelow = a <= b
            when {
                isOne && !isBelow ->
                    "(a⇒b) = $residuum ≈ 1, but a = $a exceeds b = $b  (ε = ${tolerance.epsilon})"
                !isOne && isBelow ->
                    "a = $a ≤ b = $b, but (a⇒b) = $residuum ≠ 1  (ε = ${tolerance.epsilon})"
                else -> null
            }
        }

        return checker.report()
    }
}
