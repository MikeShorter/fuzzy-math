package dk.eusrbin.fuzzy.number

import dk.eusrbin.fuzzy.set.ConvexityWitness
import dk.eusrbin.fuzzy.set.Domain
import dk.eusrbin.fuzzy.set.Verdict
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * A **Gaussian fuzzy number** — `f(x) = exp(−(x − m)² / 2σ²)`.
 *
 * **Attributed:** standard in the fuzzy-number literature; **not on hand** (§20.6).
 * Bergmann §17.1 discusses membership-function *shapes* — Black's curves, Goguen's
 * `1/(1+x)` — but not this one, and not as an arithmetic type. Checked, not
 * assumed.
 *
 * Note this is the **membership** Gaussian, normalised to `f(m) = 1`, not the
 * probability density. CLAUDE.md §0's whole point: a grade of membership is not a
 * probability, and the missing `1/(σ√2π)` is where that shows.
 *
 * ## Bounded in Zadeh's sense, with an unbounded support
 *
 * `f > 0` **everywhere**, so the support is ℝ. That does not disqualify it:
 * Zadeh's boundedness (§V p.348) asks that `Γ_α` be bounded **for every `α > 0`**,
 * and `Γ_α = m ± σ√(2 ln(1/α))` is bounded for all such α. Only `Γ_0` — all of ℝ
 * for every fuzzy set — is not, and `α = 0` is excluded from
 * [FuzzyNumber.alphaCutInterval] for that reason.
 *
 * So a Gaussian is a fuzzy number whose *support interval* does not exist. That is
 * why [FuzzyNumber] exposes `alphaCutInterval` and **not** a `supportInterval`:
 * one of them is total and the other is not.
 *
 * @property mean the peak `m`. `f(m) = 1`.
 * @property sigma the spread `σ > 0`.
 */
public class GaussianNumber private constructor(
    public val mean: Double,
    public val sigma: Double,
) : FuzzyNumber {

    override fun applyAsDouble(x: Double): Double {
        val z = (x - mean) / sigma
        return exp(-0.5 * z * z)
    }

    /**
     * `Γ_α = [m − σ√(2 ln(1/α)), m + σ√(2 ln(1/α))]`.
     *
     * Inverting `exp(−z²/2) = α`. At `α = 1` this is `[m, m]` — `ln(1) = 0` — so
     * the core is the peak, as it should be.
     *
     * **§20.2(iii): the round trip through `√`/`ln`/`exp` is not exact**, and this
     * is the type that shows it. `f(endpoint)` lands at `α ± ε`:
     * `α = 0.9` gives `0.89999999999999991`, `α = 0.1` gives `0.10000000000000002`.
     * Not systematic — some α round-trip exactly. So the law relating
     * [FuzzyNumber.alphaCutInterval] to `MembershipFn.alphaCut` takes a
     * tolerance at the boundary (`fuzzy-laws`' `Tolerance`, per §8) and is stated strictly only on the interior.
     * That is §14.6(a) again: **an exact comparison meeting a computed value — the
     * exactness belongs to the comparison, not to the value.**
     */
    override fun alphaCutInterval(alpha: Double): Interval {
        FuzzyNumber.requireCutLevel(alpha)
        val halfWidth = sigma * sqrt(2.0 * ln(1.0 / alpha))
        return Interval.of(mean - halfWidth, mean + halfWidth)
    }

    /**
     * `Sup_{x ∈ [lo, hi]} f(x)`, `O(1)` — §20.8.
     *
     * Two cases only: a Gaussian's support is ℝ, so the *"window misses the
     * support"* branch that [TriangularNumber] needs cannot arise. `f` is strictly
     * monotone on each side of `m`, so off-peak the supremum is at the nearer
     * endpoint.
     */
    override fun supremumOver(lo: Double, hi: Double): Double = when {
        mean in lo..hi -> 1.0
        hi < mean -> applyAsDouble(hi)
        else -> applyAsDouble(lo)
    }

    /**
     * **`Verdict.Proven`** — a Gaussian **is** strongly convex, and analytically so.
     *
     * **Source:** Zadeh 1965, §V p.349 for the definition. The proof: `f` is
     * strictly quasi-concave — `exp(−z²/2)` is strictly decreasing in `|x − m|` and
     * **flat nowhere** — so for distinct `x₁, x₂` and `λ ∈ (0,1)` the interpolated
     * point is strictly nearer `m` than the farther endpoint, giving
     * `f[·] > Min[f(x₁), f(x₂)]` strictly.
     *
     * ## The contrast worth seeing
     *
     * | | `findNonConvexity` | `findNonStrongConvexity` |
     * |---|---|---|
     * | [TriangularNumber] | `Proven` (inherited) | **`Refuted`** — flat tails, `0 > 0` fails |
     * | `GaussianNumber` | `Proven` (inherited) | **`Proven`** — no flat anywhere |
     *
     * Both answers are closed-form; neither searches. §20.8 permits both to ignore
     * the domain because strong convexity's universe is **ℝ, fixed by the
     * operation** — and Zadeh's own caveat (p.349, *"strong convexity does not
     * imply strict convexity or vice-versa"*) is why the two rows are independent
     * rather than a tier.
     *
     * `ConvexityLaws` cross-checks it the honest way (§20.5): `Proven` ⟹ the
     * generic search finds no witness. It never asks how the claim was reached.
     */
    override fun findNonStrongConvexity(
        over: Domain<Double>,
        weights: DoubleArray,
    ): Verdict<ConvexityWitness> = Verdict.Proven()

    override fun toString(): String = "N($mean, σ=$sigma)"

    override fun equals(other: Any?): Boolean =
        this === other || (other is GaussianNumber && mean == other.mean && sigma == other.sigma)

    override fun hashCode(): Int = 31 * mean.hashCode() + sigma.hashCode()

    public companion object {

        /**
         * `exp(−(x − mean)² / 2σ²)`.
         *
         * @throws IllegalArgumentException if [mean] is not finite, or [sigma] is
         *   not finite and positive. `σ = 0` would be a crisp point and a division
         *   by zero — use `TriangularNumber.crisp` for that.
         */
        @JvmStatic
        public fun of(mean: Double, sigma: Double): GaussianNumber {
            require(mean.isFinite()) { "Gaussian mean must be finite, but was $mean" }
            require(sigma.isFinite() && sigma > 0.0) {
                "Gaussian σ must be finite and positive, but was $sigma " +
                    "(σ = 0 is a crisp point — use TriangularNumber.crisp)"
            }
            return GaussianNumber(mean, sigma)
        }
    }
}
