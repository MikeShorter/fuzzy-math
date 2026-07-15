package dk.eusrbin.fuzzy.laws

import dk.eusrbin.fuzzy.set.Domain
import dk.eusrbin.fuzzy.set.FuzzySets
import dk.eusrbin.fuzzy.set.MembershipFn

/**
 * **The decomposition tier.** `A = ⋃_α α·Γ_α` — a set is recoverable from its
 * α-cuts.
 *
 * **Source:** Zadeh 1965, §V eq. **(24)**, p.347, for `Γ_α = {x | f_A(x) ≥ α}`.
 * **Attributed:** the names "decomposition" and "representation theorem", which
 * are not in the paper (CLAUDE.md §18.2).
 *
 * The theorem needs no source beyond eq. (24), because it is two substitutions
 * from it:
 *
 * ```
 * sup { α | x ∈ Γ_α }  =  sup { α | f_A(x) ≥ α }  =  f_A(x)
 * ```
 *
 * ## The only 2a law that is not pointwise
 *
 * Which is why it earns its own suite while [ZadehSetLaws] admits to being a
 * lifting check. De Morgan over sets reduces to De Morgan over degrees; this does
 * not reduce to anything. `Γ_α` is a **fold over the domain**, so the round trip
 * genuinely exercises CLAUDE.md §3's seam — and it is the one law that would
 * catch a broken [dk.eusrbin.fuzzy.set.Domain] implementation rather than a
 * broken algebra.
 *
 * ## Levels are the whole game
 *
 * `sup { α | x ∈ Γ_α }` can return **no degree that is not among the levels cut
 * at**. Decompose `f` at `{0.5}` and reconstruction gives `0.5` wherever `f ≥
 * 0.5` and `0` elsewhere — a staircase, not `f`, and *correctly so*. The round
 * trip is exact iff every degree the set attains is a level, which is exactly
 * what `MembershipFn.decompose(over)` (no explicit levels) constructs.
 *
 * So this suite checks two different things, and the second is the interesting
 * one:
 *
 * - **round trip** at the attained levels — must be exact;
 * - **monotone recovery** at arbitrary levels — reconstruction must never
 *   *exceed* the original, at any level set. Under-recovery is the honest
 *   failure mode of a coarse level set; over-recovery would mean a cut contained
 *   an `x` it should not, which is a real bug.
 */
public object DecompositionLaws {

    private const val CITATION = "Zadeh 1965, §V eq. (24), p.347; representation theorem attributed"

    /**
     * Checks that [fn] survives a decomposition round trip over [over] and
     * **throws** on failure.
     *
     * @param tolerance defaults to [Tolerance.EXACT]. Deliberately: the round trip
     *   does no arithmetic. It compares degrees, selects a supremum among degrees
     *   the function itself produced, and hands back one of them — nothing is
     *   computed, so nothing rounds, exactly as `min`/`max` do not round (§8). If
     *   this ever needs an epsilon, something is wrong that an epsilon should not
     *   hide.
     */
    @JvmStatic
    @JvmOverloads
    public fun <X> verify(
        fn: MembershipFn<X>,
        over: Domain<X>,
        tolerance: Tolerance = Tolerance.EXACT,
    ) {
        check(fn, over, tolerance).assertHolds()
    }

    /** Checks the round trip and **returns** a [LawReport]. Never throws. */
    @JvmStatic
    @JvmOverloads
    public fun <X> check(
        fn: MembershipFn<X>,
        over: Domain<X>,
        tolerance: Tolerance = Tolerance.EXACT,
    ): LawReport {
        val checker = LawChecker("DecompositionLaws", "$fn over $over", tolerance, Sampling.DEFAULT)

        // At the attained levels: exact.
        val attained = fn.decompose(over)
        val rebuilt = FuzzySets.fromDecomposition(attained)
        checker.lawOverDomain("round trip: fromDecomposition(decompose(A)) = A", CITATION, over) { x ->
            val original = fn.apply(x)
            val recovered = rebuilt.apply(x)
            checker.eq(recovered, original, "recovered f_A(x)", "f_A(x)")?.let {
                Counterexample(doubleArrayOf(original, recovered), "x = $x — $it")
            }
        }

        // Γ_α must actually contain what it claims to.
        checker.law1("Γ_α = {x | f_A(x) ≥ α}: every member qualifies", CITATION) { alpha ->
            val cut = fn.alphaCut(over, alpha)
            val intruder = cut.firstOrNull { x -> fn.apply(x) < alpha }
            intruder?.let { "x = $it is in Γ_$alpha but f_A(x) = ${fn.apply(it)} < $alpha" }
        }

        // At arbitrary levels: never over-recovers. Under-recovery is a coarse
        // level set doing its job; over-recovery is a bug.
        val coarse = fn.decompose(over, doubleArrayOf(0.25, 0.5, 0.75, 1.0))
        val fromCoarse = FuzzySets.fromDecomposition(coarse)
        checker.lawOverDomain("coarse levels under-recover, never over-recover", CITATION, over) { x ->
            val original = fn.apply(x)
            val recovered = fromCoarse.apply(x)
            checker.leq(recovered, original, "recovered f_A(x) at coarse levels", "f_A(x)")?.let {
                Counterexample(doubleArrayOf(original, recovered), "x = $x — $it")
            }
        }

        return checker.report()
    }
}
