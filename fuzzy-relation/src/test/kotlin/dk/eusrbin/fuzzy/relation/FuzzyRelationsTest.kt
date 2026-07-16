package dk.eusrbin.fuzzy.relation

import dk.eusrbin.fuzzy.algebra.Algebra
import dk.eusrbin.fuzzy.set.Enumerable
import dk.eusrbin.fuzzy.set.MembershipFn
import dk.eusrbin.fuzzy.set.Product
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
 * Behavioural pins for `fuzzy-relation` — hand-computed values, the Verdict
 * paths, and the eq. (23) guard. The law suites live in `fuzzy-laws`
 * (`RelationLaws`); this file is the known-values layer under them.
 */
class FuzzyRelationsTest : FunSpec({

    // ---- Fixtures: two relations over V = {0, 1}, as explicit tables --------

    val aTable = mapOf(
        (0 to 0) to 0.8, (0 to 1) to 0.3,
        (1 to 0) to 0.5, (1 to 1) to 1.0,
    )
    val bTable = mapOf(
        (0 to 0) to 0.6, (0 to 1) to 0.2,
        (1 to 0) to 0.9, (1 to 1) to 0.4,
    )
    val a = MembershipFn<Pair<Int, Int>> { p -> aTable.getValue(p) }
    val b = MembershipFn<Pair<Int, Int>> { p -> bTable.getValue(p) }
    val v = Enumerable.of(0, 1)

    context("compose — Zadeh p.346, hand-computed") {

        test("sup-min matches the hand computation") {
            val composed = FuzzyRelations.compose(a, b, v)
            // f(x,y) = max(min(a(x,0), b(0,y)), min(a(x,1), b(1,y)))
            composed.apply(0 to 0) shouldBe 0.6 // max(min(.8,.6), min(.3,.9)) = max(.6,.3)
            composed.apply(0 to 1) shouldBe 0.3 // max(min(.8,.2), min(.3,.4)) = max(.2,.3)
            composed.apply(1 to 0) shouldBe 0.9 // max(min(.5,.6), min(1,.9)) = max(.5,.9)
            composed.apply(1 to 1) shouldBe 0.4 // max(min(.5,.2), min(1,.4)) = max(.2,.4)
        }

        test("sup-product is the same mechanism at another parameter (§6)") {
            val composed = FuzzyRelations.compose(a, b, v, Algebra.PRODUCT)
            composed.apply(0 to 0) shouldBe 0.8 * 0.6 // max(.48, .27)
            composed.apply(1 to 0) shouldBe 1.0 * 0.9 // max(.30, .90)
        }

        test("heterogeneous legs compose in path order — the types carry the semantics (§21.4)") {
            // X = String, V = Int, Y = Double: compose(first, second) reads x → v → y.
            val first = MembershipFn<Pair<String, Int>> { (s, i) -> if (s.length == i) 1.0 else 0.2 }
            val second = MembershipFn<Pair<Int, Double>> { (i, d) -> if (i.toDouble() == d) 0.9 else 0.1 }
            val composed: MembershipFn<Pair<String, Double>> = FuzzyRelations.compose(first, second, v)
            // "x" has length 1: path "x" → 1 → 1.0 gives min(1.0, 0.9).
            composed.apply("x" to 1.0) shouldBe 0.9
        }
    }

    context("eq. (22) — the preimage, pointwise") {

        test("f_A(x) = f_B(T(x)), no domain anywhere") {
            val setOnY = MembershipFn<Int> { y -> if (y == 4) 0.7 else 0.1 }
            val preimage = FuzzyRelations.preimageUnder(setOnY, Mapping { x: Int -> x * x })
            preimage.apply(2) shouldBe 0.7
            preimage.apply(-2) shouldBe 0.7
            preimage.apply(3) shouldBe 0.1
        }
    }

    context("eq. (23) — the image, guarded (§21.5)") {

        val degrees = mapOf(-2 to 0.1, -1 to 0.3, 0 to 0.5, 1 to 0.7, 2 to 0.9)
        val set = MembershipFn<Int> { x -> degrees.getValue(x) }
        val square = Mapping { x: Int -> x * x }
        val ints = Enumerable.of(-2, -1, 0, 1, 2)

        test("f_B(y) = Max over the preimage — Zadeh's own finite Max, verbatim") {
            val image = FuzzyRelations.imageUnder(set, square, ints)
            image.apply(4) shouldBe 0.9 // max(f(-2), f(2)) = max(0.1, 0.9)
            image.apply(1) shouldBe 0.7 // max(f(-1), f(1)) = max(0.3, 0.7)
            image.apply(0) shouldBe 0.5
        }

        test("an empty preimage over an exhaustive domain is honestly 0.0 — no x maps there") {
            FuzzyRelations.imageUnder(set, square, ints).apply(3) shouldBe 0.0
        }

        test("a Sampled domain is refused — the fold would return 0.0 as the image") {
            val exception = shouldThrow<IllegalArgumentException> {
                FuzzyRelations.imageUnder(
                    MembershipFn { x: Double -> exp(-x * x) },
                    Mapping { x: Double -> x * x },
                    Sampled.of(-2.0, 2.0, 101),
                )
            }
            exception.message.shouldContain("exhaustive")
        }

        test("Product(Enumerable, Enumerable) is exhaustive and welcome — §16.4's motivating case") {
            // The image of a relation (a fuzzy set over pairs) under first-projection:
            // exactly the constrained Sup an Enumerable-only signature would have refused.
            val pairs: Product<Int, Int> = Product.of(v, v)
            val image = FuzzyRelations.imageUnder(a, Mapping { p: Pair<Int, Int> -> p.first }, pairs)
            image.apply(0) shouldBe 0.8 // max(a(0,0), a(0,1)) = max(0.8, 0.3)
            image.apply(1) shouldBe 1.0 // max(a(1,0), a(1,1)) = max(0.5, 1.0)
        }
    }

    context("the relational image (§21.6)") {

        test("hand-computed sup-min over a table") {
            val set = MembershipFn<Int> { x -> if (x == 0) 1.0 else 0.4 }
            val image = FuzzyRelations.imageUnderRelation(set, a, v)
            // f(y) = max(min(set(0), a(0,y)), min(set(1), a(1,y)))
            image.apply(0) shouldBe 0.8 // max(min(1,.8), min(.4,.5)) = max(.8,.4)
            image.apply(1) shouldBe 0.4 // max(min(1,.3), min(.4,1)) = max(.3,.4)
        }

        test("sound over a Sampled domain — a fold of degrees, no selection anywhere") {
            val set = MembershipFn<Double> { x -> exp(-x * x) }
            val near = MembershipFn<Pair<Double, Double>> { (x, y) -> exp(-abs(x - y)) }
            val image = FuzzyRelations.imageUnderRelation(set, near, Sampled.of(-2.0, 2.0, 101))
            // No exact claim — the point is that it returns an honest degree, not initial-as-answer.
            val degree = image.apply(0.5)
            withClue("a grid Sup of continuous degrees is a genuine lower bound, not 0.0") {
                (degree > 0.5) shouldBe true
                (degree <= 1.0) shouldBe true
            }
        }
    }

    context("the queries (§21.7)") {

        // A crisp total order: f(x,y) = 1 iff x ≤ y. Reflexive, min-transitive,
        // NOT symmetric.
        val leq = MembershipFn<Pair<Int, Int>> { (x, y) -> if (x <= y) 1.0 else 0.0 }
        val ints = Enumerable.of(1, 2, 3)

        test("reflexivity of ≤ is Proven over an Enumerable — a ∀ that visited everything") {
            FuzzyRelations.findNonReflexivity(leq, ints)
                .shouldBeInstanceOf<Verdict.Proven<Int>>()
        }

        test("a subnormal diagonal is Refuted with the x that breaks it") {
            val almostReflexive = MembershipFn<Pair<Int, Int>> { (x, y) ->
                if (x == y) (if (x == 2) 0.9 else 1.0) else 0.0
            }
            val verdict = FuzzyRelations.findNonReflexivity(almostReflexive, ints)
            verdict.shouldBeInstanceOf<Verdict.Refuted<Int>>()
            verdict.witness shouldBe 2
        }

        test("≤ is not symmetric, and the witness is an ordered pair that re-derives it") {
            val verdict = FuzzyRelations.findNonSymmetry(leq, ints)
            verdict.shouldBeInstanceOf<Verdict.Refuted<Pair<Int, Int>>>()
            val (x, y) = verdict.witness!!
            leq.apply(x to y) shouldBe (1.0 - leq.apply(y to x)) // 1 vs 0, either orientation
        }

        test("symmetry Proven through Product(Enumerable, Enumerable) — the §16.4 case, exercised") {
            val distance = MembershipFn<Pair<Int, Int>> { (x, y) -> 1.0 / (1 + abs(x - y)) }
            FuzzyRelations.findNonSymmetry(distance, ints)
                .shouldBeInstanceOf<Verdict.Proven<Pair<Int, Int>>>()
        }

        test("symmetry over a Sampled factor is only ever NotRefuted — the product inherits the grid") {
            val distance = MembershipFn<Pair<Double, Double>> { (x, y) -> 1.0 / (1 + abs(x - y)) }
            FuzzyRelations.findNonSymmetry(distance, Sampled.of(0.0, 1.0, 17))
                .shouldBeInstanceOf<Verdict.NotRefuted<Pair<Double, Double>>>()
        }

        test("min-transitivity of a crisp order is Proven — an exhaustive ∀ over X³") {
            FuzzyRelations.findNonTransitivity(leq, ints)
                .shouldBeInstanceOf<Verdict.Proven<TransitivityWitness<Int>>>()
        }

        test("product-transitivity of a crisp order is Proven too — 0/1 degrees are exact under ×") {
            FuzzyRelations.findNonTransitivity(leq, ints, Algebra.PRODUCT)
                .shouldBeInstanceOf<Verdict.Proven<TransitivityWitness<Int>>>()
        }

        test("exp(−|x−y|) similarity is NOT min-transitive, and the witness reproduces") {
            // Through the midpoint: min(e⁻¹, e⁻¹) = e⁻¹ > e⁻² = f(0, 2).
            val similarity = MembershipFn<Pair<Double, Double>> { (x, y) -> exp(-abs(x - y)) }
            val points = Enumerable.of(0.0, 1.0, 2.0)
            val verdict = FuzzyRelations.findNonTransitivity(similarity, points)
            verdict.shouldBeInstanceOf<Verdict.Refuted<TransitivityWitness<Double>>>()
            val witness = verdict.witness!!
            // §19.7(3): the witness must re-derive by hand.
            val composedAgain = minOf(
                similarity.apply(witness.x to witness.via),
                similarity.apply(witness.via to witness.y),
            )
            composedAgain shouldBe witness.composed
            similarity.apply(witness.x to witness.y) shouldBe witness.direct
            (witness.composed > witness.direct) shouldBe true
        }
    }
})
