package dk.eusrbin.fuzzy.algebra

import kotlin.math.expm1
import kotlin.math.ln
import kotlin.math.ln1p
import kotlin.math.pow

/**
 * The named fuzzy negations.
 *
 * **Source:** Zadeh 1965, §II eq. (1), p.340 for [STANDARD]; Bergmann 2008 §11.7
 * for the notion of a negation — both read and checked.
 * **Attributed:** Sugeno (1977) and Yager (1980) for the parametric families;
 * neither is on hand (CLAUDE.md §17.5). CLAUDE.md §6:
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
     * **Attributed:** Sugeno (1977). Not on hand — CLAUDE.md §17.5.
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
     * **Attributed:** Yager (1980). Not on hand — CLAUDE.md §17.5.
     *
     * Strong for every admissible `w`. `w = 1` is [STANDARD] exactly (and the
     * singleton is returned for it). As `w → ∞` the curve approaches the
     * "crisp-ish" negation that maps everything below 1 to 1.
     *
     * ## Known limitation: **not involutive in `double`** for `w ≠ 1`
     *
     * Mathematically `N_w(N_w(a)) = a` for every admissible `w`. In IEEE 754 it
     * does not, and **no implementation can fix it** — the information is
     * destroyed by the representation, not by the arithmetic. Read this before
     * building anything on Yager that depends on involutivity.
     *
     * For small `a`, `N_w(a) ≈ 1 − a^w/w`, so `N_w(a)` sits near 1 and only the
     * `a^w/w` part carries information about `a`. Near 1 a double resolves to
     * `ulp(1) = 2.2e-16` — so once `a^w` approaches that, `a` is gone. The
     * round-trip error is approximately
     *
     * ```
     * |N_w(N_w(a)) − a|  ≈  5.55e-17 · a^(1−w)
     * ```
     *
     * For `w > 1` that is **unbounded as `a → 0`**, and it is a *gradient, not a
     * cliff at the edge* — there is no "safe interior" to retreat to, only
     * places where the error happens to be small. Measured, at `w = 4.25`:
     *
     * | `a` | 0.5 | 0.2 | 0.032 | 0.01 | 0.001 |
     * |---|---|---|---|---|---|
     * | error | 5.6e-17 | 1.1e-14 | 2.1e-12 | 1.0e-10 | **7.6e-8** |
     *
     * `w = 2` is far gentler (3e-14 at `a = 0.001`); the damage grows quickly
     * with `w`. For `w < 1` the same thing happens as `a → 1`.
     *
     * Consequences, all honest rather than fixable:
     * - **A De Morgan triple built from a Yager negation will fail**
     *   [dk.eusrbin.fuzzy.laws.DeMorganLaws]. De Morgan over a dual triple
     *   reduces to involutivity, so the suite is reporting the negation, not the
     *   law, and it is right to. There is no tolerance that rescues this and no
     *   sampling that dodges it — treat a Yager triple as numerically
     *   unverifiable, or use a `w` near 1 and know why.
     * - [Negations.sugeno] does **not** share this. Its curve meets the boundary
     *   *linearly* rather than flattening, so `N(a)` never collapses into the
     *   last few ulps and its triples verify cleanly. If you need an involutive
     *   parametric negation, that is the one.
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

/**
 * @see Negations.yager
 *
 * Not evaluated as the textbook writes it. `(1 − a^w)^(1/w)` is badly
 * conditioned near `a = 1`:
 *
 * - for `a = 1 − 2.2e-16` and `w ≈ 2.48`, `a^w ≈ 1 − 5.5e-16`;
 * - `1 − a^w` then subtracts two numbers agreeing to ~16 digits — catastrophic
 *   cancellation, leaving barely a significant bit.
 *
 * At that point the naive form returns `6.9236e-7` against a true `6.8961e-7`:
 * ~0.4% out, in the *first* significant digit.
 *
 * **This is worth fixing but does not buy involutivity.** The `N_w(N_w(a)) = a`
 * round trip is ~8% out near the boundary whichever form is used, because
 * `N_w(a)` lands within two representable doubles of 1 and the intermediate
 * value cannot hold `a` — see [Negations.yager]'s KDoc. Removing the
 * cancellation removes the error this code is responsible for and leaves the one
 * IEEE 754 is. Do not read the reformulation as making the boundary safe.
 *
 * The fix is the standard one. Writing `a^w = exp(w·ln a)`:
 *
 * ```
 * 1 − a^w  =  1 − exp(w·ln a)  =  −expm1(w·ln a)
 * ```
 *
 * `expm1` computes `exp(x) − 1` accurately for small `x`, so the cancellation
 * never happens. `ln a` near `a = 1` comes from `ln1p(a − 1)`, where `a − 1` is
 * *exact* for `a ∈ [0.5, 1]` by the Sterbenz lemma.
 *
 * Below `0.5` it must be plain `ln(a)` instead: there `a − 1` rounds, and for
 * small `a` it rounds to exactly `−1.0`, which would send `ln1p` to `−∞` and
 * return `1` for every tiny `a`. The branch is not a micro-optimisation; each
 * side is wrong where the other is used.
 */
private class YagerNegation(private val w: Double) : Negation {
    override fun apply(a: Double): Double {
        val lnA = if (a > 0.5) ln1p(a - 1.0) else ln(a)
        // `0.0 - x` rather than `-x`: expm1(0) is 0.0, and negating it yields
        // -0.0, whose `pow` can propagate a negative zero out through an odd
        // 1/w. Degrees are not signed.
        val oneMinusPow = 0.0 - expm1(w * lnA)
        return oneMinusPow.pow(1.0 / w)
    }

    override fun toString(): String = "Negation.Yager(w=$w)"
}
