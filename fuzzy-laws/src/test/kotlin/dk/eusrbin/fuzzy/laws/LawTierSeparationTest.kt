package dk.eusrbin.fuzzy.laws

import dk.eusrbin.fuzzy.algebra.Algebra
import dk.eusrbin.fuzzy.algebra.TNorms
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * **The test of the test.**
 *
 * CLAUDE.md §7 does not merely permit this, it requires it:
 *
 * > *"**Required test-of-the-test:** `StandardLaws` must FAIL for the Product
 * > algebra — assert that failure explicitly. If distributivity appears to hold
 * > for Product, the suite is broken."*
 *
 * A law suite that passes everything is indistinguishable from a law suite that
 * checks nothing. The tiers of CLAUDE.md §7 are only real if each one **rejects**
 * something the tier above accepts. This file pins every cell of that table —
 * the passes *and* the failures — so that a suite which silently stops
 * discriminating fails the build.
 *
 * | | TNorm | Residuum | BL | MV | Standard |
 * |---|---|---|---|---|---|
 * | Gödel / Standard | ✓ | ✓ | ✓ | ✗ | **✓** |
 * | Product | ✓ | ✓ | ✓ | ✗ | **✗** |
 * | Łukasiewicz | ✓ | ✓ | ✓ | **✓** | ✗ |
 * | Nilpotent minimum | ✓ | ✓ | **✗** | — | — |
 * | Drastic | ✓ | **✗** | ✗ | — | — |
 *
 * Every bold cell is a claim that would be invisible without an explicit
 * assertion of failure.
 */
