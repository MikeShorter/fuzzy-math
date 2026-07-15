package dk.eusrbin.fuzzy.laws

import dk.eusrbin.fuzzy.algebra.Algebra
import dk.eusrbin.fuzzy.set.Enumerable
import dk.eusrbin.fuzzy.set.FuzzySets
import dk.eusrbin.fuzzy.set.MembershipFn
import dk.eusrbin.fuzzy.set.Sampled
import dk.eusrbin.fuzzy.set.Verdict
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * **The test of the test, for slice 2a.**
 *
 * CLAUDE.md §7: *"A law suite that passes everything is indistinguishable from a
 * law suite that checks nothing."* Slice 1 asserted that `StandardLaws` fails for
 * Product. The set suites need the same treatment, and they have more to prove:
 * each one is asserted to **reject** something.
 *
 * | suite | must reject |
 * |---|---|
 * | [ZadehSetLaws] | eqs. (9)/(10) for `Algebra.PRODUCT` — the tier lifts |
 * | [MembershipFnLaws] | a wrong §15.3 override — the whole reason it exists |
 * | [DecompositionLaws] | a cut that contains what it should not |
 * | [Verdict] | conflating "proved" with "found nothing" |
 */
class SetLawTierSeparationTest : FunSpec({

    // ---- The tier lifts from degrees to sets --------------------------------

    context("ZadehSetLaws must FAIL eqs. (9)/(10) for Product (CLAUDE.md §7 lifted)") {

        test("Standard satisfies every law Zadeh states") {
            ZadehSetLaws.verify(
                SetGenerators.warm,
                SetGenerators.cool,
                SetGenerators.mild,
                SetGenerators.terms,
                Algebra.STANDARD,
            )
        }

        test("Product fails — and fails EXACTLY the two distributive laws") {
            val report = ZadehSetLaws.check(
                SetGenerators.warm,
                SetGenerators.cool,
                SetGenerators.mild,
                SetGenerators.terms,
                Algebra.PRODUCT,
            )
            report.holds shouldBe false

            val failed = report.failures.map { it.law }
            failed.count { it.startsWith("distributive") } shouldBe 2
            failed.any { it.contains("eq. 9") } shouldBe true
            failed.any { it.contains("eq. 10") } shouldBe true

            // De Morgan and Zadeh's two containments are NOT tier-bound: they
            // hold for Product exactly as they hold for Standard. If this ever
            // reports more than the two distributive laws, the lift is wrong.
            failed.size shouldBe 2
        }

        test("the failure is algebraic, not numerical — the witness re-derives by hand") {
            val counterexample = ZadehSetLaws.check(
                SetGenerators.warm,
                SetGenerators.cool,
                SetGenerators.mild,
                SetGenerators.terms,
                Algebra.PRODUCT,
            ).failures.first { it.law.contains("eq. 9") }.counterexample!!

            counterexample.detail shouldContain "x = "

            // C ∩ (A ∪ B) = c(a+b−ab) vs (C ∩ A) ∪ (C ∩ B) = ca+cb−c²ab.
            // The gap is abc(1−c) — O(0.05) here, nowhere near an epsilon.
            val p = Algebra.PRODUCT
            val (a, b, c) = Triple(0.3, 0.7, 0.5)
            val lhs = p.and(c, p.or(a, b))
            val rhs = p.or(p.and(c, a), p.and(c, b))
            lhs shouldBe 0.395
            rhs shouldBe 0.4475
        }

        test("Łukasiewicz fails them too — min/max is the only distributive pair") {
            val failed = ZadehSetLaws.check(
                SetGenerators.warm,
                SetGenerators.cool,
                SetGenerators.mild,
                SetGenerators.terms,
                Algebra.LUKASIEWICZ,
            ).failures.map { it.law }
            failed.count { it.startsWith("distributive") } shouldBe 2
        }
    }

    // ---- MembershipFnLaws guards §15.3's hook -------------------------------

    context("MembershipFnLaws must catch a wrong override (§15.3's hook is unguarded)") {

        test("an honest analytic height passes") {
            MembershipFnLaws.verify(SetGenerators.HonestTriangle(), SetGenerators.line)
        }

        test("a lying height is caught — and nothing else could catch it") {
            val lying = SetGenerators.LyingTriangle()

            // 0.5 is a perfectly good degree. Closure checks, range checks and
            // every degree-level law in this artifact are silent here.
            lying.height(SetGenerators.line) shouldBe 0.5
            dk.eusrbin.fuzzy.algebra.Degrees.isDegree(lying.height(SetGenerators.line)) shouldBe true

            val report = MembershipFnLaws.check(lying, SetGenerators.line)
            report.holds shouldBe false
            report.failures.map { it.law }.any { it.contains("height") } shouldBe true
        }

        test("a lying alphaCut is caught too — the override check is not just about height") {
            val report = MembershipFnLaws.check(SetGenerators.ForgetfulTriangle(), SetGenerators.line)
            report.holds shouldBe false
            report.failures.map { it.law }.any { it.contains("alphaCut") } shouldBe true
        }

        test("a plain lambda overrides nothing, so it passes trivially — as it must") {
            val plain = MembershipFn<String> { x -> SetGenerators.warm.apply(x) }
            MembershipFnLaws.verify(plain, SetGenerators.terms)
        }

        test("a function leaving [0,1] is caught by the range law") {
            val outOfRange = MembershipFn<String> { x -> if (x == "hot") 1.5 else 0.5 }
            val report = MembershipFnLaws.check(outOfRange, SetGenerators.terms)
            report.holds shouldBe false
            report.failures.map { it.law }.any { it.startsWith("range") } shouldBe true
        }
    }

    // ---- §18.3: core and maximalGradeSet are different sets -----------------

    context("core vs maximalGradeSet — they coincide iff normal (§18.3)") {

        test("normal: the two agree") {
            val warm = SetGenerators.warm
            warm.isNormal(SetGenerators.terms) shouldBe true
            warm.core(SetGenerators.terms) shouldContainExactly listOf("hot")
            warm.maximalGradeSet(SetGenerators.terms) shouldContainExactly listOf("hot")
        }

        test("subnormal: core is EMPTY while maximalGradeSet is not — the whole point") {
            val s = SetGenerators.subnormal
            s.isNormal(SetGenerators.terms) shouldBe false
            s.height(SetGenerators.terms) shouldBe 0.6

            s.core(SetGenerators.terms) shouldBe emptyList()
            s.maximalGradeSet(SetGenerators.terms) shouldContainExactly listOf("mild", "warm")
        }

        test("shipping one name for both would have been a lie either way") {
            // Option (1) alone would have silently redefined Zadeh's word;
            // option (2) alone would have surprised everyone expecting {f = 1}.
            // §18.3 ships both, and this is the observation that forced it.
            val s = SetGenerators.subnormal
            (s.core(SetGenerators.terms) == s.maximalGradeSet(SetGenerators.terms)) shouldBe false
        }
    }

    // ---- §16.4: the Verdict earns its three values --------------------------

    context("Verdict must distinguish proved from found-nothing (§15.6, §16.4)") {

        test("Enumerable + no witness = Proven — the ∀ visited every element") {
            val v = SetGenerators.cool.checkContainment(FuzzySets.constant(1.0), SetGenerators.terms)
            (v is Verdict.Proven) shouldBe true
            v.isProven shouldBe true
        }

        test("Sampled + no witness = NotRefuted — a grid proves nothing") {
            val v = SetGenerators.nearZero.checkContainment(FuzzySets.constant(1.0), SetGenerators.line)
            (v is Verdict.NotRefuted) shouldBe true
            v.isProven shouldBe false // and this is NOT "the claim is false"
        }

        test("a witness on a GRID refutes absolutely — sampling is lossy one way only") {
            // The asymmetry that makes three values necessary rather than fussy.
            val v = FuzzySets.constant<Double>(1.0).checkContainment(SetGenerators.nearZero, SetGenerators.line)
            (v is Verdict.Refuted) shouldBe true
            v.witness shouldBe -1.0 // exp(-1) < 1: the first grid point that breaks it
        }

        test("the boolean sugar exists only where a boolean is honest") {
            // isContainedIn takes an Enumerable, not a Domain. There is
            // deliberately no overload that would let a Sampled answer true.
            SetGenerators.cool.isContainedIn(FuzzySets.constant(1.0), SetGenerators.terms) shouldBe true
            SetGenerators.warm.isEqualTo(SetGenerators.warm, SetGenerators.terms) shouldBe true
            FuzzySets.constant<String>(0.0).isEmpty(SetGenerators.terms) shouldBe true
            SetGenerators.warm.isEmpty(SetGenerators.terms) shouldBe false
        }

        test("Verdict.of maps isExhaustive to the verdict in ONE place") {
            val enumerable: Enumerable<String> = SetGenerators.terms
            val sampled: Sampled = SetGenerators.line
            enumerable.isExhaustive shouldBe true
            sampled.isExhaustive shouldBe false
            (Verdict.noWitness(enumerable) is Verdict.Proven) shouldBe true
            (Verdict.noWitness(sampled) is Verdict.NotRefuted) shouldBe true
        }
    }

    // ---- DecompositionLaws rejects a bad cut --------------------------------

    context("DecompositionLaws must catch a cut that contains what it should not") {

        test("an honest set round-trips exactly, at ε = 0") {
            val report = DecompositionLaws.check(SetGenerators.warm, SetGenerators.terms)
            report.holds shouldBe true
            report.tolerance shouldBe Tolerance.EXACT
        }

        test("a cut with an intruder is caught") {
            // alphaCut claiming a point whose degree is below α.
            val greedy = object : MembershipFn<String> {
                override fun apply(x: String): Double = SetGenerators.warm.apply(x)

                override fun alphaCut(over: dk.eusrbin.fuzzy.set.Domain<String>, alpha: Double): List<String> =
                    SetGenerators.terms.elements() // everything, always
            }
            val report = DecompositionLaws.check(greedy, SetGenerators.terms)
            report.holds shouldBe false
            report.failures.map { it.law }.any { it.startsWith("Γ_α") } shouldBe true
        }

        test("coarse levels under-recover — and that is not a failure") {
            // Reconstruction cannot return a degree that is not a level. A
            // staircase is the CORRECT answer to a coarse decomposition.
            val coarse = SetGenerators.warm.decompose(SetGenerators.terms, doubleArrayOf(0.5, 1.0))
            val rebuilt = FuzzySets.fromDecomposition(coarse)

            rebuilt.apply("cool") shouldBe 0.0 // true degree 0.25 — below every level
            rebuilt.apply("warm") shouldBe 0.5 // true degree 0.75 — rounded down to a level
            rebuilt.apply("hot") shouldBe 1.0 // exact: 1.0 IS a level

            withClue("under-recovery is honest; over-recovery would be a bug") {
                for (x in SetGenerators.terms.elements()) {
                    (rebuilt.apply(x) <= SetGenerators.warm.apply(x)) shouldBe true
                }
            }
        }
    }
})
