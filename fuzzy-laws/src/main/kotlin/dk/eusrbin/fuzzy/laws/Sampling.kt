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
 * | `Double.MIN_VALUE` | smallest positive subnormal: distinguishes "zero" from "nearly zero" |
 * | `1 − ulp(1)` | largest double below 1: distinguishes "one" from "nearly one", which is exactly where [dk.eusrbin.fuzzy.algebra.TNorms.DRASTIC] changes behaviour |
 * | `1/3` | not representable in binary; forces a rounding on every operation |
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
            Double.MIN_VALUE,
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
    }
}
