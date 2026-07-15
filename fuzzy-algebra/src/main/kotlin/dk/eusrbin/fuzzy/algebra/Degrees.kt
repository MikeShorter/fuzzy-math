package dk.eusrbin.fuzzy.algebra

/**
 * The unit interval `[0,1]`, in which every value in this library lives.
 *
 * Depending on which hat you are wearing, an element of `[0,1]` is either a
 * **membership degree** `f_A(x)` — "the grade of membership of `x` in `A`"
 * (Zadeh 1965, §II, p.339) — or a **truth value** of a many-valued logic
 * (Bergmann 2008, §11.2). Zadeh's own footnote 3 (p.339) observes that these
 * are the same thing:
 *
 * > "the notion of a fuzzy set is completely non-statistical in nature ...
 * > \[if\] `f_A(x)` is interpreted as the truth value of the proposition
 * > *x is a member of A*, \[fuzzy set theory\] is a many-valued logic with a
 * > continuum of truth values in the interval `[0,1]`."
 *
 * That footnote is why this module exists standalone (CLAUDE.md §2, §10).
 *
 * ## Representation
 *
 * A degree is a primitive [Double], never a wrapper and never a value class
 * (CLAUDE.md §4, §9). There is deliberately no `Degree` type:
 *
 * - CLAUDE.md §4 rules out generic-over-lattice membership because "the hot
 *   path is Sup over a sampled grid — millions of calls. Boxing there destroys
 *   the performance story."
 * - CLAUDE.md §9 rules out value/inline classes in public signatures because
 *   their name mangling is hostile from Java.
 *
 * The cost of that choice is that `[0,1]`-ness cannot be enforced by the type
 * system, only checked. This object holds the checks.
 *
 * ## Where validation happens — and where it does not
 *
 * **Operations do not validate.** [TNorm.apply], [TConorm.apply],
 * [Negation.apply] and [Implication.apply] treat `a, b ∈ [0,1]` as a
 * *documented precondition*, not a checked one. Validating on every call would
 * put a branch in the innermost loop of the very code path §4 protects.
 *
 * **Construction does validate.** Anything with a parameter — [Negations.sugeno],
 * [Negations.yager], [TNorms.hamacher], [TNorms.ordinalSum] — checks eagerly,
 * once, at construction. That is where a mistake is cheap to catch and cheap to
 * check.
 *
 * If you are feeding this library data from outside, run it through [clamp] or
 * [requireDegree] at your boundary, once, and then stop worrying.
 */
public object Degrees {

    /** The least degree: non-membership, or absolute falsity. Zadeh 1965, §II. */
    public const val MIN: Double = 0.0

    /** The greatest degree: full membership, or absolute truth. Zadeh 1965, §II. */
    public const val MAX: Double = 1.0

    /**
     * `true` iff [x] lies in `[0,1]`.
     *
     * `NaN` is not a degree and yields `false` — the comparison chain does that
     * for free, since every ordered comparison against `NaN` is `false`.
     */
    @JvmStatic
    public fun isDegree(x: Double): Boolean = x >= MIN && x <= MAX

    /**
     * Returns [x] if it is a degree; otherwise throws [IllegalArgumentException].
     *
     * @param name a label for the offending value, used in the message.
     * @throws IllegalArgumentException if [x] is outside `[0,1]` or is `NaN`.
     */
    @JvmStatic
    @JvmOverloads
    public fun requireDegree(x: Double, name: String = "value"): Double {
        if (!isDegree(x)) {
            throw IllegalArgumentException("$name must be a degree in [0,1], but was $x")
        }
        return x
    }

    /**
     * Clamps [x] into `[0,1]`.
     *
     * `NaN` is rejected rather than clamped: there is no defensible degree to
     * map it to, and silently choosing one would launder a bug in the caller
     * into a plausible-looking answer.
     *
     * @throws IllegalArgumentException if [x] is `NaN`.
     */
    @JvmStatic
    public fun clamp(x: Double): Double = when {
        x.isNaN() -> throw IllegalArgumentException("NaN is not a degree and cannot be clamped into [0,1]")
        x < MIN -> MIN
        x > MAX -> MAX
        else -> x
    }
}
