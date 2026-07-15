package dk.eusrbin.fuzzy.algebra

/**
 * A **fuzzy negation**: the complement of a fuzzy set theory, the negation of a
 * fuzzy logic.
 *
 * A negation is a function `N: [0,1] → [0,1]` that is non-increasing with
 * `N(0) = 1` and `N(1) = 0`. It is **strict** if it is strictly decreasing and
 * continuous, and **strong** (involutive) if additionally
 *
 * ```
 * N(N(a)) = a
 * ```
 *
 * Source: Bergmann 2008, §11.7; Klement, Mesiar & Pap 2000.
 *
 * Zadeh 1965 §II (p.340) defines only the standard case — the complement of `A`
 * is `f_A' = 1 − f_A` ([Negations.STANDARD]). The parametric families here are
 * from the later t-norm literature.
 *
 * ## Involutivity is load-bearing
 *
 * Every negation in [Negations] is strong. This is not incidental:
 *
 * - De Morgan duality (`S(a,b) = N(T(N(a), N(b)))`, [TConorms.dualOf]) only
 *   round-trips for involutive `N` — otherwise the "dual" of the dual is not
 *   the original. [dk.eusrbin.fuzzy.laws.DeMorganLaws] checks involutivity for
 *   exactly this reason.
 * - [Implications.sImplication] and [Implications.qlImplication] both rest on it.
 * - MV-algebras require it outright ([dk.eusrbin.fuzzy.laws.MVAlgebraLaws]).
 *
 * A non-involutive negation is a perfectly good `Negation`; it just cannot be
 * used to build a De Morgan triple. If you write one, `DeMorganLaws.check` will
 * tell you so rather than letting it fail silently downstream.
 *
 * ## Precondition, not validation
 *
 * `a` **must** lie in `[0,1]`. Not checked; see [Degrees].
 */
public fun interface Negation {

    /**
     * Applies the negation. `a` must be in `[0,1]`; the result is in `[0,1]`.
     *
     * This is the named method; [invoke] is Kotlin-only sugar (CLAUDE.md §9).
     */
    public fun apply(a: Double): Double

    /** Kotlin sugar for [apply], letting you write `N(a)`. Java callers use [apply]. */
    public operator fun invoke(a: Double): Double = apply(a)
}
