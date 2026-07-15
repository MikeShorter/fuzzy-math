package dk.eusrbin.fuzzy.laws

import dk.eusrbin.fuzzy.set.Domain
import dk.eusrbin.fuzzy.set.DoubleMembershipFn
import dk.eusrbin.fuzzy.set.Enumerable
import dk.eusrbin.fuzzy.set.MembershipFn
import dk.eusrbin.fuzzy.set.Sampled
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max

/**
 * Fixtures for the `fuzzy-set` suites — see [Generators] for why kotest-property
 * is test-scope only.
 *
 * The subjects here are chosen to *separate* things, not to be representative:
 * a term set where the ∀ is a proof, a grid where it is not, a set that is
 * normal and one that is not, and a function whose closed forms are right beside
 * one whose are wrong.
 */
internal object SetGenerators {

    // ---- Enumerable: a linguistic term set. The ∀ here is a proof. ---------

    /** Zadeh's kind of finite X — Bergmann §16.1's `tall` lives on one of these. */
    val terms: Enumerable<String> = Enumerable.of("cold", "cool", "mild", "warm", "hot")

    /** Normal: reaches 1.0 at "hot". */
    val warm: MembershipFn<String> = MembershipFn { x ->
        when (x) {
            "cold" -> 0.0; "cool" -> 0.25; "mild" -> 0.5; "warm" -> 0.75; else -> 1.0
        }
    }

    /** Normal: reaches 1.0 at "cold". Deliberately the reverse of [warm]. */
    val cool: MembershipFn<String> = MembershipFn { x ->
        when (x) {
            "cold" -> 1.0; "cool" -> 0.75; "mild" -> 0.5; "warm" -> 0.25; else -> 0.0
        }
    }

    /** A third, independent set — eqs. (9)/(10) need a `C`. */
    val mild: MembershipFn<String> = MembershipFn { x ->
        when (x) {
            "cold" -> 0.1; "cool" -> 0.4; "mild" -> 0.9; "warm" -> 0.4; else -> 0.1
        }
    }

    /**
     * **Subnormal** — tops out at 0.6. The set that separates `core` from
     * `maximalGradeSet` (CLAUDE.md §18.3): its `core` is empty, its
     * `maximalGradeSet` is not.
     */
    val subnormal: MembershipFn<String> = MembershipFn { x ->
        when (x) {
            "cold" -> 0.0; "cool" -> 0.2; "mild" -> 0.6; "warm" -> 0.6; else -> 0.3
        }
    }

    // ---- Sampled: a grid on ℝ. The ∀ here is only a search. ----------------

    /** A grid whose points include −1, 0, 1, 2, 3 exactly — peaks land on it. */
    val line: Sampled = Sampled.of(-1.0, 3.0, 5)

    /** A finer grid, for watching answers converge (§15.1's point). */
    val fineLine: Sampled = Sampled.of(-1.0, 3.0, 4097)

    /** A Gaussian, primitive throughout. Normal: `exp(0) = 1` at `x = 0`, a grid point. */
    val nearZero: DoubleMembershipFn = DoubleMembershipFn { x -> exp(-x * x) }

    /** A triangle peaked at 1.0, zero outside [0,2] — `1 − |x − 1|`, clamped. */
    val triangle: DoubleMembershipFn = DoubleMembershipFn { x -> max(0.0, 1.0 - abs(x - 1.0)) }

    // ---- Subjects for MembershipFnLaws' own test-of-the-test ---------------

    /**
     * A triangle that **overrides its analysis correctly** — §15.3's intended use.
     *
     * `1 − |x − 1|` peaks at `x = 1` with height 1; both [line] and [fineLine]
     * contain `1.0` exactly, so the analytic answer and the fold agree.
     */
    class HonestTriangle : DoubleMembershipFn {
        override fun applyAsDouble(x: Double): Double = max(0.0, 1.0 - abs(x - 1.0))

        /** Analytic: the peak is 1.0, and the domain is irrelevant (§15.3, verbatim). */
        override fun height(over: Domain<Double>): Double = 1.0
    }

    /**
     * The same triangle with a **wrong** closed form. Nothing but
     * [MembershipFnLaws] can catch this: `0.5` is a perfectly plausible degree.
     */
    class LyingTriangle : DoubleMembershipFn {
        override fun applyAsDouble(x: Double): Double = max(0.0, 1.0 - abs(x - 1.0))

        override fun height(over: Domain<Double>): Double = 0.5
    }

    /**
     * A set whose `alphaCut` override silently drops a point — the subtler lie,
     * and the reason the override check covers every operation rather than just
     * `height`.
     */
    class ForgetfulTriangle : DoubleMembershipFn {
        override fun applyAsDouble(x: Double): Double = max(0.0, 1.0 - abs(x - 1.0))

        override fun alphaCut(over: Domain<Double>, alpha: Double): List<Double> =
            over.filter { x -> applyAsDouble(x) >= alpha }.drop(1)
    }

    // ---- Arbs ---------------------------------------------------------------

    /** Arbitrary sets over [terms], for property-testing the suites. */
    val termSet: Arb<MembershipFn<String>> = arbitrary { rs ->
        val degrees = terms.elements().associateWith { rs.random.nextDouble() }
        MembershipFn { x -> degrees.getValue(x) }
    }

    /** Arbitrary Λ for eq. (17)/(18) — **a fuzzy set**, not a scalar (§17.2). */
    val termLambda: Arb<MembershipFn<String>> = termSet
}