class LawTierSeparationTest : FunSpec({

    // ---- CLAUDE.md §7's required test-of-the-test --------------------------

    context("StandardLaws must FAIL for Product (CLAUDE.md §7)") {

        test("the report does not hold") {
            StandardLaws.check(Algebra.PRODUCT).holds shouldBe false
        }

        test("distributivity fails — both eq. 9 and eq. 10") {
            val failed = StandardLaws.check(Algebra.PRODUCT).failures.map { it.law }

            // CLAUDE.md §7: "If distributivity appears to hold for Product, the
            // suite is broken." This is that assertion, stated positively.
            failed.count { it.startsWith("distributivity") } shouldBe 2
            failed.any { it.contains("eq. 9") } shouldBe true
            failed.any { it.contains("eq. 10") } shouldBe true
        }

        test("idempotence and absorption fail too") {
            val failed = StandardLaws.check(Algebra.PRODUCT).failures.map { it.law }
            failed shouldContainAll listOf(
                "idempotence: T(a,a) = a",
                "idempotence: S(a,a) = a",
                "absorption: T(a,S(a,b)) = a",
                "absorption: S(a,T(a,b)) = a",
            )
        }

        test("every StandardLaws law fails for Product — the tier is entirely absent") {
            val report = StandardLaws.check(Algebra.PRODUCT)
            report.failures.size shouldBe report.results.size
        }

        test("the counterexample is concrete and re-checkable") {
            val distributivity = StandardLaws.check(Algebra.PRODUCT)
                .failures
                .first { it.law.contains("eq. 9") }
            val counterexample = distributivity.counterexample!!
            val (a, b, c) = Triple(
                counterexample.inputs[0],
                counterexample.inputs[1],
                counterexample.inputs[2],
            )

            // Re-derive the failure by hand from the reported triple. The gap is
            // abc(1 − a) — algebraic, not numerical: no epsilon is involved.
            val lhs = Algebra.PRODUCT.and(a, Algebra.PRODUCT.or(b, c))
            val rhs = Algebra.PRODUCT.or(Algebra.PRODUCT.and(a, b), Algebra.PRODUCT.and(a, c))
            (lhs == rhs) shouldBe false
        }

        test("verify() throws, and the message names the algebra and the law") {
            val error = runCatching { StandardLaws.verify(Algebra.PRODUCT) }.exceptionOrNull()
            (error is LawViolationException) shouldBe true
            error!!.message!! shouldContain "Algebra.Product"
            error.message!! shouldContain "distributivity"
        }
    }

    test("StandardLaws must FAIL for Łukasiewicz — min is the only idempotent t-norm") {
        val failed = StandardLaws.check(Algebra.LUKASIEWICZ).failures.map { it.law }
        failed shouldContainAll listOf("idempotence: T(a,a) = a")
        failed.count { it.startsWith("distributivity") } shouldBe 2
    }

    test("StandardLaws HOLDS for Standard — and at ε = 0, exactly") {
        val report = StandardLaws.check(Algebra.STANDARD)
        report.holds shouldBe true

        // CLAUDE.md §8: "min/max are exactly associative and idempotent → the
        // Zadeh tier is safe with exact equality." If this ever needs an epsilon,
        // min has stopped being min.
        report.tolerance shouldBe Tolerance.EXACT
    }

    // ---- The residuated tier rejects non-left-continuity --------------------

    context("ResiduumLaws must FAIL for Drastic — left-continuity is a real precondition") {

        test("the adjunction fails") {
            ResiduumLaws.check(TNorms.DRASTIC).holds shouldBe false
        }

        test("it is the ← direction that breaks: the supremum is not attained") {
            val failed = ResiduumLaws.check(TNorms.DRASTIC).failures.map { it.law }
            failed.any { it.startsWith("residuation ←") } shouldBe true
        }

        test("the documented witness reproduces by hand: a=0.5, b=0.3, z=1") {
            // TNorms.DRASTIC's KDoc claims exactly this. Assert the claim.
            TNorms.DRASTIC.residuum(0.5, 0.3) shouldBe 1.0     // z = 1 ≤ (a ⇒ b)
            TNorms.DRASTIC.apply(0.5, 1.0) shouldBe 0.5        // yet T(a,z) = 0.5
            (0.5 <= 0.3) shouldBe false                        // ... which exceeds b
        }

        test("Drastic is still a perfectly good t-norm — only the residuum fails") {
            // The tiers are independent claims. Failing ResiduumLaws must not
            // imply failing TNormLaws.
            TNormLaws.check(TNorms.DRASTIC).holds shouldBe true
        }
    }

    // ---- The BL tier rejects non-continuity ---------------------------------

    context("Nilpotent minimum separates ResiduumLaws from BLAlgebraLaws") {

        test("left-continuous ⟹ ResiduumLaws holds") {
            ResiduumLaws.check(TNorms.NILPOTENT_MINIMUM).holds shouldBe true
        }

        test("but not continuous ⟹ divisibility fails: MTL, not BL") {
            val report = BLAlgebraLaws.check(TNorms.NILPOTENT_MINIMUM)
            report.holds shouldBe false
            report.failures.map { it.law }
                .any { it.startsWith("divisibility") } shouldBe true
        }

        test("prelinearity still holds — that is what makes it MTL") {
            val prelinearity = BLAlgebraLaws.check(TNorms.NILPOTENT_MINIMUM)
                .results
                .first { it.law.startsWith("prelinearity") }
            prelinearity.holds shouldBe true
        }

        test("the documented divisibility witness reproduces: a=0.6, b=0.2") {
            val residuum = TNorms.NILPOTENT_MINIMUM.residuum(0.6, 0.2)
            residuum shouldBe 0.4                                    // max(1−a, b)
            TNorms.NILPOTENT_MINIMUM.apply(0.6, residuum) shouldBe 0.0  // a + 0.4 = 1.0, not > 1
            // ... but divisibility demands min(a,b) = 0.2
        }
    }

    // ---- The MV tier is Łukasiewicz alone -----------------------------------

    context("MVAlgebraLaws must hold for Łukasiewicz and FAIL for the others") {

        test("Łukasiewicz is an MV-algebra") {
            MVAlgebraLaws.verify(Algebra.LUKASIEWICZ)
        }

        test("Standard is not — excluded middle fails at a = 0.5") {
            val failed = MVAlgebraLaws.check(Algebra.STANDARD).failures.map { it.law }
            failed.any { it.startsWith("(MV7)") } shouldBe true
            Algebra.STANDARD.or(0.5, Algebra.STANDARD.not(0.5)) shouldBe 0.5
        }

        test("Standard fails Chang's axiom — the documented witness a=0.2, b=0.9") {
            val failed = MVAlgebraLaws.check(Algebra.STANDARD).failures.map { it.law }
            failed.any { it.startsWith("(MV8)") } shouldBe true
        }

        test("Product is not — excluded middle gives 0.75 at a = 0.5") {
            val failed = MVAlgebraLaws.check(Algebra.PRODUCT).failures.map { it.law }
            failed.any { it.startsWith("(MV7)") } shouldBe true
            Algebra.PRODUCT.or(0.5, Algebra.PRODUCT.not(0.5)) shouldBe 0.75
        }

        test("MV1–MV6 hold for every built-in — the tier is earned by MV7 and MV8 alone") {
            for (algebra in Algebra.BUILT_INS) {
                val unremarkable = MVAlgebraLaws.check(algebra).results.filter {
                    it.law.startsWith("(MV1)") || it.law.startsWith("(MV2)") ||
                        it.law.startsWith("(MV3)") || it.law.startsWith("(MV4)") ||
                        it.law.startsWith("(MV5)") || it.law.startsWith("(MV6)")
                }
                unremarkable.size shouldBe 6
                unremarkable.all { it.holds } shouldBe true
            }
        }
    }

    // ---- The De Morgan tier rejects a mismatched triple ----------------------

    context("DeMorganLaws must FAIL for a mismatched triple (CLAUDE.md §6's footgun)") {

        test("Product t-norm with Gödel conorm breaks De Morgan") {
            // Exactly the mistake Algebra.of() lets you make and Algebra.deMorgan()
            // does not. Nothing throws; the numbers just stop meaning anything.
            val mismatched = Algebra.of(
                "Mismatched",
                TNorms.PRODUCT,
                dk.eusrbin.fuzzy.algebra.TConorms.GODEL,
                dk.eusrbin.fuzzy.algebra.Negations.STANDARD,
            )
            DeMorganLaws.check(mismatched).holds shouldBe false
        }

        test("... while every value it produces is a perfectly plausible degree") {
            // Why this needs a law suite rather than a runtime check: the failure
            // mode is silent, not loud.
            val mismatched = Algebra.of(
                "Mismatched",
                TNorms.PRODUCT,
                dk.eusrbin.fuzzy.algebra.TConorms.GODEL,
                dk.eusrbin.fuzzy.algebra.Negations.STANDARD,
            )
            val result = mismatched.or(mismatched.and(0.3, 0.7), 0.4)
            dk.eusrbin.fuzzy.algebra.Degrees.isDegree(result) shouldBe true
        }

        test("all three built-in algebras satisfy De Morgan") {
            for (algebra in Algebra.BUILT_INS) {
                DeMorganLaws.verify(algebra)
            }
        }
    }
})
