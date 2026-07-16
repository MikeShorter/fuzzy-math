package dk.eusrbin.fuzzy.defuzz

import dk.eusrbin.fuzzy.set.DoubleMembershipFn
import dk.eusrbin.fuzzy.set.Enumerable
import dk.eusrbin.fuzzy.set.Sampled
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.math.abs
import kotlin.math.max

/**
 * Behavioural pins for the defuzzifiers, against hand-built lambdas — no
 * fuzzy-number here. The facts that need a `TriangularNumber` (the §20.9 case,
 * the off-window throw, the spike) are pinned in `fuzzy-laws`' own tests,
 * where both modules are on the path (§22.6).
 *
 * Tolerances are per-operation and never EXACT (§14.6(a), §21.9): everything
 * here is a ratio or a mean — arithmetic end to end.
 */
class DefuzzifiersTest : FunSpec({

    /** Symmetric triangle peaked at 1.0 on [0, 2]. */
    val symmetric = DoubleMembershipFn { x -> max(0.0, 1.0 - abs(x - 1.0)) }

    /** Trapezoid with plateau [1, 2] inside [0, 3]; kinks land on any grid that divides thirds. */
    val plateau = DoubleMembershipFn { x ->
        when {
            x <= 0.0 || x >= 3.0 -> 0.0
            x < 1.0 -> x
            x <= 2.0 -> 1.0
            else -> 3.0 - x
        }
    }

    val grid = Sampled.of(0.0, 3.0, 301) // spacing 0.01; kinks at 1.0 and 2.0 on-grid

    context("centroid") {

        test("of a symmetric set is its centre of symmetry") {
            Defuzzifiers.centroid(symmetric, Sampled.of(0.0, 2.0, 201)) shouldBe
                (1.0 plusOrMinus 1e-9)
        }

        test("over an Enumerable is the discrete weighted mean, duplicates counted (§16.3, §22.1)") {
            val ones = DoubleMembershipFn { _ -> 1.0 }
            // 1.0 listed twice: it weighs twice, exactly as sigmaCount counts it.
            Defuzzifiers.centroid(ones, Enumerable.of(1.0, 1.0, 4.0)) shouldBe
                (2.0 plusOrMinus 1e-12)
        }
    }

    context("bisector") {

        test("of a symmetric set is its centre, within a grid step") {
            Defuzzifiers.bisector(symmetric, Sampled.of(0.0, 2.0, 201)) shouldBe
                (1.0 plusOrMinus 0.01)
        }
    }

    context("the maxima family") {

        test("of a plateau: smallest and largest are its shoulders, the mean its midpoint") {
            // Tolerance is one grid step, not ulps: whether the shoulder kink
            // lands exactly on a computed grid point is Sampled's internal
            // arithmetic, and one ulp either way moves the answer a full step.
            // The grid estimate converging to the shoulder is the claim; the
            // kink being a grid point is not.
            Defuzzifiers.smallestOfMaxima(plateau, grid) shouldBe (1.0 plusOrMinus 0.011)
            Defuzzifiers.largestOfMaxima(plateau, grid) shouldBe (2.0 plusOrMinus 0.011)
            Defuzzifiers.meanOfMaxima(plateau, grid) shouldBe (1.5 plusOrMinus 0.011)
        }

        test("smallest ≤ mean ≤ largest, by construction") {
            val som = Defuzzifiers.smallestOfMaxima(symmetric, grid)
            val mom = Defuzzifiers.meanOfMaxima(symmetric, grid)
            val lom = Defuzzifiers.largestOfMaxima(symmetric, grid)
            (som <= mom && mom <= lom) shouldBe true
        }

        test("smallest/largest are by VALUE, not enumeration order") {
            val f = DoubleMembershipFn { x -> if (x == 2.0 || x == 5.0) 1.0 else 0.1 }
            val shuffled = Enumerable.of(5.0, 1.0, 2.0, 3.0)
            Defuzzifiers.smallestOfMaxima(f, shuffled) shouldBe 2.0
            Defuzzifiers.largestOfMaxima(f, shuffled) shouldBe 5.0
        }
    }

    context("§22.3: no mass found → every operation refuses") {

        val zero = DoubleMembershipFn { _ -> 0.0 }

        listOf<Pair<String, () -> Double>>(
            "centroid" to { Defuzzifiers.centroid(zero, grid) },
            "bisector" to { Defuzzifiers.bisector(zero, grid) },
            "meanOfMaxima" to { Defuzzifiers.meanOfMaxima(zero, grid) },
            "smallestOfMaxima" to { Defuzzifiers.smallestOfMaxima(zero, grid) },
            "largestOfMaxima" to { Defuzzifiers.largestOfMaxima(zero, grid) },
        ).forEach { (name, call) ->
            test("$name throws, and the message reports what was DONE, not what is true") {
                val exception = shouldThrow<IllegalArgumentException> { call() }
                exception.message.shouldContain("no mass found")
                exception.message.shouldContain("checkEmptiness")
            }
        }

        test("over an Enumerable the zero fold IS §II p.340's emptiness — and it still throws") {
            // Same refusal, stronger epistemic footing (§22.3): here the fold
            // visited every element, so "no mass found" is "no mass".
            shouldThrow<IllegalArgumentException> {
                Defuzzifiers.centroid(zero, Enumerable.of(1.0, 2.0, 3.0))
            }
        }
    }
})
