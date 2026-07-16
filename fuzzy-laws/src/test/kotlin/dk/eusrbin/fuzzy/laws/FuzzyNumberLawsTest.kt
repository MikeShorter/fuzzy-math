package dk.eusrbin.fuzzy.laws

import dk.eusrbin.fuzzy.number.AlphaCutNumber
import dk.eusrbin.fuzzy.number.FuzzyNumber
import dk.eusrbin.fuzzy.number.FuzzyNumbers
import dk.eusrbin.fuzzy.number.GaussianNumber
import dk.eusrbin.fuzzy.number.Interval
import dk.eusrbin.fuzzy.number.TrapezoidalNumber
import dk.eusrbin.fuzzy.number.TriangularNumber
import dk.eusrbin.fuzzy.set.Domain
import dk.eusrbin.fuzzy.set.DoubleMembershipFn
import dk.eusrbin.fuzzy.set.Enumerable
import dk.eusrbin.fuzzy.set.Sampled
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * **§15.3, made true rather than asserted.**
 *
 * Until `fuzzy-number` existed, nothing in the library overrode anything, so
 * §15.3 — *"closed forms are overrides on the function"* — was an unproven claim
 * and `MembershipFnLaws` was a suite with no subject. These are the tests that
 * settle it, and §7's **test-of-the-test** discipline is why the lying fixtures
 * matter more than the honest ones.
 */
