package dk.eusrbin.fuzzy.algebra

import kotlin.math.max
import kotlin.math.min

/**
 * The named t-norms, and the constructions that generate the rest.
 *
 * ## This is a basis, not a grab-bag
 *
 * By the **Mostert–Shields theorem**, every continuous t-norm is an ordinal sum
 * of copies of [LUKASIEWICZ], [GODEL] and [PRODUCT]. Those three plus
 * [ordinalSum] are therefore not three convenient examples and a utility — they
 * are a *generating set for the whole continuous class*. CLAUDE.md §6: "provide
 * those three plus an ordinal-sum construction — a generating basis, not a
 * grab-bag."
 *
 * Everything else here ([DRASTIC], [NILPOTENT_MINIMUM], [hamacher]) is either
 * outside the continuous class or a convenience within it, and is labelled as
 * such.
 *
 * Sources: Bergmann 2008 §11.7, §11.8, §11.9; Klement, Mesiar & Pap 2000.
 *
 * ## Residua are closed-form here
 *
 * Every t-norm in this object overrides [TNorm.residuum] with its closed form
 * rather than inheriting the generic bisection. CLAUDE.md §5.
 */
public object TNorms {

    /**
     * The **Gödel** t-norm, `T(a,b) = min(a,b)`: the greatest t-norm, and the
     * only idempotent one.
     *
     * Sources: Zadeh 1965, §III eq. (3) — his *intersection*, `f_{A∩B} =
     * Min[f_A, f_B]`; Bergmann 2008, §11.8.
     *
     * CLAUDE.md §6: *"Min/max are not a special mechanism — they are just the
     * Gödel t-norm. There is one parameterised mechanism, and Zadeh's algebra is
     * its default instantiation."*
     *
     * Its uniqueness matters more than its familiarity. **Min is the only
     * idempotent t-norm** — which is precisely why CLAUDE.md §7's law tiers
     * exist: Zadeh's remark (p.343) that fuzzy sets form a distributive lattice
     * is a claim about min/max *specifically*, and distributivity and idempotence
     * fail for [PRODUCT] and [LUKASIEWICZ]. See
     * [dk.eusrbin.fuzzy.laws.StandardLaws].
     *
     * Also the ordinal sum with no summands — see [ordinalSum].
     *
     * Residuum (Bergmann 2008, §11.8): `a ⇒ b = 1 if a ≤ b, else b`.
     */
    @JvmField
    public val GODEL: TNorm = object : TNorm {
        override fun apply(a: Double, b: Double): Double = min(a, b)

        override fun residuum(a: Double, b: Double): Double = if (a <= b) 1.0 else b

        override fun toString(): String = "TNorm.Godel(min)"
    }

    /** Alias for [GODEL], under the name Zadeh 1965 §III uses. Same instance. */
    @JvmField
    public val MINIMUM: TNorm = GODEL

    /**
     * The **Product** (Goguen) t-norm, `T(a,b) = a·b`. Strict and continuous.
     *
     * Sources: Zadeh 1965, §IV — his *algebraic product* `f_{AB} = f_A f_B`;
     * Bergmann 2008, §11.9.
     *
     * CLAUDE.md §6 notes what Zadeh did not: his "algebraic product" *is* the
     * Product t-norm, and footnote 4's dual `f_A + f_B − f_A f_B` *is* its
     * conorm ([TConorms.PROBABILISTIC_SUM]). §III and §IV of the 1965 paper are
     * one mechanism at two parameter values.
     *
     * Residuum (Goguen implication, Bergmann 2008 §11.9):
     * `a ⇒ b = 1 if a ≤ b, else b/a`. The division is safe: `a > b ≥ 0` forces
     * `a > 0`.
     */
    @JvmField
    public val PRODUCT: TNorm = object : TNorm {
        override fun apply(a: Double, b: Double): Double = a * b

        override fun residuum(a: Double, b: Double): Double = if (a <= b) 1.0 else b / a

        override fun toString(): String = "TNorm.Product"
    }

