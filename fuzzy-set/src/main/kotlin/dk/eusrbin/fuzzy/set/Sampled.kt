package dk.eusrbin.fuzzy.set

/**
 * A [Domain] that folds over an evenly-spaced **grid on an interval of ℝ**.
 * Approximate, and says so.
 *
 * CLAUDE.md §15.4. This is the case Zadeh's own examples mostly inhabit — *"the
 * class of all real numbers which are much greater than 1"* (p.339), and every
 * figure in §V. `X` is uncountable, so no fold can visit it; a grid is the
 * concession, and [isExhaustive] is `false` to say so out loud.
 *
 * ```java
 * DoubleMembershipFn nearZero = x -> Math.exp(-x * x);
 * Domain<Double> line = Sampled.of(-3.0, 3.0, 1024);
 * ```
 *
 * ## What "approximate" costs you, precisely
 *
 * Everything that reduces to a number — height, σ-count — is a **lower bound**
 * on the truth, not the truth. A supremum over 1024 points cannot exceed the real
 * supremum, and will miss a spike between two of them. Refine the grid and watch
 * it converge; §15.1's decision to keep the domain out of a set's type exists
 * partly so you *can* — the same set over a coarse and a fine domain is one
 * expression, not a type error.
 *
 * Everything that quantifies over `X` — containment, equality, emptiness — can
 * only report **"no counterexample found"**. That is §15.6, and it is why those
 * operations return a witness rather than a boolean. Returning `true` here would
 * assert a proof nobody performed.
 *
 * Everything that returns elements — support, core, α-cuts — returns the grid
 * points that qualify, which is a *sample of* the answer. The α-cut of a
 * continuous function is an interval; what you get back is the points of the grid
 * inside it.
 *
 * ## One dimension, and this is a wall
 *
 * `Sampled` is a grid over ℝ, not ℝⁿ, and [Product] does not rescue that. At 1000
 * points per dimension a fold costs 10³ evaluations on ℝ, **10⁶ on ℝ²**, and
 * **10⁹ on ℝ³**. Grid sampling is tractable in one dimension and nowhere else.
 *
 * §15.4 documents this as a wall rather than papering over it: `Product(Sampled,
 * Sampled)` is legal, will compile, and will take a very long time. It exists for
 * `Product(Enumerable, Enumerable)` — which `fuzzy-relation` (§10) needs for
 * `Sup_v Min[...]` (Zadeh p.346) — and for mixed products where only one factor is
 * a continuum.
 *
 * ## The primitive path
 *
 * [reduceDegrees] checks **once, outside its loop**, whether the function is a
 * [DoubleMembershipFn], and if so calls `applyAsDouble` — no boxing per element.
 * This is the loop CLAUDE.md §4 protects. Hand it a `MembershipFn<Double>` and it
 * still works, boxing each point; hand it a `DoubleMembershipFn` and it does not.
 *
 * @property lower the interval's lower endpoint, sampled exactly.
 * @property upper the interval's upper endpoint, sampled exactly.
 * @property points how many grid points, endpoints included.
 */
