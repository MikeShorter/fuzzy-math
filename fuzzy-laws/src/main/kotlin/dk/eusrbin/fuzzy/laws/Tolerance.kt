package dk.eusrbin.fuzzy.laws

import dk.eusrbin.fuzzy.algebra.Algebra
import dk.eusrbin.fuzzy.algebra.Negation
import dk.eusrbin.fuzzy.algebra.Negations
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
 * ## Calibration follows the OPERATIONS a suite uses, not the algebra's name
 *
 * The subtlety that matters, and the one that first shipped wrong here.
 *
 * CLAUDE.md §8 says min/max are exactly associative and idempotent, so "the
 * Zadeh tier is safe with exact equality". True — and it is a claim about
 * **min/max**, not about [Algebra.STANDARD]. The Standard algebra also contains
 * the negation `1 − x`, and **`1 − x` is not exactly involutive in IEEE 754**:
 *
 * ```
 * a = 1/3          1 − a = 0.6666666666666667   (rounded — a tie, broken to even)
 * 1 − (1 − a) = 0.33333333333333326  ≠  0.3333333333333333   (Δ = 5.55e-17)
 * ```
 *
 * So [EXACT] is right for [StandardLaws] — idempotence, distributivity and
 * absorption touch nothing but min/max — and *wrong* for [DeMorganLaws] and
 * [MVAlgebraLaws], which lean on `N(N(a)) = a`. Same algebra, different suites,
 * different correct epsilon.
 *
 * Hence [forTNorm] and [forAlgebra] calibrate the **lattice/monoid** side, while
 * [forNegation] calibrates the negation, and suites that use both combine them
 * with [looserOf]. Reading a tolerance off the algebra alone is exactly the
 * mistake that made a correct implementation of Zadeh's own complement look
 * like a bug.
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
         * Correct for **min/max and nothing else**. CLAUDE.md §8: *"min/max are
         * exactly associative and idempotent → the Zadeh tier is safe with exact
         * equality."*
         *
         * This is not laxity avoided, it is information gained: if
         * `StandardLaws.check(Algebra.STANDARD)` ever needed an epsilon, `min`
         * or `max` would have stopped being `min` or `max`.
         *
         * Read the scope narrowly. This applies to the lattice operations, which
         * *select* an operand and never *compute* one. It does **not** extend to
         * the Standard algebra's negation — see the class KDoc on why `1 − x` is
         * not exactly involutive, and [forNegation].
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
         * The calibrated tolerance for [algebra]'s **t-norm and t-conorm**.
         *
         * [Algebra.STANDARD] is [EXACT]: min and max select an operand rather
         * than computing one, so nothing rounds.
         *
         * **This does not cover the algebra's negation.** A suite that touches
         * `N` must widen with [forNegation] — see the class KDoc, and note that
         * `Algebra.STANDARD`'s own `1 − x` fails involutivity at [EXACT]. The
         * name says `forAlgebra` because an algebra is what you have in hand;
         * what it calibrates is the monoid side.
         */
        @JvmStatic
        public fun forAlgebra(algebra: Algebra): Tolerance = when (algebra) {
            Algebra.STANDARD -> EXACT
            else -> forTNorm(algebra.tNorm)
        }

        /**
         * The calibrated tolerance for [negation].
         *
         * Never [EXACT], and [Negations.STANDARD] is the reason why. `1 − x`
         * looks as exact as `min` and is not: for `a ∈ [0, 0.5)` the subtraction
         * rounds, so `1 − (1 − a) = a − δ`. The round trip is off by up to half
         * an ulp of 1 — `5.55e-17`, comfortably inside [ARITHMETIC], and
         * comfortably outside [EXACT].
         *
         * The parametric families divide ([Negations.sugeno]) or take logs and
         * exponentials ([Negations.yager]), so they get [GENERAL].
         */
        @JvmStatic
        public fun forNegation(negation: Negation): Tolerance = when (negation) {
            // 1 − x: arithmetic, not lattice selection. Involutive only to
            // within a rounding of the subtraction.
            Negations.STANDARD -> ARITHMETIC
            // Sugeno divides; Yager goes through ln/expm1. Neither is worse than
            // GENERAL once Yager is evaluated in a well-conditioned form.
            else -> GENERAL
        }

        /**
         * The looser of two tolerances.
         *
         * For suites spanning operations with different numerics — [DeMorganLaws]
         * relates a t-norm, a conorm and a negation in one equation, and the
         * result can be no more exact than its sloppiest ingredient. Taking the
         * looser is the only sound combination: the alternative is holding an
         * expression to a standard one of its own terms cannot meet.
         */
        @JvmStatic
        public fun looserOf(a: Tolerance, b: Tolerance): Tolerance =
            if (a.epsilon >= b.epsilon) a else b
    }
}
