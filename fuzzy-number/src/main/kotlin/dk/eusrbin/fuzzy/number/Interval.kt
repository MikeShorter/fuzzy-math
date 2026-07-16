package dk.eusrbin.fuzzy.number

import kotlin.math.max
import kotlin.math.min

/**
 * A closed, bounded real interval `[lower, upper]`.
 *
 * **Attributed:** ordinary interval arithmetic — Moore (1966) is the standard
 * attribution and is **not on hand** (CLAUDE.md §17.5, §20.6). Nothing here is
 * cited as though it could arbitrate; the operations are elementary and their
 * correctness is asserted by `fuzzy-laws` rather than by a reference.
 *
 * ## Why this type exists, twice over
 *
 * CLAUDE.md §20.2. It answers two needs that turned out to be one:
 *
 * 1. **§20.1(b)'s second name.** `MembershipFn.alphaCut(over, α)` returns
 *    `List<X>` — which grid points are in `Γ_α`. A [FuzzyNumber]'s `Γ_α` **is** an
 *    interval, and no `List<Double>` can hold it. Those are two questions, so
 *    they get two names (§18.3), and this is the return type of the second.
 * 2. **α-cut arithmetic.** `A + B` is defined cut-wise: `(A+B)_α = A_α + B_α`.
 *    Without intervals there is nothing to add.
 *
 * ## It needs an **order** on X, which a `Domain` does not supply
 *
 * The same shape of constraint as §15.5's vector space, and the same resolution:
 * this lives on the ℝ tier. `Domain<X>` can *search* X; it cannot say which of two
 * elements is smaller, so it cannot form an interval in X. That is why
 * [FuzzyNumber] extends `DoubleMembershipFn` and not `MembershipFn<X>`.
 *
 * ## Degenerate intervals are legal
 *
 * `[a, a]` is a point, and it is exactly what `alphaCutInterval(1.0)` returns for a
 * triangular number — the peak. Rejecting it would reject the most ordinary case
 * in the module.
 *
 * @property lower the left endpoint, `≤ upper`.
 * @property upper the right endpoint.
 */
public class Interval private constructor(
    public val lower: Double,
    public val upper: Double,
) {

    /** `upper − lower`. Zero for a degenerate interval. */
    public val width: Double
        get() = upper - lower

    /** The midpoint `(lower + upper) / 2`. */
    public val midpoint: Double
        get() = lower + 0.5 * width

    /** `true` iff this interval is a single point. */
    public val isDegenerate: Boolean
        get() = lower == upper

    /** `true` iff [x] lies in `[lower, upper]`. Closed at both ends. */
    public operator fun contains(x: Double): Boolean = x in lower..upper

    /** `true` iff every point of [other] lies in this interval. */
    public operator fun contains(other: Interval): Boolean =
        other.lower >= lower && other.upper <= upper

    /**
     * `[a, b] + [c, d] = [a+c, b+d]`.
     *
     * **Attributed:** interval arithmetic. The named method; [plus] is the Kotlin
     * operator alongside it, per §9.
     */
    public fun add(other: Interval): Interval = of(lower + other.lower, upper + other.upper)

    /** Kotlin operator for [add]. Java callers use [add]. */
    public operator fun plus(other: Interval): Interval = add(other)

    /**
     * `[a, b] − [c, d] = [a−d, b−c]`.
     *
     * **Note the crossed endpoints, and note what follows from them.**
     * `X − X = [a−b, b−a]`, which is **not** `[0, 0]` unless `X` is degenerate.
     * Interval arithmetic has no cancellation: the two `X`s are treated as
     * independent quantities that merely happen to share a range. This is the
     * root of CLAUDE.md §20.4, and the reason [FuzzyNumber.subtract] carries the
     * warning it does.
     */
    public fun subtract(other: Interval): Interval = of(lower - other.upper, upper - other.lower)

    /** Kotlin operator for [subtract]. Java callers use [subtract]. */
    public operator fun minus(other: Interval): Interval = subtract(other)

    /**
     * `[a, b] × [c, d] = [min, max]` of the four endpoint products.
     *
     * All four are needed, and only because of sign: for intervals spanning zero,
     * the extremes of the product are not attained at matching endpoints. For two
     * positive intervals it reduces to `[a·c, b·d]`, but branching on sign to save
     * three multiplications would be a bug waiting for the first negative fuzzy
     * number.
     */
    public fun multiply(other: Interval): Interval {
        val a = lower * other.lower
        val b = lower * other.upper
        val c = upper * other.lower
        val d = upper * other.upper
        return of(min(min(a, b), min(c, d)), max(max(a, b), max(c, d)))
    }

    /** Kotlin operator for [multiply]. Java callers use [multiply]. */
    public operator fun times(other: Interval): Interval = multiply(other)

    /** Scales both endpoints by [factor], swapping them if [factor] is negative. */
    public fun scale(factor: Double): Interval =
        if (factor >= 0.0) of(lower * factor, upper * factor) else of(upper * factor, lower * factor)

    override fun toString(): String = "[$lower, $upper]"

    override fun equals(other: Any?): Boolean =
        this === other || (other is Interval && lower == other.lower && upper == other.upper)

    override fun hashCode(): Int = 31 * lower.hashCode() + upper.hashCode()

    public companion object {

        /**
         * The interval `[lower, upper]`.
         *
         * @throws IllegalArgumentException if the endpoints are not finite, or if
         *   `lower > upper`. Checked eagerly — this is a *value*, and §4's split
         *   puts value validation at construction. (Contrast [AlphaCutNumber]'s
         *   nestedness, which is a property of a *function* and cannot be checked
         *   here at all — §20.2(ii).)
         */
        @JvmStatic
        public fun of(lower: Double, upper: Double): Interval {
            require(lower.isFinite() && upper.isFinite()) {
                "Interval endpoints must be finite, but were [$lower, $upper]"
            }
            require(lower <= upper) {
                "Interval must have lower ≤ upper, but was [$lower, $upper]"
            }
            return Interval(lower, upper)
        }

        /** The degenerate interval `[x, x]`. */
        @JvmStatic
        public fun point(x: Double): Interval = of(x, x)
    }
}
