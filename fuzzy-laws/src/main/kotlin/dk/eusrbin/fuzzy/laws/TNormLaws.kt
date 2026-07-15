package dk.eusrbin.fuzzy.laws

import dk.eusrbin.fuzzy.algebra.TNorm

/**
 * **The universal tier.** The four defining laws of a triangular norm, plus the
 * annihilator they imply.
 *
 * Source: Bergmann 2008, §11.7; Klement, Mesiar & Pap 2000.
 *
 * ```
 * T(a, b)       = T(b, a)                     commutativity
 * T(T(a,b), c)  = T(a, T(b,c))                associativity
 * b ≤ c         ⟹ T(a,b) ≤ T(a,c)             monotonicity
 * T(a, 1)       = a                           boundary (neutral element)
 * T(a, 0)       = 0                           annihilator
 * ```
 *
 * CLAUDE.md §7's top tier: these hold for **any** t-norm — Gödel, Product,
 * Łukasiewicz, Drastic, Hamacher, ordinal sums, and whatever you wrote — and
 * nothing stronger does. If [verify] fails here, the thing you have is not a
 * t-norm, no qualifications.
 *
 * ## This artifact is the point
 *
 * CLAUDE.md §7: *"**publish the law suites as a consumable artifact —
 * `fuzzy-laws`.** Not internal tests. A user writing their own t-norm adds
 * `fuzzy-laws` in test scope and calls `TNormLaws.verify(myTNorm)`. This
 * operationalises 'the sources are the spec', makes the library trustworthy in a
 * way docs cannot, and ships the extension mechanism *with its correctness
 * criteria attached*."*
 *
 * ```java
 * // Java, JUnit
 * @Test void einsteinIsATNorm() {
 *     TNorm einstein = (a, b) -> (a * b) / (2 - (a + b - a * b));
 *     TNormLaws.verify(einstein);
 * }
 * ```
 * ```kotlin
 * TNormLaws.verify(myTNorm)                       // throws on failure
 * val report = TNormLaws.check(myTNorm)           // returns; never throws
 * ```
 *
 * @see TConormLaws for the dual suite.
 * @see ResiduumLaws for the adjunction, which needs left-continuity too.
 * @see StandardLaws for the laws that hold for min/max *only*.
 */
public object TNormLaws {

    private const val CITATION = "Bergmann 2008, §11.7; Klement, Mesiar & Pap 2000"

    /**
     * Checks [tNorm] and **throws** [LawViolationException] on any failure.
     *
     * The front door. `verify` throws rather than returning a report you might
     * forget to assert on — a law suite whose failure is silent unless you
     * remember an extra call is worse than no law suite.
     *
     * @param tolerance defaults to [Tolerance.forTNorm], which calibrates by
     *   identity against the built-ins and falls back to [Tolerance.GENERAL] for
     *   a t-norm it does not recognise (CLAUDE.md §8).
     * @throws LawViolationException if any law fails, with the counterexample.
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

    /**
     * Checks [tNorm] and **returns** a [LawReport]. Never throws.
     *
     * Use when you want to inspect which laws hold rather than fail on the first
     * that does not — including when a failure is the expected outcome, as in
     * `StandardLaws.check(Algebra.PRODUCT)` (CLAUDE.md §7's test-of-the-test).
     */
    @JvmStatic
    @JvmOverloads
    public fun check(
        tNorm: TNorm,
        tolerance: Tolerance = Tolerance.forTNorm(tNorm),
        sampling: Sampling = Sampling.DEFAULT,
    ): LawReport {
        val checker = LawChecker("TNormLaws", tNorm.toString(), tolerance, sampling)

        checker.law2("commutativity", CITATION) { a, b ->
            checker.eq(tNorm.apply(a, b), tNorm.apply(b, a), "T(a,b)", "T(b,a)")
        }

        checker.law3("associativity", CITATION) { a, b, c ->
            checker.eq(
                tNorm.apply(tNorm.apply(a, b), c),
                tNorm.apply(a, tNorm.apply(b, c)),
                "T(T(a,b),c)",
                "T(a,T(b,c))",
            )
        }

        // Stated as an implication, so the pool is used to generate the
        // hypothesis rather than to assert on every triple: only (b ≤ c) pairs
        // constrain anything.
        checker.law3("monotonicity", CITATION) { a, b, c ->
            if (b > c) {
                null
            } else {
                checker.leq(tNorm.apply(a, b), tNorm.apply(a, c), "T(a,b)", "T(a,c)")
            }
        }

        checker.law1("boundary: T(a,1) = a", CITATION) { a ->
            checker.eq(tNorm.apply(a, 1.0), a, "T(a,1)", "a")
        }

        // Implied by the boundary condition and monotonicity, and checked anyway:
        // it is the law a hand-written t-norm most often trips over, and deriving
        // it is not the same as observing it.
        checker.law1("annihilator: T(a,0) = 0", CITATION) { a ->
            checker.eq(tNorm.apply(a, 0.0), 0.0, "T(a,0)", "0")
        }

        return checker.report()
    }
}
