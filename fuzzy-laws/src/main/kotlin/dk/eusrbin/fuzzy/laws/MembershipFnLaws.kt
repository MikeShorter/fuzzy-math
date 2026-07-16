package dk.eusrbin.fuzzy.laws

import dk.eusrbin.fuzzy.algebra.Degrees
import dk.eusrbin.fuzzy.set.Domain
import dk.eusrbin.fuzzy.set.MembershipFn

/**
 * **The membership-function tier.** What every fuzzy set must satisfy — and,
 * more usefully, **the correctness criterion for §15.3's overrides**.
 *
 * **Source:** Zadeh 1965, §II, p.339 — a membership function *"associates with
 * each point in X a real number in the interval `[0,1]`"*. That is the only law
 * a membership function has, and on its own it would make a thin suite.
 *
 * ## What this is actually for
 *
 * CLAUDE.md §15.3 decides that closed forms are **overrides on the function**:
 *
 * > *"`height(over:)` folds by default, and a `TriangularNumber` overrides it
 * > with the analytic answer and ignores the domain entirely."*
 *
 * That is a powerful hook and an unguarded one. An override that disagrees with
 * the fold it replaces is a silent wrong answer — `height` returns a plausible
 * degree, `alphaCut` returns a plausible list, and nothing complains. §7 exists
 * for exactly this shape of problem:
 *
 * > *"ships the extension mechanism **with its correctness criteria attached**"*
 *
 * So [verify] checks each analysis operation against **the generic
 * implementation it overrode**, recovered by wrapping the function in a plain
 * lambda:
 *
 * ```kotlin
 * val generic = MembershipFn<X> { x -> subject.apply(x) }   // same f, default bodies
 * subject.height(over) == generic.height(over)              // must agree
 * ```
 *
 * This is `KnownValuesTest`'s "the generic bisection agrees with every closed
 * form" from slice 1, generalised and published. A `MembershipFn` that overrides
 * nothing passes trivially — as it should, since then there is nothing to be
 * wrong.
 *
 * ```java
 * // A triangular number that knows its own height analytically.
 * @Test void triangleOverridesAreSound() {
 *     MembershipFnLaws.verify(myTriangle, Sampled.of(-1, 3, 4096));
 * }
 * ```
 *
 * ## What a report here does *not* tell you
 *
 * Whether the domain was exhaustive. Over a [dk.eusrbin.fuzzy.set.Sampled] grid
 * "no counterexample" means the grid found none — and a closed form that is right
 * on 1024 points may be wrong between them. That asymmetry is
 * [dk.eusrbin.fuzzy.set.Verdict]'s to carry (§16.4), not [LawReport]'s; the
 * domain is named in the report's subject so at least it is on the page.
 */
public object MembershipFnLaws {

    private const val RANGE_CITATION = "Zadeh 1965, §II p.339 (f_A: X → [0,1])"
    private const val OVERRIDE_CITATION = "CLAUDE.md §15.3 (closed forms are overrides); §7"

    /**
     * Checks [fn] over [over] and **throws** [LawViolationException] on failure.
     *
     * @param tolerance defaults to [Tolerance.GENERAL] — nothing is known about a
     *   membership function a consumer wrote, and an analytic `height` that
     *   agrees with the fold to 1e-13 is agreeing (CLAUDE.md §8).
     */
    @JvmStatic
    @JvmOverloads
    public fun <X> verify(
        fn: MembershipFn<X>,
        over: Domain<X>,
        tolerance: Tolerance = Tolerance.GENERAL,
    ) {
        check(fn, over, tolerance).assertHolds()
    }

