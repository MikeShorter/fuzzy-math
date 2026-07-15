package dk.eusrbin.fuzzy.laws

import dk.eusrbin.fuzzy.algebra.Algebra
import dk.eusrbin.fuzzy.algebra.TNorm
import dk.eusrbin.fuzzy.algebra.TNorms
import kotlin.math.abs

/**
 * How close is close enough — **the one place tolerances live**.
 *
 * CLAUDE.md §8: *"Tolerances are calibrated **per algebra**, defined in one
 * place inside `fuzzy-laws`, never scattered through test files."* This class is
 * that place. If you are about to write a literal epsilon in a test, add a
 * calibration here instead and say why.
 *
 * ## Why this exists at all
 *
 * The algebraic laws of CLAUDE.md §7 are stated over the reals. We evaluate them
 * in IEEE 754. They are not the same claim:
 *
 * - `min`/`max` are **exactly** associative, idempotent and distributive. They
 *   select an operand; they never compute one. Nothing rounds. → [EXACT]
 * - The **Product** t-norm's associativity is not exact: `(a·b)·c` and `a·(b·c)`
 *   round differently. → [ARITHMETIC]
 * - **Łukasiewicz** suffers cancellation: `a + b − 1` loses low bits precisely
 *   when `a + b ≈ 1`, which is where its interesting behaviour is. → [ARITHMETIC]
 * - Parametric families divide and exponentiate ([TNorms.hamacher],
 *   `Negations.yager`), and residua for them come from bisection. → [GENERAL]
 *
 * So the law tiers acquire a numerical dimension, exactly as CLAUDE.md §8 says.
 *
 * CLAUDE.md §8 also records the rejected alternative — Kazakov's fixed
 * `Resolution`, discretising `[0,1]` so that everything is exact: *"it trades
 * the whole continuum, and the parametric/analytic path, for exactness we can
 * get with tolerances."*
 *
 * ## Absolute, not relative
 *
 * Comparison is absolute: `|a − b| ≤ ε`. This is deliberate and it is the right
 * choice *here* specifically, for two reasons:
 *
 * 1. Degrees live in `[0,1]`. There is no dynamic range for a relative measure
 *    to earn its keep — absolute and relative error coincide up to a factor of
 *    one, near the top of the interval where it matters.
 * 2. Relative error is undefined or useless exactly where these algebras are
 *    most interesting: at `0`. Łukasiewicz and the nilpotent norms genuinely
 *    *reach* zero on large regions, and "`0` vs `1e-17` is a 100% relative
 *    error" is a statement about arithmetic, not about mathematics.
 */