    /** Alias for [PRODUCT], under Goguen's name. Same instance. */
    @JvmField
    public val GOGUEN: TNorm = PRODUCT

    /**
     * The **Łukasiewicz** t-norm, `T(a,b) = max(0, a + b − 1)`. Continuous and
     * nilpotent.
     *
     * Source: Bergmann 2008, §11.2 and §12 (MV-algebras).
     *
     * CLAUDE.md §6 observes that Zadeh's §IV algebraic sum `f_A + f_B`, carrying
     * the side-condition `f_A + f_B ≤ 1`, "is an uncapped Łukasiewicz conorm
     * missing its `min(1, ·)`" — the side-condition is doing the capping by
     * hand. See [TConorms.LUKASIEWICZ].
     *
     * This is the t-norm of MV-algebras and the only one for which
     * [dk.eusrbin.fuzzy.laws.MVAlgebraLaws] holds (CLAUDE.md §7).
     *
     * Residuum (Bergmann 2008, §11.2): `a ⇒ b = min(1, 1 − a + b)`.
     */
    @JvmField
    public val LUKASIEWICZ: TNorm = object : TNorm {
        override fun apply(a: Double, b: Double): Double = max(0.0, a + b - 1.0)

        override fun residuum(a: Double, b: Double): Double = min(1.0, 1.0 - a + b)

        override fun toString(): String = "TNorm.Lukasiewicz"
    }

    /**
     * The **Drastic** product: the *least* t-norm.
     *
     * ```
     * T(a,b) = b     if a = 1
     *        = a     if b = 1
     *        = 0     otherwise
     * ```
     *
     * Source: Klement, Mesiar & Pap 2000. Provided "for convenience"
     * (CLAUDE.md §6) — it is not in the continuous basis and cannot be built
     * from it, being neither continuous nor left-continuous.
     *
     * ## It has no residuum, and that is the point
     *
     * The supremum `sup { z | T(a,z) ≤ b }` is perfectly computable here, and
     * [residuum] returns it. But the **adjunction fails**, because the supremum
     * is not attained. Take `a = 0.5`, `b = 0.3`:
     *
     * - `T(0.5, z) = 0 ≤ 0.3` for every `z < 1`, so the supremum is `1`.
     * - Yet `z = 1 ≤ 1 = (a ⇒ b)` while `T(0.5, 1) = 0.5 > 0.3`.
     *
     * The right-hand side of `T(a,z) ≤ b ⟺ z ≤ (a ⇒ b)` holds and the left does
     * not. So `ResiduumLaws.check(DRASTIC)` **reports a failure, and is correct
     * to** — left-continuity is a genuine precondition of CLAUDE.md §5's
     * "for a left-continuous t-norm the residuum is *determined*", not a
     * technicality. This t-norm is the cheapest way to see that.
     */
    @JvmField
    public val DRASTIC: TNorm = object : TNorm {
        override fun apply(a: Double, b: Double): Double = when {
            a == 1.0 -> b
            b == 1.0 -> a
            else -> 0.0
        }

        // sup { z | T(a,z) ≤ b }. For a < 1 the set is [0,1) — possibly plus {1}
        // when a ≤ b — and its supremum is 1 either way. For a = 1 the norm is
        // the identity in z, so the supremum is b.
        override fun residuum(a: Double, b: Double): Double = if (a == 1.0) b else 1.0

        override fun toString(): String = "TNorm.Drastic"
    }

