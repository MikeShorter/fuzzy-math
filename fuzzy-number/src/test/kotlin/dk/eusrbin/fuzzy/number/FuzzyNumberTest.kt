package dk.eusrbin.fuzzy.number

import dk.eusrbin.fuzzy.set.Enumerable
import dk.eusrbin.fuzzy.set.Sampled
import dk.eusrbin.fuzzy.set.Verdict
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * `fuzzy-number`'s own tests. The law suites live in `fuzzy-laws` and are applied
 * from there; what is pinned here is **the arithmetic** and **the findings §20
 * records** — so that a later edit that quietly re-breaks one fails rather than
 * merely disagreeing with a markdown file.
 */
class FuzzyNumberTest : FunSpec({

    // ---- §20.8: the universe is supplied by the caller ----------------------

    context("height reads the carrier, and does not fold over it (§20.8)") {

        // §15.3's own worked example, and the one that refuted its wording.
        val t = TriangularNumber.of(-0.5, 0.5, 1.5)

        test("the window that broke 'ignores the domain entirely'") {
            // The triangle is identically ZERO on [2,3] — its support is (-0.5, 1.5).
            // The Sup over [2,3] IS 0. An override returning 1.0 here answers a
            // question nobody asked, and §20.7's `override >= fold` lets it through.
            t.height(Sampled.of(2.0, 3.0, 1024)) shouldBe 0.0
            t.height(Sampled.of(-3.0, -2.0, 1024)) shouldBe 0.0
        }

        test("and where the closed form is not merely equal but MORE accurate") {
            // 0.5 is the peak and is not a grid point of Sampled(-1, 2, 1024).
            // The fold can only approach it; the analytic answer reaches it.
            val over = Sampled.of(-1.0, 2.0, 1024)
            val fold = dk.eusrbin.fuzzy.set.DoubleMembershipFn { x -> t.applyAsDouble(x) }.height(over)

            t.height(over) shouldBe 1.0
            fold shouldBe (0.998534 plusOrMinus 1e-5)
            (t.height(over) > fold) shouldBe true // §20.7's `>=`, earning its inequality
        }

        test("a window on the flank takes the nearer endpoint") {
            t.height(Sampled.of(1.0, 2.0, 1024)) shouldBe (0.5 plusOrMinus 1e-12)
        }

        test("over an Enumerable the fold IS the Sup, so the override defers to it") {
            // No analytic shortcut exists here: the universe is the element set,
            // and folding it is not an approximation of anything (§20.8).
            val over = Enumerable.of(0.0, 1.0)
            t.height(over) shouldBe 0.5 // f(0) = f(1) = 0.5 for this triangle
        }
    }

    // ---- §20.3: the product is not a triangle -------------------------------

    context("× ships the exact answer, not the triangle (§20.3)") {

        val x = TriangularNumber.of(1.0, 2.0, 3.0)
        val product = FuzzyNumbers.multiply(x, x)
        val theLieEverybodyShips = TriangularNumber.of(1.0, 4.0, 9.0)

        test("the α-cuts are quadratic, and agree with T(1,4,9) at exactly the two points anyone checks") {
            // α = 0 and α = 1 — the support and the peak. This is why it ships
            // everywhere and why nobody notices.
            for (alpha in listOf(1e-9, 1.0)) {
                val exact = product.alphaCutInterval(alpha)
                val approx = theLieEverybodyShips.alphaCutInterval(alpha)
                abs(exact.lower - approx.lower) shouldBe (0.0 plusOrMinus 1e-8)
                abs(exact.upper - approx.upper) shouldBe (0.0 plusOrMinus 1e-8)
            }
        }

        test("...and are wrong by 0.25 through the whole interior") {
            val exact = product.alphaCutInterval(0.5)
            exact.lower shouldBe (2.25 plusOrMinus 1e-9) // (1 + 0.5)²
            exact.upper shouldBe (6.25 plusOrMinus 1e-9) // (3 − 0.5)²

            val approx = theLieEverybodyShips.alphaCutInterval(0.5)
            approx.lower shouldBe (2.5 plusOrMinus 1e-9)
            abs(exact.lower - approx.lower) shouldBe (0.25 plusOrMinus 1e-9)
        }

        test("the recovered membership function matches the closed form μ(y) = min(√y − 1, 3 − √y)") {
            // §20.3: the exact product's membership function EXISTS and is writable.
            // The bisection is recovering a real function, not manufacturing one.
            fun closedForm(y: Double): Double = minOf(sqrt(y) - 1.0, 3.0 - sqrt(y)).coerceIn(0.0, 1.0)

            for (y in listOf(1.5, 2.25, 4.0, 6.25, 8.5)) {
                product.applyAsDouble(y) shouldBe (closedForm(y) plusOrMinus 1e-6)
            }
            product.applyAsDouble(4.0) shouldBe (1.0 plusOrMinus 1e-6)
        }

        test("and it disagrees with the triangle where it counts") {
            // μ(2.25): exact 0.5, triangle 0.4167. Both look plausible; one is right.
            product.applyAsDouble(2.25) shouldBe (0.5 plusOrMinus 1e-6)
            theLieEverybodyShips.applyAsDouble(2.25) shouldBe (0.4167 plusOrMinus 1e-3)
        }

        test("the exact product is still a fuzzy number — its cuts are nested") {
            var previous = product.alphaCutInterval(1e-9)
            for (i in 1..100) {
                val cut = product.alphaCutInterval(i / 100.0)
                (cut in previous) shouldBe true
                previous = cut
            }
        }
    }

    // ---- §20.4: X ⊖ X is not crisp zero -------------------------------------

    test("X − X is a fuzzy number spread about zero, not {0} (§20.4)") {
        val x = TriangularNumber.of(1.0, 2.0, 3.0)
        val difference = FuzzyNumbers.subtract(x, x)

        // The first thing a user will report as a bug. α-cut arithmetic has no
        // cancellation: the two X's are independent quantities that happen to
        // share a range.
        difference shouldBe TriangularNumber.of(-2.0, 0.0, 2.0)
        difference.alphaCutInterval(1e-9).width shouldBe (4.0 plusOrMinus 1e-8)
        difference.alphaCutInterval(0.5).width shouldBe (2.0 plusOrMinus 1e-9)
        difference.alphaCutInterval(1.0).width shouldBe (0.0 plusOrMinus 1e-9) // only HERE is it {0}
    }

    // ---- §20.1(b): the type that proves it was not pedantry ------------------

    test("a trapezoid's core is an interval, which no List<Double> holds (§20.1)") {
        val tz = TrapezoidalNumber.of(1.0, 2.0, 4.0, 5.0)

        tz.coreInterval() shouldBe Interval.of(2.0, 4.0)
        tz.coreInterval().isDegenerate shouldBe false

        // `core(over)` asks which GRID POINTS have f = 1; `coreInterval()` asks
        // where f = 1 IS. Two questions (§18.3), and the trapezoid is what makes
        // the difference impossible to paper over.
        tz.core(Sampled.of(0.0, 6.0, 7)).size shouldBe 3 // {2, 3, 4} — a sample of [2, 4]
    }

    // ---- §20.5: Proven from a closed-form proof -----------------------------

    context("the dividend: Proven from a proof, not from exhaustion (§20.5)") {

        val line = Sampled.of(-5.0, 5.0, 512)

        test("a triangle is convex by construction, over a Sampled domain") {
            // Verdict.Proven means "a proof exists", not "the domain was
            // exhaustive". Exhaustive enumeration was only ever ONE way to have one.
            TriangularNumber.of(1.0, 2.0, 3.0)
                .findNonConvexity(line)
                .shouldBeInstanceOf<Verdict.Proven<*>>()
        }

        test("but a triangle is NOT strongly convex — the flat tails refute it") {
            val verdict = TriangularNumber.of(1.0, 2.0, 3.0).findNonStrongConvexity(line)
            val refuted = verdict.shouldBeInstanceOf<Verdict.Refuted<dk.eusrbin.fuzzy.set.ConvexityWitness>>()

            // The witness must actually witness: two points outside the support,
            // both f = 0, and 0 > 0 is false.
            refuted.witness.atSegment shouldBe 0.0
            refuted.witness.minEndpoints shouldBe 0.0
        }

        test("while a Gaussian IS strongly convex — it is flat nowhere") {
            // The contrast is the point: both answers are closed-form, neither
            // searches, and they differ. Strong convexity's universe is ℝ — fixed
            // by the operation — so both may ignore the domain (§20.8).
            GaussianNumber.of(0.0, 1.0)
                .findNonStrongConvexity(line)
                .shouldBeInstanceOf<Verdict.Proven<*>>()
        }
    }

    // ---- §0: fuzzy is not probability ---------------------------------------

    test("Gaussian spreads add LINEARLY, not in quadrature (§0)") {
        val sum = FuzzyNumbers.add(GaussianNumber.of(1.0, 2.0), GaussianNumber.of(3.0, 4.0))

        sum shouldBe GaussianNumber.of(4.0, 6.0) // σ = σ₁ + σ₂
        sum shouldNotBe GaussianNumber.of(4.0, sqrt(20.0)) // σ = √(σ₁² + σ₂²) = 4.47

        // A 34% disagreement, and neither is a rounding error. §0: "the notion of
        // a fuzzy set is completely non-statistical in nature."
        (sum as GaussianNumber).sigma shouldBe (6.0 plusOrMinus 1e-12)
    }

    // ---- closure, verified rather than assumed ------------------------------

    test("+ and − are closed in-family, and the returned family is the exact answer") {
        val a = TriangularNumber.of(1.0, 2.0, 4.0)
        val b = TriangularNumber.of(0.0, 1.0, 2.0)
        val sum = FuzzyNumbers.add(a, b)

        // In-family is not an optimisation — it must agree cut-wise with the
        // general form, or the fast path is a lie.
        for (alpha in listOf(1e-9, 0.25, 0.5, 0.75, 1.0)) {
            val general = a.alphaCutInterval(alpha) + b.alphaCutInterval(alpha)
            sum.alphaCutInterval(alpha).lower shouldBe (general.lower plusOrMinus 1e-9)
            sum.alphaCutInterval(alpha).upper shouldBe (general.upper plusOrMinus 1e-9)
        }
        sum shouldBe TriangularNumber.of(1.0, 3.0, 6.0)
    }

    test("subtraction crosses the feet, and a trapezoid stays ordered") {
        // a ≤ b ≤ c ≤ d must survive [a₁−d₂, b₁−c₂, c₁−b₂, d₁−a₂], or `of` throws.
        val result = FuzzyNumbers.subtract(
            TrapezoidalNumber.of(1.0, 2.0, 4.0, 5.0),
            TrapezoidalNumber.of(0.0, 1.0, 2.0, 3.0),
        )
        result shouldBe TrapezoidalNumber.of(-2.0, 0.0, 3.0, 5.0)
    }

    test("a negative scale mirrors, so the triangular feet swap") {
        FuzzyNumbers.scale(TriangularNumber.of(1.0, 2.0, 3.0), -2.0) shouldBe
            TriangularNumber.of(-6.0, -4.0, -2.0)
    }
})
