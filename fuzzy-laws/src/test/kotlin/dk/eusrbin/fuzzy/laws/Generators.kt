package dk.eusrbin.fuzzy.laws

import dk.eusrbin.fuzzy.algebra.Algebra
import dk.eusrbin.fuzzy.algebra.Negations
import dk.eusrbin.fuzzy.algebra.OrdinalSummand
import dk.eusrbin.fuzzy.algebra.TNorm
import dk.eusrbin.fuzzy.algebra.TNorms
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.of

/**
 * Generators for `fuzzy-laws`' own tests.
 *
 * ## Why kotest-property here but not in the published API
 *
 * SCAFFOLD_PROMPT asks for property-based testing and for the choice to be
 * justified. **kotest-property**, because:
 *
 * - it is the mature property library for Kotlin, and this is a Kotlin/JVM-only
 *   project (CLAUDE.md §1) — jqwik is Java-first and its Kotlin ergonomics are
 *   poor; Quickcheck-style ports are unmaintained;
 * - `Arb` composes, which is what "random **algebras**" needs: a generated
 *   Hamacher parameter has to become a t-norm, then a De Morgan triple, then an
 *   algebra, and `map`/`flatMap` do that in one line;
 * - shrinking gives a minimal counterexample when a *test* fails, which is
 *   worth real money at the boundaries of `[0,1]`;
 * - it is already the JUnit-5-compatible runner in the version catalog, so it
 *   costs no additional dependency.
 *
 * It is deliberately **test-scope only**. [Sampling] explains at length why the
 * published `check`/`verify` API hand-rolls its sampling instead: CLAUDE.md §9
 * forbids coroutines in public API and `checkAll` is `suspend`, and CLAUDE.md §7
 * makes `fuzzy-laws` a consumable artifact that must not conscript its users
 * into a test framework. The split is the point — kotest exercises the suites
 * from outside, exactly as `fuzzy-laws` exercises `fuzzy-algebra` from outside.
 */
internal object Generators {

    /**
     * Degrees in `[0,1]`.
     *
     * Built with the `arbitrary` builder rather than `Arb.double(0.0, 1.0)`
     * because the stock double arb can emit non-finite edge cases, and `NaN` is
     * not a degree ([dk.eusrbin.fuzzy.algebra.Degrees]). Feeding one in would
     * test nothing but our willingness to propagate it.
     *
     * The edge cases are the same ones [Sampling] pins, and for the same reasons.
     */
    val degree: Arb<Double> = arbitrary(
        edgecases = listOf(0.0, 1.0, 0.5, 0.25, 0.75, Double.MIN_VALUE, 1.0 - Math.ulp(1.0)),
    ) { rs -> rs.random.nextDouble() }

    /**
     * Degrees excluding subnormals.
     *
     * For when the *generic bisection* residuum is under test rather than a
     * closed form. [dk.eusrbin.fuzzy.algebra.TNorm.residuum] documents why:
     * bisection reads its supremum off `apply`, so it computes the supremum of
     * the floating-point t-norm — and at `a = Double.MIN_VALUE` the Product
     * underflows (`MIN_VALUE · 0.5 == 0.0`), giving the float function a flat
     * region the real function does not have. Bisection then reports a supremum
     * near 1 where `b/a` says 0.
     *
     * That divergence is real and documented, not a bug to paper over — so it is
     * excluded here rather than absorbed into a tolerance, which would hide it.
     * [degree] keeps `MIN_VALUE`; the law suites pass with it.
     */
    val normalDegree: Arb<Double> = arbitrary(
        edgecases = listOf(0.0, 1.0, 0.5, 0.25, 0.75, 1.0 - Math.ulp(1.0)),
    ) { rs -> rs.random.nextDouble() }

    /** The three named algebras (CLAUDE.md §6). */
    val builtInAlgebra: Arb<Algebra> = Arb.of(Algebra.BUILT_INS)

    /** The three fundamental continuous t-norms (**Mostert–Shields**). */
    val basisTNorm: Arb<TNorm> = Arb.of(TNorms.CONTINUOUS_BASIS)

    /**
     * Hamacher t-norms with `γ ∈ [0, 8]`.
     *
     * Covers the Hamacher product (`γ = 0`), Product (`γ = 1`) and Einstein
     * (`γ = 2`) landmarks, plus the strictly-increasing tail.
     */
    val hamacherTNorm: Arb<TNorm> = arbitrary { rs ->
        TNorms.hamacher(rs.random.nextDouble() * 8.0)
    }

    /**
     * Ordinal sums of one or two basis summands over disjoint intervals.
     *
     * The construction that, with [TNorms.CONTINUOUS_BASIS], generates every
     * continuous t-norm — so generating these is generating (a slice of) the
     * whole continuous class, which is exactly what CLAUDE.md §6's "generating
     * basis, not a grab-bag" claims to buy.
     *
     * Intervals are laid out on an ordered grid, guaranteeing disjointness by
     * construction rather than by rejection sampling.
     */
    val ordinalSumTNorm: Arb<TNorm> = arbitrary { rs ->
        val cuts = List(4) { rs.random.nextDouble() }.sorted()
        val summands = buildList {
            if (cuts[1] - cuts[0] > 1e-3) {
                add(OrdinalSummand(cuts[0], cuts[1], TNorms.CONTINUOUS_BASIS[rs.random.nextInt(3)]))
            }
            if (cuts[3] - cuts[2] > 1e-3) {
                add(OrdinalSummand(cuts[2], cuts[3], TNorms.CONTINUOUS_BASIS[rs.random.nextInt(3)]))
            }
        }
        TNorms.ordinalSum(summands)
    }

    /**
     * Algebras built through the safe door: [Algebra.deMorgan] over a generated
     * Hamacher t-norm, so the triple is dual by construction.
     */
    val generatedAlgebra: Arb<Algebra> = arbitrary { rs ->
        val gamma = rs.random.nextDouble() * 8.0
        Algebra.deMorgan("Hamacher(γ=$gamma)", TNorms.hamacher(gamma), Negations.STANDARD)
    }
}
