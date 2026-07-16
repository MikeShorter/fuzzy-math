package dk.eusrbin.fuzzy.number

import dk.eusrbin.fuzzy.set.ConvexityWitness
import dk.eusrbin.fuzzy.set.Domain
import dk.eusrbin.fuzzy.set.Verdict

/**
 * A **trapezoidal fuzzy number** `Tz(a, b, c, d)` — "somewhere between `b` and
 * `c`", rising from `a` and falling to `d`.
 *
 * **Attributed:** Dubois & Prade (1978), the LR family. **Not on hand** (§20.6).
 *
 * ```
 *              1 ┤      ╱▔▔▔▔╲
 *                │    ╱        ╲
 *              0 ┼──╱────────────╲──
 *                   a  b      c  d
 * ```
 *
 * ## The type that proves §20.1(b) was not pedantry
 *
 * A [TriangularNumber]'s core is a single point, so `[m]` fits in a `List<Double>`
 * and one could *almost* believe `core(over)` was overridable. A trapezoid's core
 * is **`[b, c]` — a genuine interval**, uncountable, and no list holds it.
 *
 * That is §20.1(b) in one type: `core(over)` asks which *grid points* have `f = 1`,
 * and [coreInterval] asks *where `f = 1` is*. Two questions, two names (§18.3) —
 * and the trapezoid is what makes the difference impossible to paper over.
 *
 * @property a the left foot. `f(a) = 0`.
 * @property b the left shoulder. `f(b) = 1`.
 * @property c the right shoulder. `f(c) = 1`.
 * @property d the right foot. `f(d) = 0`.
 */
public class TrapezoidalNumber private constructor(
    public val a: Double,
    public val b: Double,
    public val c: Double,
    public val d: Double,
) : FuzzyNumber {

    /**
     * `f(x)`.
     *
     * **The plateau is tested first, and it must be.** Feet-first, a crisp interval
     * `Tz(lo, lo, hi, hi)` returns `f(lo) = 0` — `lo <= a` is true when they are the
     * same number — so `crispInterval` was zero at both the points that define it.
     * Same defect as [TriangularNumber.applyAsDouble], same cause: the degenerate
     * member of a family is where clause order stops being cosmetic.
     */
    override fun applyAsDouble(x: Double): Double = when {
        x in b..c -> 1.0 // before the feet: for Tz(a, a, d, d) they coincide
        x <= a || x >= d -> 0.0
        x < b -> (x - a) / (b - a)
        else -> (d - x) / (d - c)
    }

    /**
     * `Γ_α = [a + α(b−a), d − α(d−c)]`.
     *
     * **Source:** the form is Zadeh eq. (24). **Attributed:** the closed form.
     * Verified against `f` at both endpoints for α ∈ {0.25, 0.5, 1.0}.
     *
     * At `α = 1` this is `[b, c]` — the core, and non-degenerate whenever `b < c`.
     */
    override fun alphaCutInterval(alpha: Double): Interval {
        FuzzyNumber.requireCutLevel(alpha)
        return Interval.of(a + alpha * (b - a), d - alpha * (d - c))
    }

    /**
     * `Sup_{x ∈ [lo, hi]} f(x)`, `O(1)` — §20.8's "read the carrier".
     *
     * As [TriangularNumber.supremumOver], but the plateau makes the first test an
     * *interval overlap* rather than a point membership: the window need only
     * touch `[b, c]` anywhere to see a `1`.
     */
    override fun supremumOver(lo: Double, hi: Double): Double = when {
        lo <= c && hi >= b -> 1.0 // window overlaps the plateau [b, c]
        hi <= a || lo >= d -> 0.0 // window misses the support entirely
        hi < b -> applyAsDouble(hi) // rising flank
        else -> applyAsDouble(lo) // falling flank
    }

    /**
     * `Verdict.Refuted` — a trapezoid is convex but **not strongly** convex, and
     * for *two* independent reasons.
     *
     * **Source:** Zadeh 1965, §V p.349. The flat tails refute it, exactly as for a
     * triangle. So does the **plateau**, whenever `b < c`: two distinct points in
     * `[b, c]` both have `f = 1`, and their midpoint's `1 > 1` is false. The tails
     * are used here because they exist for every trapezoid, plateau or not.
     */
    override fun findNonStrongConvexity(
        over: Domain<Double>,
        weights: DoubleArray,
    ): Verdict<ConvexityWitness> {
        val x1 = d + 1.0
        val x2 = d + 3.0
        val lambda = 0.5
        val at = x2 + lambda * (x1 - x2)
        return Verdict.Refuted(ConvexityWitness(x1, x2, lambda, applyAsDouble(at), 0.0))
    }

    override fun toString(): String = "Tz($a, $b, $c, $d)"

    override fun equals(other: Any?): Boolean =
        this === other ||
            (other is TrapezoidalNumber && a == other.a && b == other.b && c == other.c && d == other.d)

    override fun hashCode(): Int =
        31 * (31 * (31 * a.hashCode() + b.hashCode()) + c.hashCode()) + d.hashCode()

    public companion object {

        /**
         * `Tz(a, b, c, d)`.
         *
         * `b == c` is a [TriangularNumber] in all but name — permitted, since
         * refusing it would make the family discontinuous at its own boundary.
         *
         * @throws IllegalArgumentException unless `a ≤ b ≤ c ≤ d` and all finite.
         */
        @JvmStatic
        public fun of(a: Double, b: Double, c: Double, d: Double): TrapezoidalNumber {
            require(a.isFinite() && b.isFinite() && c.isFinite() && d.isFinite()) {
                "Trapezoidal number parameters must be finite, but were ($a, $b, $c, $d)"
            }
            require(a <= b && b <= c && c <= d) {
                "Trapezoidal number needs a ≤ b ≤ c ≤ d, but was ($a, $b, $c, $d)"
            }
            return TrapezoidalNumber(a, b, c, d)
        }

        /** The crisp **interval** `[lo, hi]`, as a trapezoid with vertical sides. */
        @JvmStatic
        public fun crispInterval(lo: Double, hi: Double): TrapezoidalNumber = of(lo, lo, hi, hi)
    }
}