    /** Checks [fn] over [over] and **returns** a [LawReport]. Never throws. */
    @JvmStatic
    @JvmOverloads
    public fun <X> check(
        fn: MembershipFn<X>,
        over: Domain<X>,
        tolerance: Tolerance = Tolerance.GENERAL,
    ): LawReport {
        val checker = LawChecker("MembershipFnLaws", "$fn over $over", tolerance, Sampling.DEFAULT)

        // The same f, with every default body intact. A lambda cannot carry
        // overrides, so this is the un-overridden twin of `fn` — the thing each
        // closed form claims to agree with.
        val generic = MembershipFn<X> { x -> fn.apply(x) }

        checker.lawOverDomain("range: f_A(x) ∈ [0,1]", RANGE_CITATION, over) { x ->
            val degree = fn.apply(x)
            if (Degrees.isDegree(degree)) {
                null
            } else {
                Counterexample(doubleArrayOf(degree), "x = $x — f_A(x) = $degree is not a degree in [0,1]")
            }
        }

        // CLAUDE.md §20.7 — NOT equality, and the asymmetry is the whole point.
        //
        // `height`'s own KDoc says the sampled answer is "a lower bound on the true
        // supremum". So the fold FOUND an x achieving 0.5: the true Sup is at least
        // 0.5, and THAT half is absolute. An override claiming 0.3 is lying and is
        // caught here. An override claiming 1.0 may simply know more — a closed form
        // beats a grid that steps over the peak, which is §15.3's entire promise.
        //
        // Asserting equality would reject §15.3's own worked example:
        // TriangularNumber(-0.5, 0.5, 1.5) over a grid of {0.0, 1.0} folds to 0.5
        // and analytically IS 1.0.
        checker.law1("override: height ≥ the fold", OVERRIDE_CITATION) { _ ->
            checker.leq(generic.height(over), fn.height(over), "the generic fold", "height(over)")
        }

        // ... and tightened where the domain can promise more (§20.7). Over an
        // Enumerable the fold visits EVERY element, so it IS the Sup over that
        // domain — not a lower bound on anything — and equality is sound. This is
        // the half with teeth: it catches an override that ignores a
        // question-defining domain (§20.8), which `≥` alone cannot.
        checker.law1("override: height == the fold, when the domain is exhaustive", OVERRIDE_CITATION) { _ ->
            if (!over.isExhaustive) {
                null
            } else {
                checker.eq(fn.height(over), generic.height(over), "height(over)", "the generic fold")
            }
        }

        // NO sigmaCount check. §20.1(b)/(c): `Σ over this domain` and `∫f dx` are
        // two questions, so nothing overrides sigmaCount — by decision, not by
        // omission. A check would be vacuous, and a vacuous law is worse than none:
        // it reads as coverage.

        checker.law1("override: support agrees with the fold", OVERRIDE_CITATION) { _ ->
            sameElements(fn.support(over), generic.support(over), "support(over)")
        }

        checker.law1("override: core agrees with the fold", OVERRIDE_CITATION) { _ ->
            sameElements(fn.core(over), generic.core(over), "core(over)")
        }

        // CLAUDE.md §20.7's guard PROPAGATES, and this is the member it reaches.
        //
        // `support`, `core` and `alphaCut` are filters on `f` alone, so they are
        // untouched by a `height` override and equality is sound for them over any
        // domain. `maximalGradeSet` is not: its default body is
        //
        //     over.filter { apply(x) >= height(over) }        <- the VIRTUAL height
        //
        // so overriding `height` silently changes it. Nobody overrides
        // maximalGradeSet (§20.1(b) forbids it — the return type cannot hold an
        // interval), and it disagrees with the fold anyway.
        //
        // Over a Sampled window with an off-grid peak the disagreement is CORRECT
        // and already ratified: §18.3 says "the true supremum may be approached
        // between grid points and attained nowhere on the grid, which is Sampled's
        // standing caveat". T(-1, 0.5, 2) over 512 points has an analytic height of
        // 1.0 that no grid point attains, so maximalGradeSet is legitimately empty
        // while the fold's is {0.499...}.
        //
        // So it inherits height's asymmetry exactly, and gets height's guard.
        checker.law1("override: maximalGradeSet agrees with the fold, when the domain is exhaustive", OVERRIDE_CITATION) { _ ->
            if (!over.isExhaustive) {
                null
            } else {
                sameElements(fn.maximalGradeSet(over), generic.maximalGradeSet(over), "maximalGradeSet(over)")
            }
        }

        // α is the thing that varies here, so the degree pool is exactly the
        // right sampler for once — it IS a pool of α values in [0,1].
        checker.law1("override: alphaCut agrees with the fold, for every α", OVERRIDE_CITATION) { alpha ->
            sameElements(fn.alphaCut(over, alpha), generic.alphaCut(over, alpha), "alphaCut(over, $alpha)")
        }

        checker.law1("override: strongAlphaCut agrees with the fold, for every α", OVERRIDE_CITATION) { alpha ->
            sameElements(
                fn.strongAlphaCut(over, alpha),
                generic.strongAlphaCut(over, alpha),
                "strongAlphaCut(over, $alpha)",
            )
        }

        // Not an override check: a consequence of the two definitions, and the
        // cheapest way to catch a core/maximalGradeSet mix-up (§18.3).
        checker.law1("core ⊆ maximalGradeSet, with equality iff normal", OVERRIDE_CITATION) { _ ->
            val core = fn.core(over)
            val maximal = fn.maximalGradeSet(over).toSet()
            when {
                !maximal.containsAll(core) ->
                    "core(over) = $core is not contained in maximalGradeSet(over) = $maximal"
                fn.isNormal(over) && fn.maximalGradeSet(over).toSet() != core.toSet() ->
                    "isNormal(over) is true, so core and maximalGradeSet must be the same set, " +
                        "but core = ${core.toSet()} and maximalGradeSet = $maximal"
                else -> null
            }
        }

        return checker.report()
    }

    /** `null` if the two element lists agree as sets; otherwise an explanation. */
    private fun <X> sameElements(actual: List<X>, expected: List<X>, what: String): String? {
        val a = actual.toSet()
        val e = expected.toSet()
        if (a == e) return null
        val missing = e - a
        val extra = a - e
        return "$what disagrees with the generic fold — missing $missing, unexpected $extra"
    }
}
