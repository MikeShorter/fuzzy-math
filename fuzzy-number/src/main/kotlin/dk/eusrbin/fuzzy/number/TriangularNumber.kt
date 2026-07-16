package dk.eusrbin.fuzzy.number

import dk.eusrbin.fuzzy.set.ConvexityWitness
import dk.eusrbin.fuzzy.set.Domain
import dk.eusrbin.fuzzy.set.Verdict

/**
 * A **triangular fuzzy number** `T(l, m, r)` — "about `m`", rising linearly from
 * `l` and falling linearly to `r`.
 *
 * **Attributed:** Dubois & Prade (1978); triangular numbers are the LR family's
 * simplest member. **Not on hand** (CLAUDE.md §20.6). The α-cut below is
 * elementary similar triangles and is asserted by `fuzzy-laws`, not by a citation.
 *
 * ```
 *              1 ┤        ╱╲
 *                │      ╱    ╲
 *                │    ╱        ╲
 *              0 ┼──╱────────────╲──
 *                   l      m      r
 * ```
 *
 * ## This class is why §20 exists
 *
 * §15.3 — *closed forms are overrides on the function* — was an **unproven claim**
 * until this type: nothing in `fuzzy-set` overrode anything. Building it proved
 * the decision and corrected its worked example (§20.8). Every override here is
 * annotated with which §20.8 group it belongs to and why.
 *
 * @property l the left foot. `f(l) = 0`.
 * @property m the peak. `f(m) = 1`.
 * @property r the right foot. `f(r) = 0`.
 */
