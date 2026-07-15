package dk.eusrbin.fuzzy.laws

import java.util.Random

/**
 * How thoroughly, and how reproducibly, a law suite samples `[0,1]`.
 *
 * ## Why this is not kotest
 *
 * `fuzzy-laws`' *own* tests are written with kotest-property. This class — the
 * sampling behind the published `check`/`verify` API — is not, and the reason is
 * not taste:
 *
 * 1. **CLAUDE.md §9: "No coroutines anywhere in the public API."**
 *    kotest-property's `checkAll`/`forAll` are `suspend`. Building `verify` on
 *    them would put a coroutine in the call path of every consumer, or bury a
 *    `runBlocking` under a published API. Neither is acceptable under §9.
 * 2. **CLAUDE.md §7 makes this a consumable artifact**, not a test folder: *"A
 *    user writing their own t-norm adds `fuzzy-laws` in test scope and calls
 *    `TNormLaws.verify(myTNorm)`."* That user may be on JUnit, TestNG, or a
 *    Clojure `deftest`. Depending on kotest would conscript them into a test
 *    framework to check a theorem. So `fuzzy-laws`' only published dependency is
 *    `fuzzy-algebra`.
 *
 * What is lost is shrinking — a counterexample here is the first one found, not
 * a minimal one. That cost is small in this specific domain: the inputs are
 * three doubles in `[0,1]`, so there is nothing structural to shrink *to*, and
 * [Counterexample] reports the tuple in full anyway.
 *
 * ## Determinism
 *
 * Sampling is **deterministic by default**: same subject, same report, on every
 * machine and every run. A law failure you cannot reproduce is a rumour. The
 * seeded generator is [java.util.Random], whose algorithm is specified exactly
 * by the JDK, so the sample sequence is identical across JVMs and versions.
 *
 * The pool combines a fixed edge set with [randomDegrees] seeded draws, and
 * every law is checked over the **full cross product** of that pool — so a
 * ternary law such as associativity is checked on `(8 + randomDegrees)³` triples.
 * The edge set is where t-norms actually break, so it is never sampled away:
 *
 * | Value | Why |
 * |---|---|
 * | `0.0`, `1.0` | the boundary conditions `T(a,1) = a`, `T(a,0) = 0` live here |
 * | `0.5` | fixed point of the standard negation; `0.5 + 0.5 = 1` exactly, straddling the Łukasiewicz and nilpotent cutoffs |
 * | `0.25`, `0.75` | sum to `1.0` exactly — the same cutoffs, off-centre |
 * | `ulp(1)` ≈ 2.2e-16 | distinguishes "zero" from "nearly zero" |
 * | `1 − ulp(1)` | largest double below 1: distinguishes "one" from "nearly one", which is exactly where [dk.eusrbin.fuzzy.algebra.TNorms.DRASTIC] changes behaviour |
 * | `1/3` | not representable in binary; forces a rounding on every operation |
 *
 * ## Why `Double.MIN_VALUE` is deliberately NOT in that list
 *
 * It was, and it had to come out. `ulp(1)` is the "nearly zero" probe instead,
 * and the difference is not fussiness.
 *
 * **In the subnormal range, IEEE 754 multiplication is not strictly monotone**,
 * so the floating-point Product t-norm is not the Product t-norm:
 * `4.9e-324 × 0.5` underflows to *exactly* `0.0`. Residuation then fails on its
 * own terms — `T(a, 0.5) ≤ 0` holds, so the adjunction demands
 * `0.5 ≤ (a ⇒ 0) = b/a = 0`, and it does not. No tolerance repairs this; the
 * gap is `0.5`.
 *
 * That is a true statement about `double`, and a useless one about anybody's
 * t-norm. A user checking their own work would be told their arithmetic is
 * broken at `1e-324`, which is not a membership degree, cannot arise from any
 * meaningful computation, and is not their bug. Sampling there manufactures
 * failures the suite exists to rule out.
 *
 * `ulp(1)` keeps the "distinguishes zero from nearly zero" coverage without
 * entering the regime where the arithmetic stops modelling the mathematics.
 * The underflow itself is not swept away — it is documented on
 * [dk.eusrbin.fuzzy.algebra.TNorm.residuum] and pinned by its own test.
 *
 * @property randomDegrees how many seeded random degrees to add to the edge set.
 * @property seed the seed. Fixed by default; vary it to widen coverage across runs.
 */
public class Sampling private constructor(
    public val randomDegrees: Int,
    public val seed: Long,
) {

    /** The degree pool: the edge set, then [randomDegrees] seeded draws. */
    internal fun pool(): DoubleArray {
        val random = Random(seed)
        val result = DoubleArray(EDGE_CASES.size + randomDegrees)
        EDGE_CASES.copyInto(result)
        for (i in 0 until randomDegrees) {
            result[EDGE_CASES.size + i] = random.nextDouble()
        }
        return result
    }

    override fun toString(): String = "Sampling(edge=${EDGE_CASES.size}, random=$randomDegrees, seed=$seed)"

    public companion object {

        /**
         * The degrees every run checks, whatever else it samples.
         *
         * Private and copied on use: a mutable array must never leak out of a
         * public field.
         */
        private val EDGE_CASES: DoubleArray = doubleArrayOf(
            0.0,
            1.0,
            0.5,
            0.25,
            0.75,
            // "Nearly zero" — NOT Double.MIN_VALUE. See the class KDoc: the
            // subnormals are where float multiplication stops being monotone
            // and every t-norm fails residuation for reasons that are not its.
            Math.ulp(1.0),
            1.0 - Math.ulp(1.0),
            1.0 / 3.0,
        )

        /** The default seed. Arbitrary, fixed, and — with 1965 in it — mildly on-topic. */
        public const val DEFAULT_SEED: Long = 0x5EED_1965L

        /**
         * 8 edge cases + 24 random degrees = a pool of 32, i.e. 32 768 triples
         * per ternary law.
         *
         * Cheap enough to be the default (these are primitive `double`
         * operations, and the whole suite runs in milliseconds) and wide enough
         * that a genuine violation has nowhere to hide — a t-norm that fails
         * associativity fails it on a positive-measure set, not at a point.
         */
        @JvmField
        public val DEFAULT: Sampling = Sampling(randomDegrees = 24, seed = DEFAULT_SEED)

        /**
         * A custom sampling.
         *
         * Cost is cubic in `8 + randomDegrees` for ternary laws — 24 is the
         * default; 100 is 1.4 million triples per law and still fast; 1000 is
         * a billion and is not.
         *
         * @throws IllegalArgumentException if [randomDegrees] is negative.
         */
        @JvmStatic
        @JvmOverloads
        public fun of(randomDegrees: Int, seed: Long = DEFAULT_SEED): Sampling {
            require(randomDegrees >= 0) { "randomDegrees must be ≥ 0, but was $randomDegrees" }
            return Sampling(randomDegrees, seed)
        }

        // There is deliberately no `interior()` factory dropping the edge cases.
        //
        // One existed briefly, to let a Yager-based De Morgan triple be checked
        // "away from the boundary where it is ill-conditioned". That premise was
        // wrong: Yager's involutivity error grows like `a^(1−w)` — a gradient
        // with no floor, not a cliff at the edge — so interior sampling does not
        // scope the problem, it only relocates it, and whether you notice comes
        // down to which random draws you got. See Negations.yager.
        //
        // The general point is worth more than the case. Public API added to
        // stop a suite reporting something true is not scoping, it is
        // suppression with a nicer name. If a law fails, either the subject is
        // wrong or the claim is; narrowing the sample answers neither.
    }
}
