package dk.eusrbin.fuzzy.laws

import dk.eusrbin.fuzzy.algebra.Algebra
import dk.eusrbin.fuzzy.algebra.Negation
import dk.eusrbin.fuzzy.algebra.TConorm
import dk.eusrbin.fuzzy.algebra.TNorm

/**
 * **The De Morgan tier.** Whether `(T, S, N)` is a dual triple.
 *
 * Sources: Zadeh 1965, §III eqs. **(7)** and **(8)** — De Morgan's laws for
 * fuzzy sets; Bergmann 2008, §11.7.
 *
 * ```
 * N(S(a,b)) = T(N(a), N(b))        Zadeh eq. (7)   complement of union
 * N(T(a,b)) = S(N(a), N(b))        Zadeh eq. (8)   complement of intersection
 * N(N(a))   = a                    involutivity — a precondition, checked
 * ```
 *
 * CLAUDE.md §7's second tier: holds for a *dual* `(T, S, N)` triple, not for an
 * arbitrary one.
 *
 * ## What this suite is for
 *
 * This is the suite that catches CLAUDE.md §6's named footgun: *"Mixing an
 * arbitrary t-norm with an arbitrary conorm silently breaks De Morgan."*
 * Silently is the operative word — a mismatched pair returns numbers in `[0,1]`
 * that look entirely reasonable. Nothing throws. This suite is the only thing
 * that tells you.
 *
 * You do not need it for [Algebra.STANDARD], [Algebra.PRODUCT] or
 * [Algebra.LUKASIEWICZ] (correct by construction), nor for anything from
 * [Algebra.deMorgan] or `TConorms.dualOf` (correct by derivation, *provided* `N`
 * is involutive). You need it for [Algebra.of] — the escape hatch — and for any
 * triple you assembled by hand from the literature.
 *
 * ## Involutivity is checked first, and deliberately
 *
 * `N(N(a)) = a` is not one of Zadeh's De Morgan equations; it is the
 * precondition under which they can hold at all. Derivation of a dual conorm
 * (`S(a,b) = N(T(N(a), N(b)))`) round-trips only for involutive `N`. A
 * non-involutive negation is a perfectly legitimate [Negation] — it just cannot
 * be half of a De Morgan triple, and this suite reports that as its own failed
 * law rather than letting it surface as two mystifying equation failures.
 */
public object DeMorganLaws {

    private const val ZADEH_CITATION = "Zadeh 1965, §III eqs. (7), (8)"
    private const val INVOLUTION_CITATION = "Bergmann 2008, §11.7 (strong negation)"

    /**
     * Checks the triple `(T, S, N)` and **throws** on any failure.
     *
     * @throws LawViolationException if any law fails, with the counterexample.
     */
    @JvmStatic
    @JvmOverloads
    public fun verify(
        tNorm: TNorm,
        tConorm: TConorm,
        negation: Negation,
        tolerance: Tolerance = Tolerance.forTNorm(tNorm),
        sampling: Sampling = Sampling.DEFAULT,
    ) {
        check(tNorm, tConorm, negation, tolerance, sampling).assertHolds()
    }

    /**
     * Checks [algebra]'s own `(T, S, N)` and **throws** on any failure.
     *
     * The overload to reach for: an [Algebra] carries exactly the triple this
     * suite is about, and its [Tolerance] calibration is known
     * ([Tolerance.forAlgebra]).
     *
     * @throws LawViolationException if any law fails, with the counterexample.
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

    /** Checks the triple `(T, S, N)` and **returns** a [LawReport]. Never throws. */
    @JvmStatic
    @JvmOverloads
    public fun check(
        tNorm: TNorm,
        tConorm: TConorm,
        negation: Negation,
        tolerance: Tolerance = Tolerance.forTNorm(tNorm),
        sampling: Sampling = Sampling.DEFAULT,
    ): LawReport {
        val subject = "($tNorm, $tConorm, $negation)"
        val checker = LawChecker("DeMorganLaws", subject, tolerance, sampling)

        checker.law1("involutivity: N(N(a)) = a", INVOLUTION_CITATION) { a ->
            checker.eq(negation.apply(negation.apply(a)), a, "N(N(a))", "a")
        }

        checker.law2("De Morgan: N(S(a,b)) = T(N(a),N(b))  [eq. 7]", ZADEH_CITATION) { a, b ->
            checker.eq(
                negation.apply(tConorm.apply(a, b)),
                tNorm.apply(negation.apply(a), negation.apply(b)),
                "N(S(a,b))",
                "T(N(a),N(b))",
            )
        }

        checker.law2("De Morgan: N(T(a,b)) = S(N(a),N(b))  [eq. 8]", ZADEH_CITATION) { a, b ->
            checker.eq(
                negation.apply(tNorm.apply(a, b)),
                tConorm.apply(negation.apply(a), negation.apply(b)),
                "N(T(a,b))",
                "S(N(a),N(b))",
            )
        }

        return checker.report()
    }

    /** Checks [algebra]'s own `(T, S, N)` and **returns** a [LawReport]. Never throws. */
    @JvmStatic
    @JvmOverloads
    public fun check(
        algebra: Algebra,
        tolerance: Tolerance = Tolerance.forAlgebra(algebra),
        sampling: Sampling = Sampling.DEFAULT,
    ): LawReport {
        val report = check(algebra.tNorm, algebra.tConorm, algebra.negation, tolerance, sampling)
        // Re-label: the subject is the algebra, not the anonymous triple.
        return LawReport(report.suite, algebra.toString(), report.tolerance, report.results)
    }
}