    /**
     * The **nilpotent minimum**, `T(a,b) = min(a,b) if a + b > 1, else 0`.
     *
     * Source: Fodor (1995); catalogued in Klement, Mesiar & Pap 2000.
     * Provided for convenience (CLAUDE.md §6).
     *
     * Left-continuous but **not continuous**, which makes it a useful specimen:
     * it *does* have a genuine residuum (the adjunction holds — left-continuity
     * is all that is required), yet it is not divisible, so it satisfies
     * [dk.eusrbin.fuzzy.laws.ResiduumLaws] while failing
     * [dk.eusrbin.fuzzy.laws.BLAlgebraLaws]. It is an MTL-algebra but not a
     * BL-algebra. Being outside the continuous class, it is also not reachable
     * from the Mostert–Shields basis.
     *
     * Residuum: `a ⇒ b = 1 if a ≤ b, else max(1 − a, b)`.
     */
    @JvmField
    public val NILPOTENT_MINIMUM: TNorm = object : TNorm {
        override fun apply(a: Double, b: Double): Double = if (a + b > 1.0) min(a, b) else 0.0

        override fun residuum(a: Double, b: Double): Double = if (a <= b) 1.0 else max(1.0 - a, b)

        override fun toString(): String = "TNorm.NilpotentMinimum"
    }

    /**
     * The three fundamental continuous t-norms of the **Mostert–Shields**
     * theorem, in the order they are conventionally named.
     *
     * Together with [ordinalSum] these generate every continuous t-norm. Handy
     * for property tests that want to range over the basis.
     */
    @JvmField
    public val CONTINUOUS_BASIS: List<TNorm> = listOf(LUKASIEWICZ, GODEL, PRODUCT)

    /**
     * The **Hamacher** family, parameterised by `γ ≥ 0`:
     *
     * ```
     * T_γ(a,b) = 0                                  if γ = 0 and a = b = 0
     *          = a·b / (γ + (1 − γ)(a + b − a·b))    otherwise
     * ```
     *
     * Source: Hamacher (1978); catalogued in Klement, Mesiar & Pap 2000.
     * Provided for convenience (CLAUDE.md §6).
     *
     * Landmarks: `γ = 0` is the Hamacher product, `γ = 1` is [PRODUCT] exactly,
     * `γ = 2` is the Einstein product. Every member is strict and continuous,
     * hence left-continuous, hence genuinely residuated.
     *
     * Returns the [PRODUCT] singleton when `γ == 1.0`, so you inherit its
     * closed-form residuum instead of the bisection fallback.
     *
     * **Residuum:** members other than `γ = 1` inherit [TNorm]'s generic
     * bisection. Closed forms exist (the family is strict, so the residuum is
     * recoverable from the additive generator), but the derivation earns little
     * over ~60 multiplications and would need its own citation and its own
     * numerical error budget. The bisection is exact to adjacent doubles and
     * cannot silently disagree with [apply]. Revisit if profiling says so.
     *
     * @param gamma the parameter `γ`, which must be `≥ 0`.
     * @throws IllegalArgumentException if [gamma] is negative or `NaN`.
     */
    @JvmStatic
    public fun hamacher(gamma: Double): TNorm {
        require(gamma >= 0.0) { "Hamacher parameter γ must be ≥ 0, but was $gamma" }
        return if (gamma == 1.0) PRODUCT else HamacherTNorm(gamma)
    }

