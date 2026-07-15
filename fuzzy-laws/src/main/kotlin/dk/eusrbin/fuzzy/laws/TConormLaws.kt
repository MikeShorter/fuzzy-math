package dk.eusrbin.fuzzy.laws

import dk.eusrbin.fuzzy.algebra.TConorm

/**
 * **The universal tier, dual side.** The defining laws of a triangular conorm.
 *
 * Source: Bergmann 2008, §11.7; Klement, Mesiar & Pap 2000.
 *
 * ```
 * S(a, b)       = S(b, a)                     commutativity
 * S(S(a,b), c)  = S(a, S(b,c))                associativity
 * b ≤ c         ⟹ S(a,b) ≤ S(a,c)             monotonicity
 * S(a, 0)       = a                           boundary (neutral element)
 * S(a, 1)       = 1                           annihilator
 * ```
 *
 * Identical to [TNormLaws] except that the neutral element is `0` rather than
 * `1`, and the annihilator `1` rather than `0`.
 *
 * ## Why this suite exists
 *
 * CLAUDE.md §7's tier table names six suites and this is not one of them. It is
 * here because CLAUDE.md §9 makes [TConorm] a **separate type** from
 * `TNorm` — so a consumer who writes their own conorm has, without this, no way
 * to check it, and `TNormLaws.verify` will not accept it. A published extension
 * mechanism (§7) with a type you can implement but cannot verify is an
 * incomplete one.
 *
 * The same reasoning that justifies the type justifies the suite. Flagged in the
 * hand-off notes as a small addition beyond the stated six.
 *
 * @see TNormLaws
 * @see DeMorganLaws for whether your conorm is the dual of your norm.
 */
public object TConormLaws {

    private const val CITATION = "Bergmann 2008, §11.7; Klement, Mesiar & Pap 2000"

    /**
     * Checks [tConorm] and **throws** [LawViolationException] on any failure.
     *
     * @param tolerance defaults to [Tolerance.DEFAULT]. Unlike [TNormLaws.verify]
     *   there is no identity-based calibration here: [Tolerance.forTNorm]
     *   dispatches on t-norm singletons, and a conorm is a different type. Pass
     *   [Tolerance.EXACT] explicitly when checking `max`.
     * @throws LawViolationException if any law fails, with the counterexample.
     */
    @JvmStatic
    @JvmOverloads
    public fun verify(
        tConorm: TConorm,
        tolerance: Tolerance = Tolerance.DEFAULT,
        sampling: Sampling = Sampling.DEFAULT,
    ) {
        check(tConorm, tolerance, sampling).assertHolds()
    }

    /** Checks [tConorm] and **returns** a [LawReport]. Never throws. */
    @JvmStatic
    @JvmOverloads
    public fun check(
        tConorm: TConorm,
        tolerance: Tolerance = Tolerance.DEFAULT,
        sampling: Sampling = Sampling.DEFAULT,
    ): LawReport {
        val checker = LawChecker("TConormLaws", tConorm.toString(), tolerance, sampling)

        checker.law2("commutativity", CITATION) { a, b ->
            checker.eq(tConorm.apply(a, b), tConorm.apply(b, a), "S(a,b)", "S(b,a)")
        }

        checker.law3("associativity", CITATION) { a, b, c ->
            checker.eq(
                tConorm.apply(tConorm.apply(a, b), c),
                tConorm.apply(a, tConorm.apply(b, c)),
                "S(S(a,b),c)",
                "S(a,S(b,c))",
            )
        }

        checker.law3("monotonicity", CITATION) { a, b, c ->
            if (b > c) {
                null
            } else {
                checker.leq(tConorm.apply(a, b), tConorm.apply(a, c), "S(a,b)", "S(a,c)")
            }
        }

        checker.law1("boundary: S(a,0) = a", CITATION) { a ->
            checker.eq(tConorm.apply(a, 0.0), a, "S(a,0)", "a")
        }

        checker.law1("annihilator: S(a,1) = 1", CITATION) { a ->
            checker.eq(tConorm.apply(a, 1.0), 1.0, "S(a,1)", "1")
        }

        return checker.report()
    }
}
