package dk.eusrbin.fuzzy.laws

import dk.eusrbin.fuzzy.defuzz.Defuzzifiers
import dk.eusrbin.fuzzy.number.TriangularNumber
import dk.eusrbin.fuzzy.set.Sampled
import dk.eusrbin.fuzzy.set.Verdict
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * `fuzzy-defuzz`'s central facts, pinned where `fuzzy-number` and
 * `fuzzy-defuzz` are both on the path — the accretion §10 anticipated, paying
 * off (CLAUDE.md §22.6). There is no `DefuzzLaws` suite to test, and that is a
 * decision (§22.6: no subject exists); these are the executable facts the
 * brief demanded instead of prose.
 */
class DefuzzTest : FunSpec({

    context("§22.3: the refusal, and what its wording may honestly claim") {

        test("a triangle wholly off the window: centroid throws — §20.8's own example, §7's central fact") {
            val triangle = TriangularNumber.of(-0.5, 0.5, 1.5)
            val offWindow = Sampled.of(2.0, 3.0, 101)
            val exception = shouldThrow<IllegalArgumentException> {
                Defuzzifiers.centroid(triangle, offWindow)
            }
            exception.message.shouldContain("no mass found")
        }

        test("the spike: height says 1.0, checkEmptiness says NotRefuted, centroid refuses — three answers, none lying") {
            // Narrower than the grid spacing: the set HAS mass on the window,
            // and the fold cannot see it. This is why the exception reports
            // what was DONE ("no mass found"), never "no mass" (§22.3).
            val spike = TriangularNumber.of(0.5000001, 0.5000002, 0.5000003)
            val window = Sampled.of(0.0, 1.0, 1024)

            withClue("§20.8's override knows the true supremum") {
                spike.height(window) shouldBe 1.0
            }
            withClue("the shipped emptiness check declines to assert emptiness over a grid") {
                spike.checkEmptiness(window)
                    .shouldBeInstanceOf<Verdict.NotRefuted<Double>>()
            }
            withClue("and the centroid refuses on its negative finding rather than inventing a point") {
                shouldThrow<IllegalArgumentException> {
                    Defuzzifiers.centroid(spike, window)
                }.message.shouldContain("no mass found")
            }
        }
    }

    context("§22.2: the §20.9 case — the record's prediction, executable") {

        val triangle = TriangularNumber.of(-1.0, 0.5, 2.0)
        val grid = Sampled.of(-1.0, 2.0, 512)

        test("maximalGradeSet against the virtual height is empty for the module's main input") {
            // §20.9 verbatim: the analytic height 1.0 is attained at no grid
            // point. Both answers right; different questions; §18.3 said so.
            triangle.maximalGradeSet(grid).isEmpty() shouldBe true
        }

        test("meanOfMaxima filters at the fold's fidelity and returns ≈ the true peak, not a crash") {
            Defuzzifiers.meanOfMaxima(triangle, grid) shouldBe (0.5 plusOrMinus 0.01)
        }
    }

    context("§22.1: the cancellation, executable") {

        val triangle = TriangularNumber.of(0.0, 1.0, 3.0)
        val coarse = Sampled.of(0.0, 3.0, 1024)
        val fine = Sampled.of(0.0, 3.0, 4096)

        test("sigma-count scales with the point count — the σ-count non-integral (§20.1(b))") {
            val ratio = triangle.sigmaCount(fine) / triangle.sigmaCount(coarse)
            withClue("4x the points, ~4x the sum") {
                (ratio > 3.9 && ratio < 4.1) shouldBe true
            }
        }

        test("the centroid ratio is h-free: same answer at both resolutions, and it is 4/3") {
            val atCoarse = Defuzzifiers.centroid(triangle, coarse)
            val atFine = Defuzzifiers.centroid(triangle, fine)
            atCoarse shouldBe (4.0 / 3.0 plusOrMinus 1e-6)
            atFine shouldBe (4.0 / 3.0 plusOrMinus 1e-6)
            withClue("h cancelled: refinement changed the sums, not the ratio") {
                abs(atCoarse - atFine) shouldBe (0.0 plusOrMinus 1e-6)
            }
        }

        test("bisector converges to the analytic 3 − √3 — partial sums, h on both sides") {
            Defuzzifiers.bisector(triangle, fine) shouldBe
                ((3.0 - sqrt(3.0)) plusOrMinus 2e-3)
        }
    }
})