class FuzzyNumberLawsTest : FunSpec({

    val line = Sampled.of(-5.0, 5.0, 512)

    val families: List<Pair<String, FuzzyNumber>> = listOf(
        "triangular" to TriangularNumber.of(-1.0, 0.5, 2.0),
        "trapezoidal" to TrapezoidalNumber.of(-2.0, -0.5, 1.0, 2.5),
        "gaussian" to GaussianNumber.of(0.0, 1.0),
        "symmetric triangle" to TriangularNumber.symmetric(0.0, 2.0),
        "crisp point" to TriangularNumber.crisp(1.0),
        "crisp interval" to TrapezoidalNumber.crispInterval(-1.0, 1.0),
        // What arithmetic returns — quadratic cuts, membership by bisection.
        "the exact product T(1,2,3)²" to FuzzyNumbers.multiply(
            TriangularNumber.of(1.0, 2.0, 3.0),
            TriangularNumber.of(1.0, 2.0, 3.0),
        ),
    )

    context("every shipped family satisfies its own laws") {

        for ((name, n) in families) {
            test("$name — FuzzyNumberLaws") {
                withClue(n) { FuzzyNumberLaws.verify(n, line) }
            }

            // The §15.3 guard: each closed form must agree with the fold it
            // replaced. This is the whole reason MembershipFnLaws exists.
            test("$name — MembershipFnLaws (the §15.3 override guard)") {
                withClue(n) { MembershipFnLaws.verify(n, line) }
            }

            test("$name — ConvexityLaws") {
                withClue(n) { ConvexityLaws.verify(n, line) }
            }
        }
    }

    // A Gaussian's cut endpoints round-trip through √/ln/exp and land at α ± ε —
    // §20.2(iii), predicted before it was met. The suite must absorb that at the
    // boundary without going blind on the interior.
    test("the Gaussian's boundary round-trip does not break the α-cut law (§20.2(iii))") {
        FuzzyNumberLaws.verify(GaussianNumber.of(0.0, 1.0), Sampled.of(-4.0, 4.0, 1024))
    }

    // ---- §7: the test of the test -------------------------------------------

    context("the suites must FAIL where they should (§7)") {

        test("a non-nested cut map is refuted — the precondition AlphaCutNumber cannot check") {
            // §20.2(ii): nestedness is unverifiable at construction but REFUTABLE
            // by sampling, which is why it gets a law rather than a shrug. These
            // cuts GROW with α, the exact opposite of nested.
            val backwards = AlphaCutNumber.of("backwards") { alpha ->
                Interval.of(-alpha, alpha)
            }
            val report = FuzzyNumberLaws.check(backwards, line)
            report.holds shouldBe false
            report.failures.map { it.law }.any { it.contains("nested") } shouldBe true
        }

        test("a cut map nested but inconsistent with f is caught by the normality law") {
            // Nestedness alone does not make a cut map describe THIS function.
            // Γ_1 = [10, 10] is a perfectly nested core that f knows nothing about.
            val displaced = object : FuzzyNumber {
                override fun applyAsDouble(x: Double): Double = GaussianNumber.of(0.0, 1.0).applyAsDouble(x)
                override fun alphaCutInterval(alpha: Double): Interval =
                    Interval.of(10.0 - alpha, 10.0 + alpha)
            }
            val report = FuzzyNumberLaws.check(displaced, line)
            report.holds shouldBe false
            report.failures.map { it.law }.any { it.contains("normal") } shouldBe true
        }

        test("§20.8's bug is caught: a height override that ignores its domain") {
            // THE test for this slice. This is what §15.3 literally said to write —
            // "overrides height with the analytic answer and ignores the domain
            // entirely" — and it is wrong, because height's universe is SUPPLIED
            // BY THE CALLER (§20.8).
            //
            // §20.7's `override >= fold` passes this: 1.0 >= 0.0. It takes the
            // `isExhaustive` tightening to catch it, which is exactly why the law
            // is two-part rather than uniformly `>=`.
            val ignoresTheDomain = object : DoubleMembershipFn {
                private val t = TriangularNumber.of(-0.5, 0.5, 1.5)
                override fun applyAsDouble(x: Double): Double = t.applyAsDouble(x)
                override fun height(over: Domain<Double>): Double = 1.0 // "the analytic answer"
            }

            // Over an Enumerable the fold IS the Sup — not a lower bound on
            // anything — so equality is sound, and it has teeth.
            val over = Enumerable.of(2.0, 3.0) // the triangle is identically 0 here
            val report = MembershipFnLaws.check(ignoresTheDomain, over)
            report.holds shouldBe false
            report.failures.map { it.law }.any { it.contains("exhaustive") } shouldBe true
        }

        test("...and the same override is NOT caught over a Sampled window — no law recovers that") {
            // Recorded because it is the honest limit of §20.7, and the reason
            // §20.8 had to fix the cause rather than the symptom. Over a grid the
            // fold is genuinely a lower bound, so `1.0 >= 0.0` is sound and the
            // wrong answer sails through. The override has to be RIGHT.
            val ignoresTheDomain = object : DoubleMembershipFn {
                private val t = TriangularNumber.of(-0.5, 0.5, 1.5)
                override fun applyAsDouble(x: Double): Double = t.applyAsDouble(x)
                override fun height(over: Domain<Double>): Double = 1.0
            }
            MembershipFnLaws.check(ignoresTheDomain, Sampled.of(2.0, 3.0, 64)).holds shouldBe true

            // Which is why the shipped type reads the carrier instead.
            TriangularNumber.of(-0.5, 0.5, 1.5).height(Sampled.of(2.0, 3.0, 64)) shouldBe 0.0
        }

        test("a height override claiming LESS than the fold found is caught — a witness is absolute") {
            // The half of §20.7 that can never be relaxed. The fold FOUND an x
            // with f(x) = 1.0, so the true Sup is at least 1.0. An override
            // claiming 0.3 is lying about a fact.
            val underclaims = object : DoubleMembershipFn {
                private val t = TriangularNumber.of(-1.0, 0.0, 1.0)
                override fun applyAsDouble(x: Double): Double = t.applyAsDouble(x)
                override fun height(over: Domain<Double>): Double = 0.3
            }
            val report = MembershipFnLaws.check(underclaims, Sampled.of(-2.0, 2.0, 65))
            report.holds shouldBe false
            report.failures.map { it.law }.any { it.contains("≥") } shouldBe true
        }

        test("the exact product must NOT be the triangle everyone ships (§20.3)") {
            // If this ever passes, someone has added an in-family fast path to
            // `multiply` and it is wrong by 0.25 through the interior.
            val product = FuzzyNumbers.multiply(
                TriangularNumber.of(1.0, 2.0, 3.0),
                TriangularNumber.of(1.0, 2.0, 3.0),
            )
            val triangle = TriangularNumber.of(1.0, 4.0, 9.0)
            val report = FuzzyNumberLaws.check(
                object : FuzzyNumber {
                    // f from the exact product, cuts from the triangle: the
                    // approximation, stated as though it were the answer.
                    override fun applyAsDouble(x: Double): Double = product.applyAsDouble(x)
                    override fun alphaCutInterval(alpha: Double): Interval = triangle.alphaCutInterval(alpha)
                },
                Sampled.of(0.0, 10.0, 512),
            )
            report.holds shouldBe false
        }
    }
})