public class Tolerance private constructor(
    /** A human-readable name, reported by [LawReport]. */
    public val name: String,
    /** The absolute epsilon. Zero means exact equality is required. */
    public val epsilon: Double,
) {

    /** `true` iff [a] and [b] agree to within [epsilon]. */
    public fun eq(a: Double, b: Double): Boolean = a == b || abs(a - b) <= epsilon

    /** `true` iff `a ≤ b` holds to within [epsilon]. */
    public fun leq(a: Double, b: Double): Boolean = a <= b + epsilon

    override fun toString(): String = "Tolerance.$name(ε=$epsilon)"

    public companion object {

        /**
         * Exact equality — `ε = 0`.
         *
         * Correct for the Zadeh/Gödel tier and nothing else. CLAUDE.md §8:
         * *"min/max are exactly associative and idempotent → the Zadeh tier is
         * safe with exact equality."*
         *
         * This is not laxity avoided, it is information gained: if
         * `StandardLaws.check(Algebra.STANDARD)` ever needed an epsilon, `min`
         * or `max` would have stopped being `min` or `max`.
         */
        @JvmField
        public val EXACT: Tolerance = Tolerance("Exact", 0.0)

        /**
         * For closed-form algebras built from `+`, `−` and `×` only — `ε = 1e-14`.
         *
         * Calibrated, not guessed. With unit roundoff `u = 2⁻⁵³ ≈ 1.11e-16`:
         *
         * - **Product associativity.** `(a·b)·c` accumulates relative error
         *   `≤ 2u + O(u²)`; results lie in `[0,1]`, so absolute error `≤ 2.2e-16`
         *   per side, `≤ 4.4e-16` between the two sides.
         * - **Łukasiewicz associativity.** `max(0, max(0, a+b−1) + c − 1)`
         *   involves four roundings on operands bounded by `2`, giving absolute
         *   error `≤ 4u ≈ 4.4e-16` per side, `≤ 8.9e-16` between sides.
         *
         * `1e-14` is ~11× the worse of those bounds. Deliberately loose enough
         * to absorb a longer expression than the ones above, and still ~12
         * orders of magnitude tighter than any genuine algebraic failure — a
         * t-norm that is not associative misses by `O(0.01)` or more, never by
         * `1e-13`.
         */
        @JvmField
        public val ARITHMETIC: Tolerance = Tolerance("Arithmetic", 1.0e-14)

        /**
         * For everything else — `ε = 1e-12`.
         *
         * The tier for parametric families and for t-norms this library did not
         * write. Division ([TNorms.hamacher]) and `pow`
         * ([dk.eusrbin.fuzzy.algebra.Negations.yager]) both carry more error than
         * `+`/`−`/`×`, `pow` is not correctly rounded on every platform, and
         * residua obtained by bisection ([TNorm.residuum]) land within an ulp or
         * two of a supremum rather than on it.
         *
         * Looser than [ARITHMETIC] because it is a *default for the unknown*: it
         * should not cry wolf on a correct t-norm that happens to be numerically
         * scruffy. Still far tighter than any real violation.
         */
        @JvmField
        public val GENERAL: Tolerance = Tolerance("General", 1.0e-12)

        /** Alias for [GENERAL] — what you get when nothing is known about the subject. */
        @JvmField
        public val DEFAULT: Tolerance = GENERAL

        /**
         * A tolerance with a custom [epsilon].
         *
         * Provided for t-norms with numerics we cannot anticipate. If you find
         * yourself calling this from a test in *this* repository, that is the
         * signal to add a calibrated constant above instead — CLAUDE.md §8.
         *
         * @throws IllegalArgumentException if [epsilon] is negative or `NaN`.
         */
        @JvmStatic
        @JvmOverloads
        public fun of(epsilon: Double, name: String = "Custom"): Tolerance {
            require(epsilon >= 0.0) { "Tolerance ε must be ≥ 0, but was $epsilon" }
            return Tolerance(name, epsilon)
        }

        /**
         * The calibrated tolerance for [tNorm] — the per-algebra dispatch
         * CLAUDE.md §8 asks for.
         *
         * Dispatches on identity against the built-in singletons, so a t-norm you
         * wrote yourself lands on [GENERAL], which is the conservative answer.
         *
         * [TNorms.DRASTIC] gets [EXACT]: it only ever *returns* an operand, `0`
         * or `1`, and computes nothing. (Its residuum genuinely fails the
         * adjunction — see [TNorms.DRASTIC] — but that is a mathematical fact
         * about left-continuity, not a rounding artefact, and no epsilon should
         * paper over it.)
         */
        @JvmStatic
        public fun forTNorm(tNorm: TNorm): Tolerance = when (tNorm) {
            // Selects an operand; never computes one.
            TNorms.GODEL, TNorms.DRASTIC -> EXACT
            // Closed-form +, −, ×. Nilpotent minimum is included for its `a + b`
            // branch test, though its return values are themselves exact.
            TNorms.PRODUCT, TNorms.LUKASIEWICZ, TNorms.NILPOTENT_MINIMUM -> ARITHMETIC
            // Hamacher, ordinal sums, and anything a consumer wrote.
            else -> GENERAL
        }

        /**
         * The calibrated tolerance for [algebra].
         *
         * [Algebra.STANDARD] is [EXACT] — the whole Zadeh/Gödel tier is exact in
         * IEEE 754. Others defer to [forTNorm], since the conorm and negation of
         * every built-in bundle are arithmetically no worse than the t-norm they
         * are dual to.
         */
        @JvmStatic
        public fun forAlgebra(algebra: Algebra): Tolerance = when (algebra) {
            Algebra.STANDARD -> EXACT
            else -> forTNorm(algebra.tNorm)
        }
    }
}
