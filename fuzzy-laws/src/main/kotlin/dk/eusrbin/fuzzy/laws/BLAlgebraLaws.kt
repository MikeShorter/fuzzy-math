package dk.eusrbin.fuzzy.laws

import dk.eusrbin.fuzzy.algebra.TNorm
import dk.eusrbin.fuzzy.algebra.TNorms
import kotlin.math.max
import kotlin.math.min

/**
 * **The BL tier.** Divisibility and prelinearity — what a left-continuous
 * residuated t-norm needs to generate a **BL-algebra** (Hájek's Basic Logic).
 *
 * Source: Bergmann 2008, §12 (MV-algebras, residuated lattices, BL-algebras).
 *
 * ```
 * T(a, a ⇒ b) = min(a, b)                     divisibility
 * max(a ⇒ b, b ⇒ a) = 1                       prelinearity
 * ```
 *
 * CLAUDE.md §7's third tier, alongside [ResiduumLaws]: *"Residuated / BL —
 * left-continuous T + residuum — residuation adjunction, divisibility,
 * prelinearity."* Run [ResiduumLaws] first: divisibility is a statement *about*
 * the residuum, so it is meaningless if the adjunction does not hold.
 *
 * ## What each law separates out
 *
 * **Divisibility** is equivalent to continuity (given left-continuity), and it
 * is what promotes a residuated lattice to a BL-algebra. It says the lattice
 * meet `min(a,b)` is *recoverable* from the monoid: whatever `b`-ness `a` has,
 * conjoining `a` with `a ⇒ b` extracts exactly it.
 *
 * This is the law that separates [TNorms.NILPOTENT_MINIMUM] from the continuous
 * basis. Nilpotent minimum is left-continuous, so it has a genuine residuum and
 * passes [ResiduumLaws] — but it is **not continuous**, so it fails divisibility
 * and is an MTL-algebra rather than a BL-algebra. `fuzzy-laws`' own tests assert
 * that split, because it is the cleanest available demonstration that
 * [ResiduumLaws] and this suite are genuinely different tiers and not two
 * spellings of one.
 *
 * **Prelinearity** is what makes the logic *linearly ordered* enough to be
 * complete with respect to `[0,1]`. Note it uses `max`, the **lattice join**,
 * not the algebra's t-conorm — in a BL-algebra the join is `max` regardless of
 * which `S` you happen to have bundled alongside. Passing a conorm here would be
 * a category error, which is why this suite takes a [TNorm] and nothing else.
 *
 * All three of [dk.eusrbin.fuzzy.algebra.Algebra.STANDARD],
 * [dk.eusrbin.fuzzy.algebra.Algebra.PRODUCT] and
 * [dk.eusrbin.fuzzy.algebra.Algebra.LUKASIEWICZ] are BL-algebras — they are the
 * three continuous basis norms (**Mostert–Shields**), and BL is exactly the
 * logic of continuous t-norms.
 */
public object BLAlgebraLaws {

    private const val CITATION = "Bergmann 2008, §12 (BL-algebras, residuated lattices)"

    /**
     * Checks [tNorm] and **throws** on any failure.
     *
     * @throws LawViolationException if divisibility or prelinearity fails.
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

    /** Checks [tNorm] and **returns** a [LawReport]. Never throws. */
    @JvmStatic
    @JvmOverloads
    public fun check(
        tNorm: TNorm,
        tolerance: Tolerance = Tolerance.forTNorm(tNorm),
        sampling: Sampling = Sampling.DEFAULT,
    ): LawReport {
        val checker = LawChecker("BLAlgebraLaws", tNorm.toString(), tolerance, sampling)

        checker.law2("divisibility: T(a, a⇒b) = min(a,b)", CITATION) { a, b ->
            checker.eq(
                tNorm.apply(a, tNorm.residuum(a, b)),
                min(a, b),
                "T(a, a⇒b)",
                "min(a,b)",
            )
        }

        // `max` is the lattice join, NOT the algebra's t-conorm — see the class
        // KDoc. A BL-algebra's join is max whatever S is bundled with it.
        checker.law2("prelinearity: max(a⇒b, b⇒a) = 1", CITATION) { a, b ->
            checker.eq(
                max(tNorm.residuum(a, b), tNorm.residuum(b, a)),
                1.0,
                "max(a⇒b, b⇒a)",
                "1",
            )
        }

        return checker.report()
    }
}
