package dk.eusrbin.fuzzy.number

/**
 * A fuzzy number defined **by its α-cut map** — the general representation, and
 * what arithmetic returns.
 *
 * **Attributed:** the representation is implicit in Dubois & Prade's α-cut
 * arithmetic; **not on hand** (§20.6). What *is* verified is the inversion:
 * `f(x) = sup{α | x ∈ Γ_α}` derives from Zadeh eq. **(24)** by two substitutions
 * (§18.2), which is why this type can compute a membership function it was never
 * given.
 *
 * ## Its whole service: cuts in, membership out
 *
 * `T(1,2,3) × T(1,2,3)` is **not a triangle** — its α-cuts `[(1+α)², (3−α)²]` are
 * quadratic where a triangle's are linear (§20.3). But it *is* a fuzzy number, so
 * it is exactly representable by its cut map, and this type recovers the
 * membership function by inverting it.
 *
 * That is why implementing [FuzzyNumber] by hand is not a substitute: there you
 * must supply **both** `applyAsDouble` and `alphaCutInterval` and keep them
 * consistent. Here you supply one and the other is derived — correctly, by
 * construction.
 *
 * ## The precondition this type cannot check — CLAUDE.md §20.2(ii)
 *
 * **[cuts] must be nested**: `α₁ < α₂ ⟹ Γ_{α₂} ⊆ Γ_{α₁}`. [applyAsDouble]'s
 * bisection **requires that monotonicity**; hand it non-nested cuts and it returns
 * a plausible number computed from a meaningless search.
 *
 * **It is not checked, and cannot be** — nestedness quantifies over uncountably
 * many α, so it can only be *sampled*. The precedent is exact and already shipped
 * — `TConorms.dualOf` requires an involutive negation and says:
 *
 * > *"it is a property of a function, not of a value, so it cannot be checked at
 * > construction, only sampled. That is exactly what `DeMorganLaws` is for."*
 *
 * Same sentence, different property. §4's "construction validates" governs
 * **values** (a `σ`, an `Interval`'s endpoints); a function's behaviour over an
 * uncountable domain was never in its scope. `FuzzyNumberLaws` samples it and
 * reports a `Verdict` — refutable in one direction, since a witness
 * `(α₁ < α₂, x ∈ Γ_{α₂} \ Γ_{α₁})` disproves nestedness **absolutely** (§16.4).
 * That makes it convexity-shaped, not §19.6-boundedness-shaped, which is why it
 * gets a law at all.
 *
 * **Anything the arithmetic produces is safe by construction.** Nguyen's theorem
 * — the one §20.6 marks not-on-hand — is precisely what says α-cut arithmetic
 * preserves nestedness. The exposure is confined to hand-built instances, which is
 * why the constructor is public rather than the risk being designed away.
 *
 * @property cuts `α ↦ Γ_α`, for `α ∈ (0,1]`. **A function**, not a collection
 *   (§20.2(i)) — α is continuous, and there is no map over uncountably many
 *   levels. A `Map<Double, Interval>` here would quietly reintroduce the sampling
 *   §20.3 exists to avoid.
 */
public class AlphaCutNumber private constructor(
    private val cuts: (Double) -> Interval,
    private val label: String,
) : FuzzyNumber {

    override fun alphaCutInterval(alpha: Double): Interval {
        FuzzyNumber.requireCutLevel(alpha)
        return cuts(alpha)
    }

    /**
     * `f(x) = sup{α | x ∈ Γ_α}` — recovered by bisection.
     *
     * **Source:** derives from Zadeh eq. (24) (§18.2). The predicate `x ∈ Γ_α` is
     * monotone **decreasing** in α when the cuts are nested, so its supremum is the
     * boundary between the α that hold and those that do not — exactly the shape
     * `TNorm.residuum`'s generic bisection exploits, and the same pattern §15.3
     * pointed at.
     *
     * Costs ~50 evaluations of [cuts] per call. That is §20.3's honest trade: the
     * exact product of two fuzzy numbers costs a bisection where an approximating
     * triangle costs an arithmetic expression and is wrong by 0.25 through the
     * interior.
     *
     * **Resolution floor.** α is searched down to [MIN_LEVEL], not to zero —
     * `Γ_0` is all of ℝ for every fuzzy set and carries no information (Zadeh
     * restricts α to `(0,1]` for this reason). An `x` outside `Γ_{MIN_LEVEL}` is
     * reported as `0.0`, which is accurate to within [MIN_LEVEL].
     */
    override fun applyAsDouble(x: Double): Double {
        if (x !in cuts(MIN_LEVEL)) return 0.0 // below the resolution floor
        if (x in cuts(1.0)) return 1.0 // in the core
        var lo = MIN_LEVEL
        var hi = 1.0
        var steps = 0
        while (steps < BISECTION_STEPS) {
            val mid = 0.5 * (lo + hi)
            if (mid <= lo || mid >= hi) break // adjacent doubles
            if (x in cuts(mid)) lo = mid else hi = mid
            steps++
        }
        return lo
    }

    /**
     * `Sup_{x ∈ [lo, hi]} f(x) = sup{α | Γ_α ∩ [lo, hi] ≠ ∅}` — §20.8's hook,
     * by the same bisection.
     *
     * Overlap is monotone decreasing in α exactly as membership is, so this needs
     * no grid either. Not `O(1)` like the closed-form numbers — but it reads the
     * carrier, which is what §20.8 requires, and beats folding a domain the caller
     * chose.
     */
    override fun supremumOver(lo: Double, hi: Double): Double {
        fun overlaps(alpha: Double): Boolean {
            val cut = cuts(alpha)
            return cut.lower <= hi && cut.upper >= lo
        }
        if (!overlaps(MIN_LEVEL)) return 0.0
        if (overlaps(1.0)) return 1.0
        var low = MIN_LEVEL
        var high = 1.0
        var steps = 0
        while (steps < BISECTION_STEPS) {
            val mid = 0.5 * (low + high)
            if (mid <= low || mid >= high) break
            if (overlaps(mid)) low = mid else high = mid
            steps++
        }
        return low
    }

    override fun toString(): String = label

    public companion object {

        /**
         * The smallest α the bisection searches.
         *
         * Not zero: `Γ_0` is all of ℝ for every fuzzy set, so it is both useless
         * and outside [FuzzyNumber.alphaCutInterval]'s contract. `1e-12` puts the
         * floor far below any degree anyone reports, and bounds the error in
         * [applyAsDouble] by the same amount.
         */
        public const val MIN_LEVEL: Double = 1.0e-12

        /** Bisection steps. Past the point where the bracket reaches adjacent doubles. */
        private const val BISECTION_STEPS: Int = 64

        /**
         * A fuzzy number with the given α-cut map.
         *
         * **[cuts] must be nested** — unchecked, and the class KDoc says why at
         * length. `FuzzyNumberLaws` samples it.
         *
         * @param label what `toString` reports; law reports name their subject, so
         *   a lambda stringifying as `AlphaCutNumber$$Lambda$14/0x…` is worthless
         *   in one. Same reasoning as `TConorms`' private `named`.
         */
        @JvmStatic
        @JvmOverloads
        public fun of(label: String = "AlphaCutNumber", cuts: (Double) -> Interval): AlphaCutNumber =
            AlphaCutNumber(cuts, label)
    }
}
