package dk.eusrbin.fuzzy.laws

import dk.eusrbin.fuzzy.set.ConvexityWitness
import dk.eusrbin.fuzzy.set.Domain
import dk.eusrbin.fuzzy.set.DoubleMembershipFn
import dk.eusrbin.fuzzy.set.Enumerable
import dk.eusrbin.fuzzy.set.FuzzySets
import dk.eusrbin.fuzzy.set.MembershipFn
import dk.eusrbin.fuzzy.set.Product
import dk.eusrbin.fuzzy.set.Sampled
import dk.eusrbin.fuzzy.set.Verdict
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max

/**
 * Zadeh §V, pinned — and the §V theorems tested the only way they honestly can
 * be.
 *
 * CLAUDE.md §19.7: **three of §V's four theorems cannot ship as law suites**,
 * because each is conditional on convexity and a grid never *proves* convexity —
 * so a failed consequent indicts the sampler, not the code. They are tested here
 * instead, against fixtures whose convexity is known **by construction** rather
 * than by sampling. The premise is then a fact, and the theorem is a real check.
 *
 * The fixtures are chosen to separate the three properties:
 *
 * | | convex | strongly convex |
 * |---|---|---|
 * | [triangle] `1 − \|x−1\|` | ✔ | ✘ — flat zero tails, so `>` fails |
 * | [gaussian] `exp(−x²)` | ✔ | ✔ — strictly quasi-concave, never flat |
 * | [bimodal] two humps | ✘ | ✘ — Zadeh's Fig. 4, right-hand side |
 */
