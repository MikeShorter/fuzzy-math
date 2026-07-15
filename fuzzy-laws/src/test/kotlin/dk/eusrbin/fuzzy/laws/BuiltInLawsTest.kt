package dk.eusrbin.fuzzy.laws

import dk.eusrbin.fuzzy.algebra.Algebra
import dk.eusrbin.fuzzy.algebra.Degrees
import dk.eusrbin.fuzzy.algebra.Negations
import dk.eusrbin.fuzzy.algebra.TConorms
import dk.eusrbin.fuzzy.algebra.TNorms
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.doubles.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.property.checkAll

/**
 * `fuzzy-laws` applied to the built-ins — the artifact validating the artifact.
 *
 * CLAUDE.md §12: shipping `fuzzy-algebra` and `fuzzy-laws` *together* "proves the
 * thesis — the laws artifact validates the algebra artifact from the outside."
 * This file is where that happens. `fuzzy-laws` has no privileged access to
 * `fuzzy-algebra`'s internals; it checks it exactly as a stranger's t-norm would
 * be checked.
 *
 * The property tests here (kotest-property — see [Generators] for why) go
 * further, using the **Mostert–Shields** basis and [TNorms.ordinalSum] to reach
 * beyond the named t-norms into the continuous class generally.
 */
class BuiltInLawsTest : FunSpec({

    // ---- The universal tier holds for everything ---------------------------

    context("TNormLaws — the universal tier") {

        test("every named t-norm is a t-norm") {
            for (tNorm in listOf(
                TNorms.GODEL,
                TNorms.PRODUCT,
                TNorms.LUKASIEWICZ,
                TNorms.DRASTIC,
                TNorms.NILPOTENT_MINIMUM,
                TNorms.hamacher(0.0),
                TNorms.hamacher(2.0),
            )) {
                withClue(tNorm) { TNormLaws.verify(tNorm) }
            }
        }

        test("Gödel passes at ε = 0 — min computes nothing, so nothing rounds") {
            TNormLaws.verify(TNorms.GODEL, Tolerance.EXACT)
        }

        test("Hamacher family — every γ in [0,8]") {
            checkAll(50, Generators.hamacherTNorm) { tNorm ->
                withClue(tNorm) { TNormLaws.verify(tNorm) }
            }
        }

        test("ordinal sums of basis norms — the whole continuous class, in principle") {
            checkAll(100, Generators.ordinalSumTNorm) { tNorm ->
                withClue(tNorm) { TNormLaws.verify(tNorm) }
            }
        }
    }

    context("TConormLaws — the universal tier, dual side") {

        test("every named t-conorm is a t-conorm") {
            for (tConorm in listOf(
                TConorms.GODEL,
                TConorms.PROBABILISTIC_SUM,
                TConorms.LUKASIEWICZ,
                TConorms.DRASTIC,
                TConorms.NILPOTENT_MAXIMUM,
                TConorms.hamacher(0.0),
                TConorms.hamacher(2.0),
            )) {
                withClue(tConorm) { TConormLaws.verify(tConorm) }
            }
        }

        test("max passes at ε = 0") {
            TConormLaws.verify(TConorms.GODEL, Tolerance.EXACT)
        }
    }

    // ---- The residuated tier -----------------------------------------------

    context("ResiduumLaws — the adjunction") {

        test("every left-continuous named t-norm satisfies residuation") {
            for (tNorm in listOf(
                TNorms.GODEL,
                TNorms.PRODUCT,
                TNorms.LUKASIEWICZ,
                TNorms.NILPOTENT_MINIMUM,
            )) {
                withClue(tNorm) { ResiduumLaws.verify(tNorm) }
            }
        }

        test("Hamacher — the bisection fallback satisfies the adjunction it was derived from") {
            // These have no closed-form residuum here; TNorm.residuum's generic
            // bisection is what is under test. CLAUDE.md §5's claim is that the
            // residuum is *determined* by the t-norm — so computing it
            // numerically must land where deriving it would.
            checkAll(30, Generators.hamacherTNorm) { tNorm ->
                withClue(tNorm) { ResiduumLaws.verify(tNorm) }
            }
        }

        test("ordinal sums — the structural residuum agrees with the adjunction") {
            checkAll(50, Generators.ordinalSumTNorm) { tNorm ->
                withClue(tNorm) { ResiduumLaws.verify(tNorm) }
            }
        }
    }

    // ---- The BL tier: exactly the continuous basis --------------------------

    context("BLAlgebraLaws — BL is the logic of continuous t-norms") {

        test("the Mostert–Shields basis is BL") {
            for (tNorm in TNorms.CONTINUOUS_BASIS) {
                withClue(tNorm) { BLAlgebraLaws.verify(tNorm) }
            }
        }

        test("Hamacher — continuous, therefore BL") {
            checkAll(30, Generators.hamacherTNorm) { tNorm ->
                withClue(tNorm) { BLAlgebraLaws.verify(tNorm) }
            }
        }

        test("ordinal sums of continuous norms are continuous, therefore BL") {
            // Mostert–Shields from the other direction: if ordinal sums of the
            // basis generate the continuous class, they had better all be BL.
            checkAll(50, Generators.ordinalSumTNorm) { tNorm ->
                withClue(tNorm) { BLAlgebraLaws.verify(tNorm) }
            }
        }
    }

    // ---- De Morgan ----------------------------------------------------------

    context("DeMorganLaws") {

        test("all three built-in algebras are De Morgan triples") {
            for (algebra in Algebra.BUILT_INS) {
                withClue(algebra) { DeMorganLaws.verify(algebra) }
            }
        }

        test("Algebra.deMorgan derives a dual conorm for any t-norm — by construction") {
            checkAll(30, Generators.generatedAlgebra) { algebra ->
                withClue(algebra) { DeMorganLaws.verify(algebra) }
            }
        }

        test("... and for the Sugeno and Yager negations too") {
            // TConorms.dualOf requires an involutive N. Both families are strong,
            // so the derived triple must satisfy De Morgan for every parameter.
            checkAll(20, Generators.degree) { t ->
                val lambda = -0.9 + t * 9.0 // λ ∈ (−0.9, 8.1), all > −1
                DeMorganLaws.verify(
                    Algebra.deMorgan("Sugeno(λ=$lambda)", TNorms.PRODUCT, Negations.sugeno(lambda)),
                    Tolerance.GENERAL,
                )
                val w = 0.25 + t * 4.0 // w ∈ [0.25, 4.25], all > 0
                DeMorganLaws.verify(
                    Algebra.deMorgan("Yager(w=$w)", TNorms.PRODUCT, Negations.yager(w)),
                    Tolerance.GENERAL,
                )
            }
        }
    }

    // ---- Closure: everything stays in [0,1] ---------------------------------

    context("closure — every operation returns a degree") {

        test("built-in algebras are closed on [0,1]") {
            checkAll(Generators.builtInAlgebra, Generators.degree, Generators.degree) { algebra, a, b ->
                for (result in listOf(
                    algebra.and(a, b),
                    algebra.or(a, b),
                    algebra.not(a),
                    algebra.implies(a, b),
                    algebra.iff(a, b),
                )) {
                    withClue("$algebra at ($a, $b) produced $result") {
                        Degrees.isDegree(result) shouldBe true
                    }
                }
            }
        }

        test("t-norms are bounded above by min, conorms below by max") {
            // T(a,b) ≤ min(a,b) ≤ max(a,b) ≤ S(a,b) — the boundary conditions
            // plus monotonicity force this for every t-norm and conorm.
            checkAll(Generators.builtInAlgebra, Generators.degree, Generators.degree) { algebra, a, b ->
                algebra.and(a, b) shouldBeLessThanOrEqual minOf(a, b)
                algebra.or(a, b) shouldBeGreaterThanOrEqual maxOf(a, b)
            }
        }

        test("Drastic is the least t-norm, Gödel the greatest") {
            checkAll(Generators.degree, Generators.degree) { a, b ->
                TNorms.DRASTIC.apply(a, b) shouldBeLessThanOrEqual TNorms.GODEL.apply(a, b)
            }
        }
    }
})