public class Sampled private constructor(
    public val lower: Double,
    public val upper: Double,
    public val points: Int,
) : Domain<Double> {

    /**
     * Always `false`. A grid is a sample of ℝ, never a visit to it.
     *
     * The single most important fact about this class, and the reason §15.6 has
     * the operations that quantify over `X` report counterexamples.
     */
    override val isExhaustive: Boolean
        get() = false

    /** The spacing between adjacent grid points, `(upper − lower) / (points − 1)`. */
    public val step: Double
        get() = (upper - lower) / (points - 1)

    /**
     * The `i`-th grid point, `0 ≤ i < points`.
     *
     * The endpoints are returned **exactly**, not reconstructed by arithmetic:
     * `lower + (upper − lower) · i/(points−1)` need not land precisely on `upper`
     * at the last index once rounding is involved, and the endpoints are where
     * α-cuts and Zadeh's intervals are most often decided. Interior points are
     * computed from `lower` each time rather than accumulated, so error does not
     * grow along the grid.
     *
     * @throws IndexOutOfBoundsException if [i] is outside `[0, points)`.
     */
    public fun pointAt(i: Int): Double {
        if (i < 0 || i >= points) {
            throw IndexOutOfBoundsException("Grid index $i outside [0, $points)")
        }
        if (i == 0) return lower
        if (i == points - 1) return upper
        return lower + (upper - lower) * (i.toDouble() / (points - 1).toDouble())
    }

    /**
     * The grid points — **not** ℝ.
     *
     * A materialised, boxed list of [points] doubles. Nothing on the hot path
     * calls it: [reduceDegrees], [firstWhere] and [filter] all walk the grid by
     * index instead. It exists because [Domain.elements] is the element-source
     * primitive [Product] builds pairs from, and because being able to ask a grid
     * what is in it is worth having.
     */
    override fun elements(): List<Double> {
        val result = ArrayList<Double>(points)
        for (i in 0 until points) result.add(pointAt(i))
        return result
    }

    override fun reduceDegrees(fn: MembershipFn<Double>, initial: Double, reducer: DegreeReducer): Double {
        var accumulator = initial
        // Hoisted out of the loop, deliberately: one type check per fold rather
        // than one per point. This is the §4 hot path — "millions of calls".
        if (fn is DoubleMembershipFn) {
            for (i in 0 until points) {
                accumulator = reducer.apply(accumulator, fn.applyAsDouble(pointAt(i)))
            }
        } else {
            for (i in 0 until points) {
                accumulator = reducer.apply(accumulator, fn.apply(pointAt(i)))
            }
        }
        return accumulator
    }

    override fun firstWhere(predicate: DomainPredicate<Double>): Double? {
        for (i in 0 until points) {
            val x = pointAt(i)
            if (predicate.test(x)) return x
        }
        return null
    }

    override fun filter(predicate: DomainPredicate<Double>): List<Double> {
        val result = ArrayList<Double>()
        for (i in 0 until points) {
            val x = pointAt(i)
            if (predicate.test(x)) result.add(x)
        }
        return result
    }

    override fun toString(): String = "Sampled([$lower, $upper], $points points)"

    public companion object {

        /**
         * The default grid resolution: 1024 points.
         *
         * **Arbitrary, and meant to be overridden when it matters.** It is a
         * round binary number in the range where §15.4's cost argument still says
         * "cheap" — a fold is a thousand primitive evaluations, microseconds. It
         * is not a claim about the accuracy of anything: a function with a spike
         * narrower than `(upper − lower)/1023` will have that spike missed, at
         * this resolution or any other. Refine and compare.
         */
        public const val DEFAULT_POINTS: Int = 1024

        /**
         * A grid of [points] evenly spaced over `[lower, upper]`, endpoints
         * included.
         *
         * @throws IllegalArgumentException if the interval is not a real,
         *   non-degenerate interval, or if [points] is below 2.
         */
        @JvmStatic
        @JvmOverloads
        public fun of(lower: Double, upper: Double, points: Int = DEFAULT_POINTS): Sampled {
            require(lower.isFinite() && upper.isFinite()) {
                "Grid endpoints must be finite, but were [$lower, $upper]"
            }
            require(lower < upper) {
                "Grid must span a non-degenerate interval, but was [$lower, $upper]"
            }
            require(points >= 2) {
                "A grid needs at least both endpoints, but points was $points"
            }
            return Sampled(lower, upper, points)
        }

        /**
         * A grid over `[lower, upper]` with a spacing of about [step].
         *
         * The point count is `round((upper − lower)/step) + 1`, and the spacing is
         * then whatever divides the interval evenly into that many gaps — so the
         * endpoints stay exact and the actual [Sampled.step] may differ slightly
         * from the one requested. Endpoint fidelity is worth more than spacing
         * fidelity: `upper` is a point people ask about by name.
         *
         * @throws IllegalArgumentException if [step] is not positive and finite,
         *   or if the interval is not a real, non-degenerate one.
         */
        @JvmStatic
        public fun withStep(lower: Double, upper: Double, step: Double): Sampled {
            require(step.isFinite() && step > 0.0) {
                "Grid step must be finite and positive, but was $step"
            }
            require(lower.isFinite() && upper.isFinite()) {
                "Grid endpoints must be finite, but were [$lower, $upper]"
            }
            require(lower < upper) {
                "Grid must span a non-degenerate interval, but was [$lower, $upper]"
            }
            val gaps = Math.round((upper - lower) / step).toInt()
            return of(lower, upper, maxOf(gaps, 1) + 1)
        }
    }
}
