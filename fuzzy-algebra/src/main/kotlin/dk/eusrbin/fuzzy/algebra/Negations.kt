package dk.eusrbin.fuzzy.algebra

import kotlin.math.pow

/**
 * The named fuzzy negations.
 *
 * Sources: Zadeh 1965, §II (p.340) for [STANDARD]; Klement, Mesiar & Pap 2000
 * and Bergmann 2008 §11.7 for the parametric families. CLAUDE.md §6:
 * "Sugeno and Yager negation families likewise" (i.e. provided for convenience,
 * alongside the named t-norms).
 *
 * Every negation here is **strong** (involutive): `N(N(a)) = a`. See [Negation]
 * for why that is load-bearing rather than decorative.
 *
 * Note the two families overlap at [STANDARD]: `sugeno(0) = yager(1) = 1 − x`.
 * They are different one-parameter curves through the same point.
 */
public object Negations {

    /**
     * The **standard** negation, `N(a) = 1 − a`.
     *
     * Source: Zadeh 1965, §II (p.340) — the complement of `A` is
     * `f_{A'}(x) = 1 − f_A(x)`. This is the only negation Zadeh 1965 defines.
     *
     * Strong, and the negation of all three named [Algebra] bundles
     * (CLAUDE.md §6). Equals `sugeno(0.0)` and `yager(1.0)`.
     */
    @JvmField
    public val STANDARD: Negation = object : Negation {
        override fun apply(a: Double): Double = 1.0 - a

        override fun toString(): String = "Negation.Standard(1−x)"
    }

    /** Alias for [STANDARD], under the name Zadeh 1965 §II uses. Same instance. */
    @JvmField
    public val ZADEH: Negation = STANDARD

    /**
     * The **Sugeno** family, parameterised by `λ > −1`:
     *
     * ```
     * N_λ(a) = (1 − a) / (1 + λ·a)
     * ```
     *
     * Source: Sugeno (1977); catalogued in Klement, Mesiar & Pap 2000.
     *
     * Strong for every admissible `λ`. `λ = 0` is [STANDARD] exactly (and the
     * singleton is returned for it). Increasing `λ` bends the curve below the
     * diagonal, decreasing it bends above.
     *
     * The constraint `λ > −1` is what keeps the denominator positive on `[0,1]`:
     * at `λ = −1` it vanishes at `a = 1`, and below that it changes sign inside
     * the interval, so the "negation" would leave `[0,1]`. Checked eagerly.
     *
     * @param lambda the parameter `λ`, which must be `> −1`.
     * @throws IllegalArgumentException if [lambda] is `≤ −1` or `NaN`.
     */
    @JvmStatic
    public fun sugeno(lambda: Double): Negation {
        require(lambda > -1.0) {
            "Sugeno parameter λ must be > −1 (the denominator 1 + λ·a must stay " +
                "positive on [0,1]), but was $lambda"
        }
        return if (lambda == 0.0) STANDARD else SugenoNegation(lambda)
    }

    /**
     * The **Yager** family, parameterised by `w > 0`:
     *
     * ```
     * N_w(a) = (1 − a^w)^(1/w)
     * ```
     *
     * Source: Yager (1980); catalogued in Klement, Mesiar & Pap 2000.
     *
     * Strong for every admissible `w`. `w = 1` is [STANDARD] exactly (and the
     * singleton is returned for it). As `w → ∞` the curve approaches the
     * "crisp-ish" negation that maps everything below 1 to 1.
     *
     * @param w the parameter `w`, which must be `> 0`.
     * @throws IllegalArgumentException if [w] is `≤ 0` or `NaN`.
     */
    @JvmStatic
    public fun yager(w: Double): Negation {
        require(w > 0.0) { "Yager parameter w must be > 0, but was $w" }
        return if (w == 1.0) STANDARD else YagerNegation(w)
    }
}

/** @see Negations.sugeno */
private class SugenoNegation(private val lambda: Double) : Negation {
    override fun apply(a: Double): Double = (1.0 - a) / (1.0 + lambda * a)

    override fun toString(): String = "Negation.Sugeno(λ=$lambda)"
}

/** @see Negations.yager */
private class YagerNegation(private val w: Double) : Negation {
    override fun apply(a: Double): Double = (1.0 - a.pow(w)).pow(1.0 / w)

    override fun toString(): String = "Negation.Yager(w=$w)"
}
