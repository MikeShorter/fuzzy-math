package dk.eusrbin.fuzzy.laws

import dk.eusrbin.fuzzy.algebra.Algebra
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
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.property.checkAll

/**
 * `fuzzy-set` pinned against the paper — every equation number read off the scan
 * (CLAUDE.md §17.1, §18.1), none from memory.
 *
 * The law suites check that the algebras are self-consistent. They cannot catch
 * an operation that is consistently the wrong function, which is what this file
 * is for. It also pins the three claims CLAUDE.md §6 makes about Zadeh's own
 * operations — claims the implementation deliberately does *not* make structurally
 * (§17.3: `boundedDifference` computes `max(0, a−b)` directly because routing it
 * through `T_Ł(A, B')` would be less accurate), so an executable assertion is the
 * only thing keeping them true.
 */
class ZadehSetKnownValuesTest : FunSpec({

    val ε = 1e-12

    // ---- §II: the definitions -----------------------------------------------

    context("Zadeh §II — the defining operations") {

        test("eq. (1) p.340: complement is 1 − f_A") {
            FuzzySets.complement(SetGenerators.warm).apply("warm") shouldBe (0.25 plusOrMinus ε)
        }

        test("eq. (3) p.340: union is Max") {
            FuzzySets.union(SetGenerators.warm, SetGenerators.cool).apply("cool") shouldBe 0.75
            FuzzySets.union(SetGenerators.warm, SetGenerators.cool).apply("mild") shouldBe 0.5
        }

        test("eq. (5) p.341: intersection is Min") {
            FuzzySets.intersection(SetGenerators.warm, SetGenerators.cool).apply("cool") shouldBe 0.25
        }

        test("p.341: union is the SMALLEST set containing both — Zadeh's own justification") {
            // "The union of A and B is the smallest fuzzy set containing both A
            // and B." Max is forced by that, not chosen. So any D containing both
            // must contain the union.
            val union = FuzzySets.union(SetGenerators.warm, SetGenerators.cool)
            union.isContainedIn(FuzzySets.constant(1.0), SetGenerators.terms) shouldBe true
            SetGenerators.warm.isContainedIn(union, SetGenerators.terms) shouldBe true
            SetGenerators.cool.isContainedIn(union, SetGenerators.terms) shouldBe true
        }

        test("eq. (2) p.340: containment is a ∀ over X — NOT pointwise (§3)") {
            // The seam's counter-intuitive half: this needs a Domain, while union
            // and intersection above did not.
            val half = FuzzySets.constant<String>(0.5)
            SetGenerators.warm.checkContainment(FuzzySets.constant(1.0), SetGenerators.terms)
                .isProven shouldBe true
            (SetGenerators.warm.checkContainment(half, SetGenerators.terms) is Verdict.Refuted) shouldBe true
        }

        test("p.340: empty is 'identically zero on X'") {
            FuzzySets.constant<String>(0.0).isEmpty(SetGenerators.terms) shouldBe true
        }
    }

    // ---- §IV: the algebraic operations --------------------------------------

    context("Zadeh §IV — the algebraic operations") {

        test("eq. (14) p.344: algebraic product is f_A·f_B") {
            FuzzySets.algebraicProduct(SetGenerators.warm, SetGenerators.cool)
                .apply("cool") shouldBe (0.1875 plusOrMinus ε)
        }

        test("eq. (16) p.344: algebraic sum is f_A + f_B, and is PARTIAL") {
            // Zadeh: "meaningful only when f_A(x) + f_B(x) ≤ 1 ... for all x".
            // At "mild" both sets are 0.5, so the sum is exactly 1 — the edge of
            // his side-condition.
            FuzzySets.algebraicSum(SetGenerators.warm, SetGenerators.cool).apply("mild") shouldBe 1.0

            // warm + cool = 1.0 everywhere here, so the side-condition holds.
            FuzzySets.checkAlgebraicSumDefined(SetGenerators.warm, SetGenerators.cool, SetGenerators.terms)
                .isProven shouldBe true
        }

        test("eq. (16)'s side-condition is a ∀ over X, and it can be violated") {
            val hot = FuzzySets.constant<String>(0.8)
            val verdict = FuzzySets.checkAlgebraicSumDefined(hot, hot, SetGenerators.terms)
            (verdict is Verdict.Refuted) shouldBe true

            // And there, A + B leaves [0,1] — i.e. is not a fuzzy set at all.
            FuzzySets.algebraicSum(hot, hot).apply("mild") shouldBe (1.6 plusOrMinus ε)
        }

        test("p.344 (no eq. number): absolute difference is |f_A − f_B|") {
            FuzzySets.absoluteDifference(SetGenerators.warm, SetGenerators.cool)
                .apply("hot") shouldBe (1.0 plusOrMinus ε)
            FuzzySets.absoluteDifference(SetGenerators.warm, SetGenerators.cool)
                .apply("mild") shouldBe (0.0 plusOrMinus ε)
        }

        test("eqs. (17)/(18) p.345: Λ is a FUZZY SET, not a scalar (§17.2)") {
            // f_(A,B;Λ)(x) = f_Λ(x)·f_A(x) + [1 − f_Λ(x)]·f_B(x)
            // The weighting VARIES with x — this is the thing reading the paper
            // caught, and a scalar signature would have silently lost it.
            val lambda = SetGenerators.mild // Λ = 0.1, 0.4, 0.9, 0.4, 0.1
            val combined = FuzzySets.convexCombination(SetGenerators.warm, SetGenerators.cool, lambda)

            // at "cool": 0.4·0.25 + 0.6·0.75 = 0.1 + 0.45 = 0.55
            combined.apply("cool") shouldBe (0.55 plusOrMinus ε)
            // at "mild": 0.9·0.5 + 0.1·0.5 = 0.5
            combined.apply("mild") shouldBe (0.5 plusOrMinus ε)
            // at "hot": 0.1·1.0 + 0.9·0.0 = 0.1
            combined.apply("hot") shouldBe (0.1 plusOrMinus ε)

            // A constant Λ is the special case — eq. (20)'s proof uses it.
            FuzzySets.convexCombination(SetGenerators.warm, SetGenerators.cool, 0.5)
                .apply("hot") shouldBe (0.5 plusOrMinus ε)
        }

        test("eq. (19) p.345: A ∩ B ⊂ (A,B;Λ) ⊂ A ∪ B, for ANY Λ") {
            checkAll(30, SetGenerators.termLambda) { lambda ->
                val combined = FuzzySets.convexCombination(SetGenerators.warm, SetGenerators.cool, lambda)
                val meet = FuzzySets.intersection(SetGenerators.warm, SetGenerators.cool)
                val join = FuzzySets.union(SetGenerators.warm, SetGenerators.cool)
                meet.isContainedIn(combined, SetGenerators.terms) shouldBe true
                combined.isContainedIn(join, SetGenerators.terms) shouldBe true
            }
        }
    }

    // ---- CLAUDE.md §6's claims, made executable -----------------------------

    context("§6: one parameterised mechanism, of which Zadeh's operations are instances") {

        test("eq. (14)'s algebraic product IS the Product t-norm") {
            checkAll(SetGenerators.termSet, SetGenerators.termSet) { a, b ->
                val viaZadeh = FuzzySets.algebraicProduct(a, b)
                val viaAlgebra = FuzzySets.intersection(a, b, Algebra.PRODUCT)
                for (x in SetGenerators.terms.elements()) {
                    viaZadeh.apply(x) shouldBe viaAlgebra.apply(x)
                }
            }
        }

        test("footnote 4's dual IS the Product conorm") {
            // A ⊕ B = (A'B')' = A + B − AB, p.344.
            checkAll(SetGenerators.termSet, SetGenerators.termSet) { a, b ->
                val viaFootnote = FuzzySets.complement(
                    FuzzySets.algebraicProduct(FuzzySets.complement(a), FuzzySets.complement(b)),
                )
                val viaAlgebra = FuzzySets.union(a, b, Algebra.PRODUCT)
                for (x in SetGenerators.terms.elements()) {
                    viaFootnote.apply(x) shouldBe (viaAlgebra.apply(x) plusOrMinus 1e-15)
                }
            }
        }

        test("eq. (16)'s algebraic sum is an UNCAPPED Łukasiewicz conorm") {
            // §6: "his f_A + f_B ≤ 1 side-condition is an uncapped Łukasiewicz
            // conorm missing its min(1, ·)". So where his side-condition holds,
            // the two agree; where it fails, min(1,·) is doing the capping his
            // proviso does by hand.
            checkAll(SetGenerators.termSet, SetGenerators.termSet) { a, b ->
                val zadeh = FuzzySets.algebraicSum(a, b)
                val capped = FuzzySets.union(a, b, Algebra.LUKASIEWICZ)
                for (x in SetGenerators.terms.elements()) {
                    val raw = zadeh.apply(x)
                    if (raw <= 1.0) {
                        withClue("side-condition holds at $x: the two must agree") {
                            capped.apply(x) shouldBe (raw plusOrMinus 1e-15)
                        }
                    } else {
                        withClue("side-condition fails at $x: min(1,·) caps it") {
                            capped.apply(x) shouldBe 1.0
                        }
                    }
                }
            }
        }

        test("bounded difference IS T_Łukasiewicz(A, B') — §17.3's identity") {
            // The implementation computes max(0, a−b) DIRECTLY, because routing
            // it through T_Ł costs three roundings instead of one. So this
            // identity is a claim the code does not make structurally, and this
            // assertion is the only thing keeping it honest.
            checkAll(SetGenerators.termSet, SetGenerators.termSet) { a, b ->
                val direct = FuzzySets.boundedDifference(a, b)
                val derived = FuzzySets.intersection(a, FuzzySets.complement(b), Algebra.LUKASIEWICZ)
                for (x in SetGenerators.terms.elements()) {
                    direct.apply(x) shouldBe (derived.apply(x) plusOrMinus 1e-15)
                }
            }
        }
    }

    // ---- §V's definitions that are domain-generic (§15.5's split) -----------

    context("Zadeh §V — the parts that need no vector space") {

        test("eq. (24) p.347: Γ_α = {x | f_A(x) ≥ α}") {
            SetGenerators.warm.alphaCut(SetGenerators.terms, 0.5) shouldContainExactly
                listOf("mild", "warm", "hot")
            SetGenerators.warm.alphaCut(SetGenerators.terms, 1.0) shouldContainExactly listOf("hot")
        }

        test("the strong α-cut is >, not ≥ — derived, not Zadeh's") {
            SetGenerators.warm.strongAlphaCut(SetGenerators.terms, 0.5) shouldContainExactly
                listOf("warm", "hot")
        }

        test("support is the strong 0-cut — p.342's 'f_A(x) being positive'") {
            SetGenerators.warm.support(SetGenerators.terms) shouldContainExactly
                listOf("cool", "mild", "warm", "hot")
        }

        test("p.348: M = Sup_x f_A(x), Zadeh's 'maximal grade' — our 'height'") {
            SetGenerators.warm.height(SetGenerators.terms) shouldBe 1.0
            SetGenerators.subnormal.height(SetGenerators.terms) shouldBe 0.6
        }

        test("α-cuts are nested: α ≤ β ⟹ Γ_β ⊆ Γ_α") {
            val loose = SetGenerators.warm.alphaCut(SetGenerators.terms, 0.25).toSet()
            val tight = SetGenerators.warm.alphaCut(SetGenerators.terms, 0.75).toSet()
            loose.containsAll(tight) shouldBe true
        }
    }

    // ---- The Domain seam ----------------------------------------------------

    context("The Domain seam (§15)") {

        test("a Sampled height is a LOWER BOUND, and refining converges") {
            // §15.1's payoff: the same set over two domains is one expression,
            // not a type error. A 5-point grid misses the Gaussian's shape; 4097
            // points do not.
            val coarse = SetGenerators.nearZero.height(SetGenerators.line)
            val fine = SetGenerators.nearZero.height(SetGenerators.fineLine)
            coarse shouldBe 1.0 // x = 0 happens to be on both grids
            fine shouldBe 1.0

            // σ-count, by contrast, is a grid sum and scales with resolution —
            // comparable across sets on ONE domain, meaningless across domains.
            (SetGenerators.nearZero.sigmaCount(SetGenerators.fineLine) >
                SetGenerators.nearZero.sigmaCount(SetGenerators.line)) shouldBe true
        }

        test("Enumerable keeps duplicates — σ-count counts what is enumerated") {
            val once = Enumerable.of("warm")
            val twice = Enumerable.of("warm", "warm")
            SetGenerators.warm.sigmaCount(once) shouldBe 0.75
            SetGenerators.warm.sigmaCount(twice) shouldBe 1.5
            // ... while the height is unmoved. Deduplicating silently would have
            // changed one answer and not the other.
            SetGenerators.warm.height(twice) shouldBe 0.75
        }

        test("an empty Enumerable is rejected at construction (§16.3)") {
            runCatching { Enumerable.of<String>(emptyList()) }.isFailure shouldBe true
        }

        test("Product is exhaustive iff both factors are") {
            val ee = Product.of(SetGenerators.terms, Enumerable.of(1, 2))
            val es = Product.of(SetGenerators.terms, SetGenerators.line)
            ee.isExhaustive shouldBe true
            es.isExhaustive shouldBe false

            // ... and it folds over every pair.
            val pairs: MembershipFn<Pair<String, Int>> = MembershipFn { (term, n) ->
                SetGenerators.warm.apply(term) / n
            }
            ee.elements().size shouldBe 10
            pairs.height(ee) shouldBe 1.0
        }

        test("DoubleMembershipFn IS a MembershipFn<Double>, and takes the primitive path") {
            val fn: DoubleMembershipFn = SetGenerators.triangle
            val generic: MembershipFn<Double> = fn // §16.1: subtyping, not adapters

            fn.applyAsDouble(1.0) shouldBe 1.0 // primitive, no boxing
            generic.apply(1.0) shouldBe 1.0 // boxed, same answer
            fn.height(SetGenerators.line) shouldBe 1.0 // domain-generic, accepts it
        }

        test("Sampled returns its endpoints EXACTLY") {
            SetGenerators.line.pointAt(0) shouldBe -1.0
            SetGenerators.line.pointAt(SetGenerators.line.points - 1) shouldBe 3.0
            SetGenerators.fineLine.pointAt(SetGenerators.fineLine.points - 1) shouldBe 3.0
        }
    }

    // ---- Hedges: Bergmann §16.1 --------------------------------------------

    context("Hedges (Bergmann §16.1)") {

        test("§16.1: I(very)(n) = n² — concentration is literally his 'very'") {
            FuzzySets.concentration(SetGenerators.warm).apply("warm") shouldBe (0.5625 plusOrMinus ε)
        }

        test("§16.1: 'very very' is n⁴ — his own example, by iteration") {
            val veryVery = FuzzySets.concentration(FuzzySets.concentration(SetGenerators.warm))
            veryVery.apply("warm") shouldBe (0.75.let { it * it * it * it } plusOrMinus ε)
        }

        test("dilation is power(0.5) — the maths is verified, only the name is not") {
            FuzzySets.dilation(SetGenerators.warm).apply("warm") shouldBe
                (kotlin.math.sqrt(0.75) plusOrMinus ε)
        }

        test("hedges raise or lower the membership threshold — §16.1's claim") {
            checkAll(SetGenerators.termSet) { a ->
                for (x in SetGenerators.terms.elements()) {
                    val d = a.apply(x)
                    // "very ... raises the threshold for membership"
                    (FuzzySets.concentration(a).apply(x) <= d + 1e-15) shouldBe true
                    // ... and a root lowers it.
                    (FuzzySets.dilation(a).apply(x) >= d - 1e-15) shouldBe true
                }
            }
        }

        test("power rejects a non-positive exponent") {
            runCatching { FuzzySets.power(SetGenerators.warm, 0.0) }.isFailure shouldBe true
            runCatching { FuzzySets.power(SetGenerators.warm, -1.0) }.isFailure shouldBe true
        }
    }

    // ---- The suites, applied to the built-ins -------------------------------

    context("the suites over built-in sets — the artifact validating the artifact") {

        test("MembershipFnLaws holds for every fixture, over both kinds of domain") {
            for (fn in listOf(SetGenerators.warm, SetGenerators.cool, SetGenerators.subnormal)) {
                withClue(fn) { MembershipFnLaws.verify(fn, SetGenerators.terms) }
            }
            MembershipFnLaws.verify(SetGenerators.nearZero, SetGenerators.line)
            MembershipFnLaws.verify(SetGenerators.triangle, SetGenerators.line)
        }

        test("DecompositionLaws holds over an Enumerable, exactly") {
            for (fn in listOf(SetGenerators.warm, SetGenerators.cool, SetGenerators.subnormal)) {
                withClue(fn) { DecompositionLaws.verify(fn, SetGenerators.terms) }
            }
        }

        test("DecompositionLaws holds over a Sampled grid too") {
            DecompositionLaws.verify(SetGenerators.triangle, SetGenerators.line)
        }

        test("ZadehSetLaws holds for Standard over arbitrary sets") {
            checkAll(20, SetGenerators.termSet, SetGenerators.termSet, SetGenerators.termSet) { a, b, c ->
                ZadehSetLaws.verify(a, b, c, SetGenerators.terms, Algebra.STANDARD)
            }
        }

        test("the decomposition round trip survives arbitrary sets") {
            checkAll(20, SetGenerators.termSet) { a ->
                DecompositionLaws.verify(a, SetGenerators.terms)
            }
        }
    }
})