    /**
     * The **ordinal sum** of [summands]: the construction that, with
     * [CONTINUOUS_BASIS], generates every continuous t-norm (**Mostert–Shields**).
     *
     * ```
     * T(x,y) = a_i + (b_i − a_i)·T_i( (x−a_i)/(b_i−a_i), (y−a_i)/(b_i−a_i) )
     *              if x, y ∈ [a_i, b_i] for some summand i
     *        = min(x, y)
     *              otherwise
     * ```
     *
     * Source: Klement, Mesiar & Pap 2000 (ordinal sums); Bergmann 2008, §11.7.
     * CLAUDE.md §6 — first-class, not a footnote.
     *
     * Each summand's t-norm is rescaled into its own subinterval; everywhere
     * outside the summands, and anywhere the two arguments fall in *different*
     * summands, the sum behaves as `min`. The summands' **open** intervals must
     * be pairwise disjoint; shared endpoints are fine, since both formulas agree
     * with `min` there.
     *
     * Two consequences worth knowing:
     * - `ordinalSum()` with **no summands is exactly [GODEL]** — `min` is the
     *   empty ordinal sum. Returned as the [GODEL] singleton.
     * - `ordinalSum(new OrdinalSummand(0, 1, T))` is `T` itself, rescaled by the
     *   identity.
     *
     * The residuum is derived structurally from the summands' own residua
     * (CLAUDE.md §5) — see the implementation.
     *
     * @throws IllegalArgumentException if any two summands' open intervals overlap.
     */
    @JvmStatic
    public fun ordinalSum(summands: List<OrdinalSummand>): TNorm {
        if (summands.isEmpty()) return GODEL

        val sorted = summands.sortedBy { it.lower }
        for (i in 1 until sorted.size) {
            val previous = sorted[i - 1]
            val current = sorted[i]
            require(current.lower >= previous.upper) {
                "Ordinal sum summands must have pairwise disjoint open intervals, but " +
                    "[${previous.lower}, ${previous.upper}] overlaps " +
                    "[${current.lower}, ${current.upper}]"
            }
        }
        return OrdinalSumTNorm(sorted)
    }

    /** Vararg overload of [ordinalSum], for `TNorms.ordinalSum(s1, s2)`. */
    @JvmStatic
    public fun ordinalSum(vararg summands: OrdinalSummand): TNorm = ordinalSum(summands.asList())
}

/** @see TNorms.hamacher */
private class HamacherTNorm(private val gamma: Double) : TNorm {
    override fun apply(a: Double, b: Double): Double {
        // The only 0/0 in the family: γ = 0 with a = b = 0. Guarding
        // unconditionally is correct for every γ, since a·b = 0 there anyway.
        if (a == 0.0 && b == 0.0) return 0.0
        val probabilisticSum = a + b - a * b
        return (a * b) / (gamma + (1.0 - gamma) * probabilisticSum)
    }

    override fun toString(): String = "TNorm.Hamacher(γ=$gamma)"
}

/**
 * @see TNorms.ordinalSum
 * @param summands sorted by [OrdinalSummand.lower], with pairwise disjoint open
 *   intervals — both established by the factory.
 */
private class OrdinalSumTNorm(private val summands: List<OrdinalSummand>) : TNorm {

    override fun apply(a: Double, b: Double): Double {
        // Both arguments must land in the SAME summand; if they land in
        // different ones, or outside every one, the sum is min there.
        val s = summands.firstOrNull { a in it && b in it } ?: return min(a, b)
        return s.lower + s.width * s.tNorm.apply(rescale(a, s), rescale(b, s))
    }

    /**
     * The residuum of an ordinal sum, derived from the summands' own residua
     * (CLAUDE.md §5 — computed, never configured):
     *
     * ```
     * a ⇒ b = 1                                   if a ≤ b
     *       = a_i + (b_i − a_i)·(a' ⇒_i b')       if a_i ≤ b < a ≤ b_i
     *       = b                                   otherwise
     * ```
     *
     * The middle case is the interesting one: it fires only when the *whole*
     * relevant span `b < a` sits inside one summand, so the sum is behaving like
     * that summand's t-norm throughout, and the residuum rescales accordingly.
     * Otherwise the sum behaves like `min` on the relevant span and the Gödel
     * residuum `b` is correct.
     */
    override fun residuum(a: Double, b: Double): Double {
        if (a <= b) return 1.0
        // Known: b < a. Need a summand with lower ≤ b and a ≤ upper.
        val s = summands.firstOrNull { b >= it.lower && a <= it.upper } ?: return b
        return s.lower + s.width * s.tNorm.residuum(rescale(a, s), rescale(b, s))
    }

    private fun rescale(x: Double, s: OrdinalSummand): Double = (x - s.lower) / s.width

    override fun toString(): String = "TNorm.OrdinalSum($summands)"
}