public class TriangularNumber private constructor(
    public val l: Double,
    public val m: Double,
    public val r: Double,
) : FuzzyNumber {

    /**
     * `f(x)` — primitive in, primitive out (§9).
     *
     * Zero outside `(l, r)`; linear on each side of the peak. The degenerate
     * shoulders (`l == m` or `m == r`) are guarded, since a zero-width side would
     * divide by zero rather than give the vertical edge intended.
     */
    /**
     * `f(x)`, by similar triangles.
     *
     * **The peak is tested first, and it must be.** Written feet-first — `x <= l ||
     * x >= r -> 0.0` — a **crisp point `T(m, m, m)` returns `f(m) = 0`**, because
     * `m <= l` is true when they are the same number. The degenerate member of the
     * family evaluates to zero everywhere, including at its own peak. Found by
     * `FuzzyNumberLaws`' normality check, not by reading this.
     */
    override fun applyAsDouble(x: Double): Double = when {
        x == m -> 1.0 // before the feet: for T(m, m, m) they coincide
        x <= l || x >= r -> 0.0
        x < m -> (x - l) / (m - l)
        else -> (r - x) / (r - m)
    }

    /**
     * `Γ_α = [l + α(m−l), r − α(r−m)]` — by similar triangles.
     *
     * **Source:** the *form* is Zadeh eq. (24), §V p.347. **Attributed:** the
     * closed form itself.
     *
     * Exact, `O(1)`, and needs no [Domain] — which is the whole point of the
     * second name (§20.1(b), §20.2). At `α = 1` it degenerates to `[m, m]`, the
     * peak; at `α → 0` it approaches `[l, r]`, the support's closure — but never
     * reaches it, and `α = 0` is rejected because `Γ_0` is all of ℝ.
     *
     * @throws IllegalArgumentException if [alpha] is not in `(0, 1]`.
     */
    override fun alphaCutInterval(alpha: Double): Interval {
        FuzzyNumber.requireCutLevel(alpha)
        return Interval.of(l + alpha * (m - l), r - alpha * (r - m))
    }

    // ---- §20.8, group "supplied": read the carrier ---------------------------

    /**
     * `Sup_{x ∈ [lo, hi]} f(x)`, in `O(1)` — **the method §20.8 is about**.
     *
     * Three cases, and unimodality is what makes them exhaustive:
     *
     * 1. the peak is inside the window → `1.0`;
     * 2. the window misses `(l, r)` entirely → `0.0` — **the case that refuted
     *    §15.3's "ignores the domain entirely"**;
     * 3. otherwise the window lies wholly on one flank, where `f` is monotone, so
     *    the supremum is at whichever endpoint is nearer the peak.
     *
     * Better than the fold, not merely faster: for `T(-0.5, 0.5, 1.5)` over
     * `Sampled(-1, 2, 1024)` this gives `1.0` where the fold gives `0.998534`,
     * because `0.5` is not a grid point.
     */
    override fun supremumOver(lo: Double, hi: Double): Double = when {
        m in lo..hi -> 1.0
        hi <= l || lo >= r -> 0.0
        hi < m -> applyAsDouble(hi) // rising flank: nearest the peak is the right edge
        else -> applyAsDouble(lo) // falling flank: nearest the peak is the left edge
    }

    // ---- §20.8, group "fixed": the universe is ℝ ----------------------------

    /**
     * `Verdict.Refuted`, with an **analytic** witness — a triangle is convex but
     * **not strongly** convex.
     *
     * **Source:** Zadeh 1965, §V p.349. Strong convexity demands
     * `f[λx₁+(1−λ)x₂] > Min[f(x₁), f(x₂)]` strictly, for any two distinct points in
     * **ℝ**. A triangle is zero on both tails, so any two points beyond `r` give
     * `0 > 0` — false. No search needed; the witness is constructed.
     *
     * §20.8: this override may ignore [over] because strong convexity's universe is
     * ℝ, fixed by the operation. The witness uses points outside the support, which
     * exist whatever domain was passed.
     *
     * Contrast [findNonConvexity], inherited from [FuzzyNumber], which returns
     * `Proven` for the same structural reason (§20.5).
     */
    override fun findNonStrongConvexity(
        over: Domain<Double>,
        weights: DoubleArray,
    ): Verdict<ConvexityWitness> {
        // Two distinct points beyond the right foot. f = 0 at both, and 0 > 0 is
        // false, so the midpoint refutes strong convexity.
        val x1 = r + 1.0
        val x2 = r + 3.0
        val lambda = 0.5
        val at = x2 + lambda * (x1 - x2) // §19.7's form: exact when x₁ = x₂
        return Verdict.Refuted(
            ConvexityWitness(x1, x2, lambda, applyAsDouble(at), 0.0),
        )
    }

    override fun toString(): String = "T($l, $m, $r)"

    override fun equals(other: Any?): Boolean =
        this === other || (other is TriangularNumber && l == other.l && m == other.m && r == other.r)

    override fun hashCode(): Int = (31 * (31 * l.hashCode() + m.hashCode())) + r.hashCode()

    public companion object {

        /**
         * `T(l, m, r)`.
         *
         * `l == m` or `m == r` is permitted — a right-angled triangle, which is an
         * ordinary "at most m" / "at least m" number. `l == m == r` is a crisp
         * point, and is the degenerate case α-cut arithmetic reduces to when
         * nothing is fuzzy.
         *
         * @throws IllegalArgumentException unless `l ≤ m ≤ r` and all are finite.
         *   Checked eagerly: these are *values* (§4).
         */
        @JvmStatic
        public fun of(l: Double, m: Double, r: Double): TriangularNumber {
            require(l.isFinite() && m.isFinite() && r.isFinite()) {
                "Triangular number parameters must be finite, but were ($l, $m, $r)"
            }
            require(l <= m && m <= r) {
                "Triangular number needs l ≤ m ≤ r, but was ($l, $m, $r)"
            }
            return TriangularNumber(l, m, r)
        }

        /** `T(m − spread, m, m + spread)` — symmetric about [m]. */
        @JvmStatic
        public fun symmetric(m: Double, spread: Double): TriangularNumber {
            require(spread >= 0.0) { "Spread must be ≥ 0, but was $spread" }
            return of(m - spread, m, m + spread)
        }

        /** The crisp point [x], as a triangular number with no spread. */
        @JvmStatic
        public fun crisp(x: Double): TriangularNumber = of(x, x, x)
    }
}
