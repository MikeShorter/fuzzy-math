package dk.eusrbin.fuzzy.laws

import dk.eusrbin.fuzzy.algebra.Algebra
import dk.eusrbin.fuzzy.algebra.Degrees
import dk.eusrbin.fuzzy.algebra.Implications
import dk.eusrbin.fuzzy.algebra.Negations
import dk.eusrbin.fuzzy.algebra.OrdinalSummand
import dk.eusrbin.fuzzy.algebra.TConorms
import dk.eusrbin.fuzzy.algebra.TNorm
import dk.eusrbin.fuzzy.algebra.TNorms
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.checkAll
import kotlin.math.abs

/**
 * Known values, pinned against the sources.
 *
 * The law suites check that the algebras are *self-consistent*. They cannot
 * catch an algebra that is consistently the wrong function — a t-norm returning
 * `min(a,b)/2` fails the boundary condition, but subtler slips survive. This
 * file pins the arithmetic itself against CLAUDE.md §2's sources, which is the
 * other half of "the sources are the spec".
 */
class KnownValuesTest : FunSpec({

    val ε = 1e-12

    // ---- Zadeh 1965, the founding paper ------------------------------------

    context("Zadeh 1965") {

        test("§II eq. (3), (5): union is Max, intersection is Min") {
            Algebra.ZADEH.or(0.3, 0.7) shouldBe 0.7
            Algebra.ZADEH.and(0.3, 0.7) shouldBe 0.3
        }

        test("§II eq. (1), p.340: complement is 1 − f_A") {
            Algebra.ZADEH.not(0.3) shouldBe (0.7 plusOrMinus ε)
        }

        test("§IV: the algebraic product IS the Product t-norm (CLAUDE.md §6)") {
            // The claim is identity, not similarity.
            TNorms.PRODUCT.apply(0.3, 0.7) shouldBe (0.21 plusOrMinus ε)
            Algebra.PRODUCT.and(0.3, 0.7) shouldBe (0.21 plusOrMinus ε)
        }

        test("§IV footnote 4: the algebraic sum IS the Product conorm (CLAUDE.md §6)") {
            // f_A + f_B − f_A·f_B = 0.3 + 0.7 − 0.21
            TConorms.PROBABILISTIC_SUM.apply(0.3, 0.7) shouldBe (0.79 plusOrMinus ε)
        }

        test("§IV: 'f_A + f_B where f_A + f_B ≤ 1' is an uncapped Łukasiewicz conorm") {
            // CLAUDE.md §6. Inside Zadeh's side-condition the two agree...
            TConorms.LUKASIEWICZ.apply(0.3, 0.6) shouldBe (0.9 plusOrMinus ε)
            // ... and outside it, min(1, ·) is doing the work his side-condition did by hand.
            TConorms.LUKASIEWICZ.apply(0.7, 0.6) shouldBe 1.0
        }

        test("§III p.343: Zadeh's distributive lattice is min/max ONLY (CLAUDE.md §7)") {
            // The remark that motivated the whole law-tier design.
            StandardLaws.check(Algebra.STANDARD).holds shouldBe true
            StandardLaws.check(Algebra.PRODUCT).holds shouldBe false
        }

        test("footnote 3: the same functions in different hats") {
            // "if f_A(x) is interpreted as the truth value ... a many-valued logic"
            // Set intersection and logical conjunction are literally one object.
            (Algebra.ZADEH.tNorm === TNorms.GODEL) shouldBe true
            (Algebra.ZADEH === Algebra.GODEL) shouldBe true
        }
    }

    // ---- Bergmann 2008, the three systems -----------------------------------

    context("Bergmann 2008 — the three t-norms and their residua") {

        test("§11.8 Gödel: T = min, residuum = 1 if a ≤ b else b") {
            TNorms.GODEL.apply(0.3, 0.7) shouldBe 0.3
            TNorms.GODEL.residuum(0.3, 0.7) shouldBe 1.0
            TNorms.GODEL.residuum(0.7, 0.3) shouldBe 0.3
        }

        test("§11.9 Product: T = a·b, residuum = 1 if a ≤ b else b/a") {
            TNorms.PRODUCT.residuum(0.3, 0.7) shouldBe 1.0
            TNorms.PRODUCT.residuum(0.8, 0.4) shouldBe (0.5 plusOrMinus ε)
        }

        test("§11.2 Łukasiewicz: T = max(0, a+b−1), residuum = min(1, 1−a+b)") {
            TNorms.LUKASIEWICZ.apply(0.7, 0.6) shouldBe (0.3 plusOrMinus ε)
            TNorms.LUKASIEWICZ.apply(0.2, 0.3) shouldBe 0.0
            TNorms.LUKASIEWICZ.residuum(0.3, 0.7) shouldBe 1.0
            TNorms.LUKASIEWICZ.residuum(0.7, 0.6) shouldBe (0.9 plusOrMinus ε)
        }

        test("§12: the Łukasiewicz biresiduum is 1 − |a − b| — a metric") {
            // What being an MV-algebra buys you.
            checkAll(Generators.degree, Generators.degree) { a, b ->
                Algebra.LUKASIEWICZ.iff(a, b) shouldBe ((1.0 - abs(a - b)) plusOrMinus 1e-14)
            }
        }
    }

    // ---- CLAUDE.md §5: the residuum is derived ------------------------------

    context("CLAUDE.md §5 — implication is derived, not configured") {

        test("the generic bisection agrees with every closed form") {
            // The load-bearing claim: "for a left-continuous t-norm the residuum
            // is *determined*". So a t-norm that hides its closed form — forcing
            // TNorm.residuum's bisection — must yield the same answer.
            //
            // normalDegree, not degree: bisection and the closed form genuinely
            // disagree on subnormals, because `apply` underflows there. That is
            // documented on TNorm.residuum and asserted as its own test below —
            // it is not swept into this one's tolerance.
            for (named in listOf(TNorms.GODEL, TNorms.PRODUCT, TNorms.LUKASIEWICZ, TNorms.NILPOTENT_MINIMUM)) {
                // A SAM lambda gets the interface default: bisection, no override.
                val opaque = TNorm { a, b -> named.apply(a, b) }
                withClue(named) {
                    checkAll(200, Generators.normalDegree, Generators.normalDegree) { a, b ->
                        opaque.residuum(a, b) shouldBe (named.residuum(a, b) plusOrMinus 1e-9)
                    }
                }
            }
        }

        test("... and diverges on subnormals, exactly as TNorm.residuum documents") {
            // Pinning a documented limitation so the KDoc cannot quietly rot.
            //
            // In the subnormal range IEEE 754 multiplication is not strictly
            // monotone, so the FLOAT Product t-norm is not the Product t-norm.
            // Bisection is not wrong about it — it is right about the only
            // function it can observe.
            //
            // The exact landing point is round-half-to-even: MIN_VALUE·z is
            // exactly halfway between 0 and MIN_VALUE at z = 0.5, and 0 is the
            // even neighbour, so it underflows to 0 for z ≤ 0.5 and rounds up to
            // MIN_VALUE above. Bisection therefore converges on 0.5 exactly.
            (Double.MIN_VALUE * 0.5) shouldBe 0.0
            (Double.MIN_VALUE * 0.75) shouldBe Double.MIN_VALUE

            val opaque = TNorm { a, b -> a * b }
            opaque.residuum(Double.MIN_VALUE, 0.0) shouldBe 0.5   // the float function's sup
            TNorms.PRODUCT.residuum(Double.MIN_VALUE, 0.0) shouldBe 0.0 // b/a — the real one's

            // Away from the subnormals the two agree, as the test above asserts
            // wholesale. This is the same claim at one concrete point.
            opaque.residuum(0.8, 0.4) shouldBe (TNorms.PRODUCT.residuum(0.8, 0.4) plusOrMinus 1e-12)
        }

        test("1 − x is NOT exactly involutive — why EXACT is wrong for any law using N") {
            // The assumption that shipped wrong: CLAUDE.md §8's "the Zadeh tier
            // is safe with exact equality" is a claim about min/max, and the
            // Standard algebra's negation is arithmetic, not lattice selection.
            val a = 1.0 / 3.0
            val roundTripped = Algebra.STANDARD.not(Algebra.STANDARD.not(a))
            (roundTripped == a) shouldBe false
            roundTripped shouldBe (a plusOrMinus 1e-15)

            // Which is exactly what Tolerance.forNegation exists to encode...
            Tolerance.forNegation(Negations.STANDARD) shouldBe Tolerance.ARITHMETIC

            // ... and why DeMorganLaws/MVAlgebraLaws must not inherit EXACT from
            // the algebra, while StandardLaws — min/max only — still may.
            DeMorganLaws.check(Algebra.STANDARD).tolerance shouldBe Tolerance.ARITHMETIC
            MVAlgebraLaws.check(Algebra.STANDARD).tolerance shouldBe Tolerance.ARITHMETIC
            StandardLaws.check(Algebra.STANDARD).tolerance shouldBe Tolerance.EXACT
        }

        test("the Yager negation stays involutive near a = 1, where the naive form collapses") {
            // Regression: (1 − a^w)^(1/w) evaluated literally loses every
            // significant bit to cancellation near a = 1, giving ~8% relative
            // error and a spurious De Morgan failure. See YagerNegation's KDoc.
            val yager = Negations.yager(2.476662004953192)
            val nearOne = 1.0 - Math.ulp(1.0)

            val n = yager.apply(nearOne)
            Degrees.isDegree(n) shouldBe true
            // The naive form returns ~6.4e-7 here against a true ~6.9e-7.
            yager.apply(n) shouldBe (nearOne plusOrMinus 1e-12)

            // Boundaries must survive the ln/expm1 route intact.
            yager.apply(0.0) shouldBe 1.0
            yager.apply(1.0) shouldBe 0.0
        }

        test("Algebra.implication is always the t-norm's residuum — never a parameter") {
            for (algebra in Algebra.BUILT_INS) {
                checkAll(50, Generators.degree, Generators.degree) { a, b ->
                    algebra.implies(a, b) shouldBe algebra.tNorm.residuum(a, b)
                }
            }
        }
    }

    // ---- The implication families are genuinely different -------------------

    context("Implication families (CLAUDE.md §5)") {

        test("R- and S-implications coincide for Łukasiewicz") {
            val s = Implications.sImplication(TConorms.LUKASIEWICZ, Negations.STANDARD)
            checkAll(Generators.degree, Generators.degree) { a, b ->
                Implications.LUKASIEWICZ.apply(a, b) shouldBe (s.apply(a, b) plusOrMinus 1e-14)
            }
        }

        test("... and diverge for Gödel — coincidence is a fact about Łukasiewicz, not a licence") {
            // Same algebra, same classical limit, different fuzzy answer.
            Implications.GODEL.apply(0.5, 0.5) shouldBe 1.0
            Implications.KLEENE_DIENES.apply(0.5, 0.5) shouldBe 0.5
        }

        test("Reichenbach: 1 − a + a·b") {
            Implications.REICHENBACH.apply(0.5, 0.4) shouldBe (0.7 plusOrMinus ε)
        }

        test("Zadeh's QL-implication: max(1−a, min(a,b))") {
            Implications.ZADEH.apply(0.7, 0.2) shouldBe (0.3 plusOrMinus ε)
        }

        test("Zadeh's QL-implication is NOT monotone in its first argument") {
            // Documented in Implications.ZADEH's KDoc; asserted here so the claim
            // cannot rot. I(a,1) = max(1−a, a) is V-shaped.
            val atHalf = Implications.ZADEH.apply(0.5, 1.0)
            val atThreeQuarters = Implications.ZADEH.apply(0.75, 1.0)
            atHalf shouldBe (0.5 plusOrMinus ε)
            atThreeQuarters shouldBe (0.75 plusOrMinus ε)
            // A fuzzy implication must be NON-INCREASING in `a`. This rises.
            (atThreeQuarters > atHalf) shouldBe true
        }

        test("all four corners agree with classical implication") {
            for (implication in listOf(
                Implications.GODEL,
                Implications.GOGUEN,
                Implications.LUKASIEWICZ,
                Implications.KLEENE_DIENES,
                Implications.REICHENBACH,
                Implications.ZADEH,
            )) {
                withClue(implication) {
                    implication.apply(0.0, 0.0) shouldBe 1.0
                    implication.apply(0.0, 1.0) shouldBe 1.0
                    implication.apply(1.0, 1.0) shouldBe 1.0
                    implication.apply(1.0, 0.0) shouldBe 0.0
                }
            }
        }
    }

    // ---- Mostert–Shields: the basis and its generator -----------------------

    context("Ordinal sums (CLAUDE.md §6 — a generating basis)") {

        test("the empty ordinal sum IS Gödel — min is the identity of the construction") {
            (TNorms.ordinalSum() === TNorms.GODEL) shouldBe true
        }

        test("a single full-width summand is that t-norm, rescaled by the identity") {
            val rebuilt = TNorms.ordinalSum(OrdinalSummand(0.0, 1.0, TNorms.LUKASIEWICZ))
            checkAll(Generators.degree, Generators.degree) { a, b ->
                rebuilt.apply(a, b) shouldBe (TNorms.LUKASIEWICZ.apply(a, b) plusOrMinus 1e-14)
            }
        }

        test("outside the summands, and across them, the sum is min") {
            val sum = TNorms.ordinalSum(OrdinalSummand(0.0, 0.5, TNorms.LUKASIEWICZ))
            sum.apply(0.8, 0.9) shouldBe 0.8          // both outside → min
            sum.apply(0.3, 0.9) shouldBe 0.3          // different regions → min
            sum.apply(0.3, 0.4) shouldBe (0.2 plusOrMinus ε) // both inside → rescaled Łukasiewicz
        }

        test("overlapping summands are rejected at construction") {
            val error = runCatching {
                TNorms.ordinalSum(
                    OrdinalSummand(0.0, 0.6, TNorms.PRODUCT),
                    OrdinalSummand(0.4, 1.0, TNorms.LUKASIEWICZ),
                )
            }.exceptionOrNull()
            (error is IllegalArgumentException) shouldBe true
        }

        test("adjacent summands sharing an endpoint are legal") {
            TNorms.ordinalSum(
                OrdinalSummand(0.0, 0.5, TNorms.LUKASIEWICZ),
                OrdinalSummand(0.5, 1.0, TNorms.PRODUCT),
            ).let { TNormLaws.verify(it) }
        }
    }

    // ---- Parameter validation happens at construction (Degrees KDoc) --------

    context("CLAUDE.md §4/§9 — construction validates, operations do not") {

        test("Hamacher rejects γ < 0 eagerly") {
            runCatching { TNorms.hamacher(-0.1) }.isFailure shouldBe true
        }

        test("Sugeno rejects λ ≤ −1 eagerly — the denominator would change sign on [0,1]") {
            runCatching { Negations.sugeno(-1.0) }.isFailure shouldBe true
            runCatching { Negations.sugeno(-0.999) }.isSuccess shouldBe true
        }

        test("Yager rejects w ≤ 0 eagerly") {
            runCatching { Negations.yager(0.0) }.isFailure shouldBe true
        }

        test("degenerate ordinal-sum intervals are rejected eagerly") {
            runCatching { OrdinalSummand(0.5, 0.5, TNorms.PRODUCT) }.isFailure shouldBe true
        }

        test("family landmarks return the singleton, inheriting its closed-form residuum") {
            (TNorms.hamacher(1.0) === TNorms.PRODUCT) shouldBe true
            (Negations.sugeno(0.0) === Negations.STANDARD) shouldBe true
            (Negations.yager(1.0) === Negations.STANDARD) shouldBe true
            (TConorms.hamacher(1.0) === TConorms.PROBABILISTIC_SUM) shouldBe true
        }

        test("Degrees rejects NaN rather than laundering it") {
            Degrees.isDegree(Double.NaN) shouldBe false
            runCatching { Degrees.clamp(Double.NaN) }.isFailure shouldBe true
            Degrees.clamp(1.5) shouldBe 1.0
            Degrees.clamp(-0.5) shouldBe 0.0
        }
    }

    // ---- Tolerance calibration (CLAUDE.md §8) ------------------------------

    context("Tolerance — calibrated per algebra, in one place") {

        test("the Zadeh/Gödel tier is exact; the arithmetic tiers are not") {
            Tolerance.forAlgebra(Algebra.STANDARD) shouldBe Tolerance.EXACT
            Tolerance.forTNorm(TNorms.GODEL) shouldBe Tolerance.EXACT
            Tolerance.forTNorm(TNorms.PRODUCT) shouldBe Tolerance.ARITHMETIC
            Tolerance.forTNorm(TNorms.LUKASIEWICZ) shouldBe Tolerance.ARITHMETIC
        }

        test("an unrecognised t-norm falls back to the conservative default") {
            Tolerance.forTNorm(TNorm { a, b -> a * b }) shouldBe Tolerance.GENERAL
            Tolerance.forTNorm(TNorms.hamacher(3.0)) shouldBe Tolerance.GENERAL
        }

        test("Product associativity is genuinely inexact — this is why ε > 0 exists") {
            // CLAUDE.md §8: "Product t-norm associativity (a·b)·c vs a·(b·c) is
            // not exact." Find a witness rather than take it on faith.
            var found = false
            checkAll(500, Generators.degree, Generators.degree, Generators.degree) { a, b, c ->
                val lhs = (a * b) * c
                val rhs = a * (b * c)
                if (lhs != rhs) found = true
            }
            found shouldBe true
        }

        test("min associativity is exact — this is why EXACT exists") {
            checkAll(500, Generators.degree, Generators.degree, Generators.degree) { a, b, c ->
                minOf(minOf(a, b), c) shouldBe minOf(a, minOf(b, c))
            }
        }

        test("EXACT and ARITHMETIC are different tolerances, not aliases") {
            Tolerance.EXACT shouldNotBe Tolerance.ARITHMETIC
            Tolerance.EXACT.epsilon shouldBe 0.0
        }
    }
})
