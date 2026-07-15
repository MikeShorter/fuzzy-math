package dk.eusrbin.fuzzy.algebra

/**
 * A **triangular conorm** (s-norm): the disjunction of a fuzzy logic, and the
 * union of a fuzzy set theory.
 *
 * A t-conorm is a function `S: [0,1]² → [0,1]` that is commutative,
 * associative, non-decreasing in each argument, and has `0` as neutral element:
 *
 * ```
 * S(a, b)       = S(b, a)                     commutativity
 * S(S(a,b), c)  = S(a, S(b,c))                associativity
 * b ≤ c         ⟹ S(a,b) ≤ S(a,c)             monotonicity
 * S(a, 0)       = a                           boundary / neutral element
 * ```
 *
 * Source: Bergmann 2008, §11.7; Klement, Mesiar & Pap 2000.
 *
 * The only difference from [TNorm] is the neutral element (`0` rather than `1`).
 * Every t-conorm is the [Negations.STANDARD]-dual of a t-norm and vice versa:
 * `S(a,b) = 1 − T(1−a, 1−b)`. See [TConorms.dualOf].
 *
 * Zadeh 1965 §III's union `max(f_A, f_B)` is the Gödel conorm
 * ([TConorms.MAXIMUM]); his footnote 4's `f_A + f_B − f_A f_B` is the Product
 * conorm ([TConorms.PROBABILISTIC_SUM]) — CLAUDE.md §6 notes he filed these
 * separately "without noticing they were the same shape". His §IV algebraic sum
 * `f_A + f_B` with the side-condition `f_A + f_B ≤ 1` is an uncapped
 * [TConorms.LUKASIEWICZ] missing its `min(1, ·)`.
 *
 * ## Why this is not just `TNorm`
 *
 * `TNorm` and `TConorm` are structurally identical — same signature, same laws
 * modulo the boundary condition — and are nonetheless **separate types**. This
 * is CLAUDE.md §9, verbatim: *"`TNorm` and `TConorm` are separate types despite
 * identical shape — the types exist to prevent mixing them up."*
 *
 * The failure being prevented is quiet. Pair an arbitrary t-norm with an
 * arbitrary conorm and De Morgan silently breaks (CLAUDE.md §6); nothing throws,
 * results just stop meaning what you think. A distinct type makes that a compile
 * error at the one place it can be caught. This is the same instinct as the
 * `Domain` seam in CLAUDE.md §3 — let the machine enforce what it can.
 *
 * ## Precondition, not validation
 *
 * `a` and `b` **must** lie in `[0,1]`. Not checked; see [Degrees].
 */
public fun interface TConorm {

    /**
     * Applies the conorm. `a` and `b` must be in `[0,1]`; the result is in `[0,1]`.
     *
     * This is the named method; [invoke] is Kotlin-only sugar (CLAUDE.md §9).
     */
    public fun apply(a: Double, b: Double): Double

    /** Kotlin sugar for [apply], letting you write `S(a, b)`. Java callers use [apply]. */
    public operator fun invoke(a: Double, b: Double): Double = apply(a, b)
}