class ZadehConvexityTest : FunSpec({

    // ---- Fixtures: convexity known by construction, not by sampling --------

    /** Convex (α-cuts are intervals); NOT strongly convex — the tails are flat at 0. */
    val triangle = DoubleMembershipFn { x -> max(0.0, 1.0 - abs(x - 1.0)) }

    /** Convex AND strongly convex — strictly quasi-concave, flat nowhere. */
    val gaussian = DoubleMembershipFn { x -> exp(-x * x) }

    /** Zadeh's Fig. 4, right: two humps at 0 and 2, a valley between. Non-convex. */
    val bimodal = DoubleMembershipFn { x ->
        max(exp(-4.0 * x * x), exp(-4.0 * (x - 2.0) * (x - 2.0)))
    }

    val line: Domain<Double> = Sampled.of(-1.0, 3.0, 5)
    val fine: Domain<Double> = Sampled.of(-1.0, 3.0, 257)

    // ---- eq. (25): convexity ------------------------------------------------

    context("eq. (25), p.347 — convexity") {

        test("the triangle and the Gaussian are not refuted; the bimodal set is") {
            (triangle.findNonConvexity(line) is Verdict.NotRefuted) shouldBe true
            (gaussian.findNonConvexity(line) is Verdict.NotRefuted) shouldBe true
            (bimodal.findNonConvexity(line) is Verdict.Refuted) shouldBe true
        }

        test("the bimodal witness is Zadeh's Fig. 4: a valley between two peaks") {
            val witness = (bimodal.findNonConvexity(line) as Verdict.Refuted).witness
            withClue(witness) {
                // Both endpoints sit high, the segment between them dips low.
                witness.minEndpoints shouldBe (1.0 plusOrMinus 1e-9)
                (witness.atSegment < witness.minEndpoints) shouldBe true
                (witness.shortfall > 0.0) shouldBe true
                // ... and it reproduces: f_A at the reported point IS atSegment.
                bimodal.applyAsDouble(witness.at) shouldBe witness.atSegment
            }
        }

        test("a convex fuzzy set is a CONCAVE function — Zadeh's own warning") {
            // p.347: "this definition does not imply that f_A(x) must be a convex
            // function of x." The triangle is convex as a fuzzy set and visibly
            // concave as a graph. "Convex" qualifies the SET, via its α-cuts.
            (triangle.findNonConvexity(fine) is Verdict.NotRefuted) shouldBe true
            // Concave as a function: the midpoint sits ABOVE the chord.
            val chordMidpoint = (triangle.applyAsDouble(0.0) + triangle.applyAsDouble(1.0)) / 2.0
            triangle.applyAsDouble(0.5) shouldBe (0.5 plusOrMinus 1e-12)
            (triangle.applyAsDouble(0.5) > chordMidpoint) shouldBe false // equal here: it is piecewise linear
            triangle.applyAsDouble(0.5) shouldBe (chordMidpoint plusOrMinus 1e-12)
        }

        test("interpolation is x₂ + λ(x₁ − x₂), and the textbook form would be WRONG") {
            // The bug this test exists for: at x₁ = x₂ the two forms differ, and
            // eq. (25) is trivially true there — so the textbook form refutes a
            // convex set. See CLAUDE.md §19.7.
            val naive = 0.2 * 3.0 + 0.8 * 3.0
            val lerp = 3.0 + 0.2 * (3.0 - 3.0)
            naive shouldBe 3.0000000000000004
            lerp shouldBe 3.0
            (naive == lerp) shouldBe false

            // The Gaussian is convex; with the naive form it was refuted at
            // (3.0, 3.0, 0.2), because f(3.0000000000000004) < f(3.0).
            (gaussian.applyAsDouble(naive) < gaussian.applyAsDouble(3.0)) shouldBe true
            (gaussian.findNonConvexity(line) is Verdict.NotRefuted) shouldBe true
        }
    }

    // ---- p.349: strong convexity -------------------------------------------

    context("p.349 — strong convexity") {

        test("the Gaussian is strongly convex; the triangle is NOT — its tails are flat") {
            (gaussian.findNonStrongConvexity(line) is Verdict.NotRefuted) shouldBe true
            (triangle.findNonStrongConvexity(line) is Verdict.Refuted) shouldBe true
        }

        test("the triangle's failure is exactly the flat region — `>` cannot hold at 0 = 0") {
            val witness = (triangle.findNonStrongConvexity(line) as Verdict.Refuted).witness
            withClue(witness) {
                // Both endpoints are outside the support, so Min = 0, and the
                // segment between them is also 0. Strong convexity wants > 0.
                witness.minEndpoints shouldBe 0.0
                witness.atSegment shouldBe 0.0
            }
        }

        test("strong ⟹ convex, on every fixture (ConvexityLaws' law 1)") {
            for (fn in listOf(triangle, gaussian, bimodal)) {
                val convex = fn.findNonConvexity(line)
                val strong = fn.findNonStrongConvexity(line)
                withClue("$fn: convex=$convex strong=$strong") {
                    if (convex is Verdict.Refuted) (strong is Verdict.Refuted) shouldBe true
                }
            }
        }
    }

    // ---- The §V theorems, against fixtures we KNOW are convex (§19.7) -------

    context("§V's theorems — premise known by construction, not sampled") {

        test("p.347: 'If A and B are convex, so is their intersection'") {
            // Both premises are facts about the fixtures, not sampler output.
            val intersection = FuzzySets.intersection(triangle, gaussian)
            val asDouble = DoubleMembershipFn { x -> intersection.apply(x) }
            (asDouble.findNonConvexity(fine) is Verdict.NotRefuted) shouldBe true
        }

        test("p.347, contrapositive: intersecting with a non-convex set can break it") {
            // Not a violation of the theorem — its premise simply fails.
            val intersection = FuzzySets.intersection(gaussian, bimodal)
            val asDouble = DoubleMembershipFn { x -> intersection.apply(x) }
            (asDouble.findNonConvexity(fine) is Verdict.Refuted) shouldBe true
        }

        test("... and the COARSE grid misses it — §19.7's argument, as a fact") {
            // The same set. Only the grid changes.
            //
            // min(gaussian, bimodal) is genuinely non-convex: it dips near x ≈ 0.9
            // where the Gaussian is still falling and the bimodal set's valley has
            // not yet recovered. A 5-point grid steps straight over the dip and
            // reports NotRefuted; 17 points find it.
            //
            // This is why §19.7 will not ship p.347's theorem as a law. A suite
            // checking "convex ∩ convex is convex" would have to read NotRefuted as
            // "convex", and NotRefuted is a fact about the SAMPLER, never about the
            // set. Here it says the opposite of the truth.
            val intersection = FuzzySets.intersection(gaussian, bimodal)
            val asDouble = DoubleMembershipFn { x -> intersection.apply(x) }

            val coarse: Domain<Double> = Sampled.of(-1.0, 3.0, 5)
            val enough: Domain<Double> = Sampled.of(-1.0, 3.0, 17)

            (asDouble.findNonConvexity(coarse) is Verdict.NotRefuted) shouldBe true
            (asDouble.findNonConvexity(enough) is Verdict.Refuted) shouldBe true

            // And the witness the finer grid finds is real — it reproduces.
            val witness = (asDouble.findNonConvexity(enough) as Verdict.Refuted).witness
            withClue(witness) {
                asDouble.applyAsDouble(witness.at) shouldBe witness.atSegment
                (witness.atSegment < witness.minEndpoints) shouldBe true
            }

            // §15.1's payoff, concretely: "the same set, analysed over different
            // domains" is one expression. §3's shape made this a type error.
        }

        test("p.349: 'If A is a convex fuzzy set, then its core is a convex set'") {
            // Zadeh's "core" is C(A) — our maximalGradeSet (§18.3), not core().
            // Over ℝ¹ a convex set is an interval, so on a grid its points must be
            // contiguous.
            for (fn in listOf(triangle, gaussian)) {
                val points = fn.maximalGradeSet(fine)
                val all = fine.elements()
                val indices = points.map { all.indexOf(it) }
                withClue("$fn maximalGradeSet = $points") {
                    indices.zipWithNext().all { (a, b) -> b == a + 1 } shouldBe true
                }
            }
        }

        test("p.350's corollary is NOT checkable on a grid — and this shows why (§19.7)") {
            // "If X = E¹ and A is strongly convex, then the point at which M is
            // essentially attained is unique."
            //
            // A triangle peaked BETWEEN two grid points has two points tied for the
            // maximal grade. The corollary reads false; the set is impeccably
            // strongly convex. The culprit is "essentially attained" — the
            // topological adverb §18.3 had to drop.
            val offGrid = DoubleMembershipFn { x -> max(0.0, 1.0 - abs(x - 0.5)) }
            val coarse: Domain<Double> = Sampled.of(0.0, 1.0, 2) // points at 0.0 and 1.0 only
            offGrid.maximalGradeSet(coarse).size shouldBe 2 // ... yet its true peak is unique.
            offGrid.applyAsDouble(0.5) shouldBe 1.0 // the real maximum, invisible to this grid
        }

        test("p.352: D = 1 − M, on bounded convex sets") {
            // Two triangles that do not overlap at all: M = 0, so D = 1.
            val left = DoubleMembershipFn { x -> max(0.0, 1.0 - abs(x + 1.0)) }
            val right = DoubleMembershipFn { x -> max(0.0, 1.0 - abs(x - 2.0)) }
            left.separationDegree(right, fine) shouldBe (1.0 plusOrMinus 1e-12)

            // A set with itself: M = 1, so D = 0. Nothing separates a set from itself.
            triangle.separationDegree(triangle, fine) shouldBe (0.0 plusOrMinus 1e-12)
        }

        test("separationDegree returns a NUMBER for non-convex sets — and it is not D") {
            // §19.3: this computes p.352's theorem, whose premise is bounded
            // convexity. Hand it a non-convex set and nothing complains — which is
            // exactly why the precondition is in the KDoc and findNonConvexity is
            // named there.
            val d = bimodal.separationDegree(gaussian, line)
            (d in 0.0..1.0) shouldBe true // plausible, well-defined, and meaningless
            (bimodal.findNonConvexity(line) is Verdict.Refuted) shouldBe true // premise fails
        }
    }

    // ---- ConvexityLaws' own test-of-the-test (§7) ---------------------------

    context("ConvexityLaws must reject a lying override (§7)") {

        test("honest fixtures pass") {
            for (fn in listOf(triangle, gaussian, bimodal)) {
                withClue(fn) { ConvexityLaws.verify(fn, line) }
            }
        }

        test("an override denying a witness the fold found is caught") {
            // The bimodal set IS refutable — the generic search finds the valley.
            // An override claiming otherwise is lying, and a witness is absolute.
            val liar = object : DoubleMembershipFn {
                override fun applyAsDouble(x: Double): Double = bimodal.applyAsDouble(x)

                override fun findNonConvexity(
                    over: Domain<Double>,
                    weights: DoubleArray,
                ): Verdict<ConvexityWitness> = Verdict.NotRefuted()
            }
            val report = ConvexityLaws.check(liar, line)
            report.holds shouldBe false
            report.failures.map { it.law }.any { it.contains("may not deny a witness") } shouldBe true
        }

        test("a sampled ∀ must never come back Proven") {
            val overclaimer = object : DoubleMembershipFn {
                override fun applyAsDouble(x: Double): Double = gaussian.applyAsDouble(x)

                override fun findNonConvexity(
                    over: Domain<Double>,
                    weights: DoubleArray,
                ): Verdict<ConvexityWitness> = Verdict.Proven()
            }
            val report = ConvexityLaws.check(overclaimer, line)
            report.holds shouldBe false
            report.failures.map { it.law }.any { it.contains("never Proven") } shouldBe true
        }
    }

    // ---- §19.2: the shadow is domain-generic --------------------------------

    context("shadow — p.350, and it needs no vector space (§19.2)") {

        test("projecting a separable set recovers its other factor") {
            // a(x, y) = f(x)·g(y)  ⟹  Sup_x a(x,y) = g(y)·Sup_x f(x) = g(y)·height(f).
            // The Gaussian's height is 1 on this grid (x = 0 is a point), so the
            // shadow IS g.
            val g = DoubleMembershipFn { y -> max(0.0, 1.0 - abs(y - 1.0)) }
            val joint: MembershipFn<Pair<Double, Double>> =
                MembershipFn { (x, y) -> gaussian.applyAsDouble(x) * g.applyAsDouble(y) }

            gaussian.height(fine) shouldBe 1.0
            val projected = FuzzySets.shadow(joint, fine)
            for (y in listOf(0.0, 0.5, 1.0, 1.5, 2.0)) {
                withClue("y = $y") { projected.apply(y) shouldBe (g.applyAsDouble(y) plusOrMinus 1e-12) }
            }
        }

        test("it works over an Enumerable factor too — no ℝ anywhere") {
            // §19.2's point: the shadow is a Sup over one coordinate. Nothing in it
            // needs arithmetic on X, so X need not be ℝ.
            val terms = Enumerable.of("cold", "warm", "hot")
            val joint: MembershipFn<Pair<String, Int>> = MembershipFn { (term, n) ->
                when (term) { "cold" -> 0.1; "warm" -> 0.6; else -> 0.9 } / n
            }
            val onN = FuzzySets.shadow(joint, terms)
            onN.apply(1) shouldBe (0.9 plusOrMinus 1e-12) // Sup over terms at n=1
            onN.apply(3) shouldBe (0.3 plusOrMinus 1e-12) // 0.9/3
        }

        test("a shadow is a fuzzy set on the surviving factor, and Product folds it") {
            val terms = Enumerable.of("a", "b")
            val joint: MembershipFn<Pair<String, String>> =
                MembershipFn { (p, q) -> if (p == q) 1.0 else 0.25 }
            val product = Product.of(terms, terms)
            product.isExhaustive shouldBe true

            val shadow = FuzzySets.shadow(joint, terms)
            shadow.apply("a") shouldBe 1.0
            // ... and the shadow of a set is contained in nothing stronger than 1.
            shadow.isContainedIn(FuzzySets.constant(1.0), terms) shouldBe true
        }
    }
})
