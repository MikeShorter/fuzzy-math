package dk.eusrbin.fuzzy.laws

import dk.eusrbin.fuzzy.algebra.Algebra

/**
 * **The Zadeh/Gödel tier — these hold for `min`/`max` and for nothing else.**
 *
 * Sources: Zadeh 1965, §III — eqs. **(9)** and **(10)** (distributivity),
 * p.343 (the distributive lattice remark); Bergmann 2008, §11.8.
 *
 * ```
 * T(a,a) = a                                  idempotence (∩)
 * S(a,a) = a                                  idempotence (∪)
 * T(a, S(b,c)) = S(T(a,b), T(a,c))            Zadeh eq. (9)    distributivity
 * S(a, T(b,c)) = T(S(a,b), S(a,c))            Zadeh eq. (10)   distributivity
 * T(a, S(a,b)) = a                            absorption
 * S(a, T(a,b)) = a                            absorption
 * ```
 *
 * ## Why this tier exists — read this before using it
 *
 * Zadeh notes (1965, p.343) that fuzzy sets form a **distributive lattice**. It
 * is true, and it is a claim about `min`/`max` *specifically* — not about fuzzy
 * sets in general. CLAUDE.md §7 is built on the distinction:
 *
 * > *"Distributivity (his eqs. 9, 10) and idempotence **fail** for Product and
 * > Łukasiewicz. Min is the only idempotent t-norm. The laws therefore
 * > stratify."*
 *
 * If you take one thing from `fuzzy-laws`, take this: **a law you read in Zadeh
 * 1965 is not automatically a law of your algebra.** Distributivity is the one
 * people carry over by habit, because it is true in the classical set theory
 * everyone learned first and true in the fuzzy set theory of the founding paper.
 * It is false the moment you switch to the Product t-norm, and nothing warns
 * you — every result stays in `[0,1]` and looks fine.
 *
 * ## The failures, concretely
 *
 * - **Idempotence.** `min` is the *only* idempotent t-norm — that is a theorem,
 *   not an observation. Product: `0.5 · 0.5 = 0.25 ≠ 0.5`.
 * - **Distributivity, eq. (9)**, for Product/probabilistic-sum:
 *   `T(a, S(b,c)) = ab + ac − abc`, while
 *   `S(T(a,b), T(a,c)) = ab + ac − a²bc`. Equal iff `a = 1` or `bc = 0`.
 *   The gap is `abc(1 − a)`, which peaks around `a = 0.5` at a very
 *   comfortable `≈ 0.125` — no tolerance discussion required.
 * - **Absorption.** Product: `T(a, S(a,b)) = a² + ab − a²b ≠ a`.
 *
 * ## The test of the test
 *
 * CLAUDE.md §7 closes with a requirement this suite exists to meet:
 *
 * > *"`ZadehLaws` must **fail** for Product — provably, as a test of the test."*
 *
 * `fuzzy-laws`' own test suite asserts that `check(Algebra.PRODUCT)` reports
 * failures for idempotence, both distributivity laws and absorption — and that
 * `check(Algebra.STANDARD)` reports none, at [Tolerance.EXACT]. If
 * distributivity ever appears to hold for Product, this suite is broken and the
 * test says so.
 *
 * That is also why [check] exists next to [verify]: asserting a *failure*
 * requires an API that reports rather than throws.
 *
 * @see MVAlgebraLaws — the mirror image. Łukasiewicz keeps excluded middle and
 *   loses distributivity; Gödel keeps distributivity and loses excluded middle.
 */
public object StandardLaws {

    private const val IDEMPOTENCE_CITATION = "Zadeh 1965, §III p.343; Bergmann 2008, §11.8"
    private const val DISTRIBUTIVITY_CITATION = "Zadeh 1965, §III eqs. (9), (10)"
    private const val ABSORPTION_CITATION = "Zadeh 1965, §III p.343 (distributive lattice)"

    /**
     * Checks [algebra] and **throws** on any failure.
     *
     * Expected to pass for [Algebra.STANDARD] and to **fail** for
     * [Algebra.PRODUCT] and [Algebra.LUKASIEWICZ]. That is not a caveat, it is
     * the tier's content — see the class KDoc.
     *
     * @throws LawViolationException if any law fails.
     */
    @JvmStatic
    @JvmOverloads
    public fun verify(
        algebra: Algebra,
        tolerance: Tolerance = Tolerance.forAlgebra(algebra),
        sampling: Sampling = Sampling.DEFAULT,
    ) {
        check(algebra, tolerance, sampling).assertHolds()
    }

    /**
     * Checks [algebra] and **returns** a [LawReport]. Never throws.
     *
     * The method to use when a failure is the expected outcome — as in the
     * test-of-the-test against [Algebra.PRODUCT].
     */
    @JvmStatic
    @JvmOverloads
    public fun check(
        algebra: Algebra,
        tolerance: Tolerance = Tolerance.forAlgebra(algebra),
        sampling: Sampling = Sampling.DEFAULT,
    ): LawReport {
        val checker = LawChecker("StandardLaws", algebra.toString(), tolerance, sampling)

        checker.law1("idempotence: T(a,a) = a", IDEMPOTENCE_CITATION) { a ->
            checker.eq(algebra.and(a, a), a, "T(a,a)", "a")
        }

        checker.law1("idempotence: S(a,a) = a", IDEMPOTENCE_CITATION) { a ->
            checker.eq(algebra.or(a, a), a, "S(a,a)", "a")
        }

        checker.law3("distributivity: T(a,S(b,c)) = S(T(a,b),T(a,c))  [eq. 9]", DISTRIBUTIVITY_CITATION) { a, b, c ->
            checker.eq(
                algebra.and(a, algebra.or(b, c)),
                algebra.or(algebra.and(a, b), algebra.and(a, c)),
                "T(a,S(b,c))",
                "S(T(a,b),T(a,c))",
            )
        }

        checker.law3("distributivity: S(a,T(b,c)) = T(S(a,b),S(a,c))  [eq. 10]", DISTRIBUTIVITY_CITATION) { a, b, c ->
            checker.eq(
                algebra.or(a, algebra.and(b, c)),
                algebra.and(algebra.or(a, b), algebra.or(a, c)),
                "S(a,T(b,c))",
                "T(S(a,b),S(a,c))",
            )
        }

        checker.law2("absorption: T(a,S(a,b)) = a", ABSORPTION_CITATION) { a, b ->
            checker.eq(algebra.and(a, algebra.or(a, b)), a, "T(a,S(a,b))", "a")
        }

        checker.law2("absorption: S(a,T(a,b)) = a", ABSORPTION_CITATION) { a, b ->
            checker.eq(algebra.or(a, algebra.and(a, b)), a, "S(a,T(a,b))", "a")
        }

        return checker.report()
    }
}
