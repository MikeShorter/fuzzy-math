package dk.eusrbin.fuzzy.algebra

/**
 * A **fuzzy implication**: `I: [0,1]² → [0,1]`, non-increasing in its first
 * argument, non-decreasing in its second, and agreeing with classical
 * implication on the corners:
 *
 * ```
 * I(0, 0) = 1     I(0, 1) = 1     I(1, 1) = 1     I(1, 0) = 0
 * ```
 *
 * Source: Bergmann 2008, §11.7 (implication), §12 (residuated lattices).
 *
 * ## Zadeh 1965 has no implication
 *
 * This is worth stating plainly, because it is the seam where this library
 * stops being a transcription of the 1965 paper and starts being t-norm theory.
 * CLAUDE.md §5: *"Zadeh 1965 has **no** implication. t-norm theory supplies one
 * canonically: the residuum."*
 *
 * Three families are provided, and they are not interchangeable:
 *
 * | Family | Definition | Factory |
 * |---|---|---|
 * | **R-implication** | `sup { z \| T(a,z) ≤ b }` | [TNorm.residuum], [Implications.residuum] |
 * | **S-implication** | `S(N(a), b)` | [Implications.sImplication] |
 * | **QL-implication** | `S(N(a), T(a,b))` | [Implications.qlImplication] |
 *
 * The R-family is privileged: CLAUDE.md §5 decides that the residuum is
 * **derived, not configured**, so [Algebra.implication] is always the residuum
 * of the bundle's t-norm and is never something you pass in. The S- and QL-
 * families are "provided as separate named families" — reachable, named, and
 * never silently substituted for the residuum.
 *
 * They coincide sometimes and diverge often. For Łukasiewicz the R- and S-
 * implications are both `min(1, 1−a+b)`; for Gödel they are not
 * ([Implications.GODEL] vs [Implications.KLEENE_DIENES]). Coincidence is a fact
 * about Łukasiewicz, not a general licence to swap them.
 *
 * ## Precondition, not validation
 *
 * `a` and `b` **must** lie in `[0,1]`. Not checked; see [Degrees].
 */
public fun interface Implication {

    /**
     * Applies the implication. `a` and `b` must be in `[0,1]`; result in `[0,1]`.
     *
     * This is the named method; [invoke] is Kotlin-only sugar (CLAUDE.md §9).
     */
    public fun apply(a: Double, b: Double): Double

    /** Kotlin sugar for [apply], letting you write `I(a, b)`. Java callers use [apply]. */
    public operator fun invoke(a: Double, b: Double): Double = apply(a, b)
}
