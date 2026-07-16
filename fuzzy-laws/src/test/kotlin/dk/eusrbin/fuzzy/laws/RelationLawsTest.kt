package dk.eusrbin.fuzzy.laws

import dk.eusrbin.fuzzy.algebra.Algebra
import dk.eusrbin.fuzzy.algebra.TNorm
import dk.eusrbin.fuzzy.relation.FuzzyRelations
import dk.eusrbin.fuzzy.relation.Mapping
import dk.eusrbin.fuzzy.relation.TransitivityWitness
import dk.eusrbin.fuzzy.set.Enumerable
import dk.eusrbin.fuzzy.set.MembershipFn
import dk.eusrbin.fuzzy.set.Sampled
import dk.eusrbin.fuzzy.set.Verdict
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.math.abs
import kotlin.math.exp

/**
 * `RelationLaws` against known subjects — the passes, and §7's required
 * failures.
 *
 * The test-of-the-test here is the raw-TNorm overload: sup-T composition is
 * associative *because* the t-norm is associative and monotone, so a
 * hand-written "t-norm" lacking those axioms must fail the associativity law.
 * If it did not, the law would be checking nothing (CLAUDE.md §7, §21.8).
 */
class RelationLawsTest : FunSpec({

    // ---- Fixtures: three relations in X, Zadeh's own setting (p.345) --------

    val points = Enumerable.of(0.0, 0.7, 1.3, 2.0)

    /** Symmetric similarity-shaped relation, exp(−|x−y|). */
    val similarity = MembershipFn<Pair<Double, Double>> { (x, y) -> exp(-abs(x - y)) }

    /** Triangular overlap, max(0, 1 − |x−y|). */
    val overlap = MembershipFn<Pair<Double, Double>> { (x, y) -> maxOf(0.0, 1.0 - abs(x - y)) }

    /** The crisp order x ≤ y, as an indicator. */
    val order = MembershipFn<Pair<Double, Double>> { (x, y) -> if (x <= y) 1.0 else 0.0 }

    context("the suite passes for every shipped algebra") {

        listOf(Algebra.STANDARD, Algebra.PRODUCT, Algebra.LUKASIEWICZ).forEach { algebra ->
            test("verify($algebra) over an Enumerable") {
                RelationLaws.verify(similarity, overlap, order, points, algebra)
            }
        }

        test("and over a Sampled middle — the derivation stays EXACT, associativity holds on the grid") {
            // Sound for a fixed grid: both associations quantify over the SAME
            // grid, and a finite max distributes over any monotone T. The
            // derivation-agreement law never depended on exhaustiveness at all.
            RelationLaws.verify(similarity, overlap, order, Sampled.of(0.0, 2.0, 9))
        }
    }

    context("test-of-the-test (§7): the failures that prove the laws check something") {

        test("a non-associative 't-norm' fails the associativity law — measured at 6.8e-2, no tolerance excuses it") {
            // avg is monotone and commutative but NOT associative, and not a
            // t-norm (no boundary). Sup-avg composition genuinely associates
            // differently: the law must catch it.
            val average = TNorm { a, b -> (a + b) / 2.0 }
            val report = RelationLaws.check(similarity, overlap, order, points, average)
            report.holds shouldBe false
            val failed = report.failures.map { it.law }
            withClue(report.toString()) {
                failed.any { it.contains("(A ∘ B) ∘ C") } shouldBe true
                // The derivation agreement must SURVIVE the broken t-norm: both
                // sides compute the same degrees whatever T is — that is §21.4's
                // whole claim, and it is a claim about the fold, not about T.
                failed.none { it.contains("shadow") } shouldBe true
            }
        }

        test("imageUnder over a Sampled domain throws — asserted as a fact, not worked around") {
            shouldThrow<IllegalArgumentException> {
                FuzzyRelations.imageUnder(
                    MembershipFn { x: Double -> exp(-x * x) },
                    Mapping { x: Double -> x * x },
                    Sampled.of(-2.0, 2.0, 101),
                )
            }.message.shouldContain("exhaustive")
        }
    }

    context("the verdict paths §21.8 promised") {

        test("a crisp order is min-transitive: Proven, from an exhaustive ∀ over X³") {
            FuzzyRelations.findNonTransitivity(order, points)
                .shouldBeInstanceOf<Verdict.Proven<TransitivityWitness<Double>>>()
        }

        test("exp(−|x−y|) is not min-transitive: Refuted, and the witness law holds — it reproduces") {
            // The intransitive subject does NOT fail the suite: transitivity of
            // the subject is not a law (§19.7 — the antecedent is the subject's
            // own business). What the suite checks is that the witness the
            // search returns is a real one.
            val report = RelationLaws.check(similarity, overlap, order, points)
            report.holds shouldBe true
            FuzzyRelations.findNonTransitivity(similarity, points)
                .shouldBeInstanceOf<Verdict.Refuted<TransitivityWitness<Double>>>()
        }
    }

    context("the image laws (§21.6)") {

        val degrees = mapOf(-2 to 0.1, -1 to 0.3, 0 to 0.5, 1 to 0.7, 2 to 0.9)
        val set = MembershipFn<Int> { x -> degrees.getValue(x) }
        val square = Mapping { x: Int -> x * x }
        val ints = Enumerable.of(-2, -1, 0, 1, 2)
        val images = Enumerable.of(0, 1, 3, 4) // includes 3: empty preimage

        listOf(Algebra.STANDARD, Algebra.PRODUCT, Algebra.LUKASIEWICZ).forEach { algebra ->
            test("verifyImage($algebra): eq. (23) = relational image over the crisp graph, both = derivation") {
                RelationLaws.verifyImage(set, square, ints, images, algebra)
            }
        }
    }
})
