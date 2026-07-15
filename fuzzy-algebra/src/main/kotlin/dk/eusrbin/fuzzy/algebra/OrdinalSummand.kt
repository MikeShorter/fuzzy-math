package dk.eusrbin.fuzzy.algebra

/**
 * One summand of an ordinal sum: a t-norm [tNorm] squeezed into the subinterval
 * `[lower, upper]` of `[0,1]`.
 *
 * Source: Klement, Mesiar & Pap 2000 (ordinal sums); Bergmann 2008, §11.7.
 * See [TNorms.ordinalSum] for the construction and for why it matters.
 *
 * The interval is given by its closed endpoints, but it is the **open** interval
 * `(lower, upper)` that must be disjoint from every other summand's — adjacent
 * summands may share an endpoint (`[0, 0.5]` and `[0.5, 1]` is legal), because
 * at a shared endpoint both candidate formulas agree with `min`.
 *
 * ```java
 * // Łukasiewicz on the bottom half, Product on the top, min in between.
 * TNorm t = TNorms.ordinalSum(
 *     new OrdinalSummand(0.0, 0.4, TNorms.LUKASIEWICZ),
 *     new OrdinalSummand(0.6, 1.0, TNorms.PRODUCT));
 * ```
 *
 * @property lower the lower endpoint `a_i`, in `[0,1]`.
 * @property upper the upper endpoint `b_i`, in `[0,1]`, strictly greater than [lower].
 * @property tNorm the t-norm to rescale into `[lower, upper]`.
 * @throws IllegalArgumentException if the endpoints are not a valid, non-degenerate
 *   subinterval of `[0,1]`. Checked eagerly at construction — see [Degrees] on
 *   where this library validates and where it does not.
 */
public class OrdinalSummand(
    public val lower: Double,
    public val upper: Double,
    public val tNorm: TNorm,
) {
    init {
        Degrees.requireDegree(lower, "lower")
        Degrees.requireDegree(upper, "upper")
        require(lower < upper) {
            "Summand interval must be non-degenerate, but was [$lower, $upper]. " +
                "A degenerate interval contributes nothing: the ordinal sum would " +
                "be `min` there anyway."
        }
    }

    /** The width `b_i − a_i` of this summand's interval; the rescaling factor. */
    public val width: Double
        get() = upper - lower

    /** `true` iff [x] lies in this summand's closed interval `[lower, upper]`. */
    public operator fun contains(x: Double): Boolean = x >= lower && x <= upper

    override fun toString(): String = "OrdinalSummand([$lower, $upper], $tNorm)"

    override fun equals(other: Any?): Boolean =
        this === other ||
            (
                other is OrdinalSummand &&
                    lower == other.lower &&
                    upper == other.upper &&
                    tNorm == other.tNorm
                )

    override fun hashCode(): Int {
        var result = lower.hashCode()
        result = 31 * result + upper.hashCode()
        result = 31 * result + tNorm.hashCode()
        return result
    }
}
