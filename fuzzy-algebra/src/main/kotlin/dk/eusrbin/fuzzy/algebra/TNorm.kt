package dk.eusrbin.fuzzy.algebra

/**
 * Bisection steps used by the generic [TNorm.residuum] fallback.
 *
 * Bisection on `[0,1]` halves the bracket each step, so 64 steps is past the
 * point where `lo` and `hi` become adjacent doubles; the loop's own
 * `mid <= lo || mid >= hi` guard is what actually terminates it. The constant
 * is a backstop against a pathological (non-monotone) `apply`.
 */
private const val RESIDUUM_BISECTION_STEPS = 64

/**
 * A **triangular norm**: the conjunction of a fuzzy logic, and the intersection
 * of a fuzzy set theory.
 *
 * A t-norm is a function `T: [0,1]² → [0,1]` that is commutative, associative,
 * non-decreasing in each argument, and has `1` as neutral element:
 *
 * ```
 * T(a, b)       = T(b, a)                     commutativity
 * T(T(a,b), c)  = T(a, T(b,c))                associativity
 * b ≤ c         ⟹ T(a,b) ≤ T(a,c)             monotonicity
 * T(a, 1)       = a                           boundary / neutral element
 * ```
 *
 * **Source:** Bergmann 2008, §11.7 — read and checked. (Klement, Mesiar & Pap
 * 2000 is §2's general t-norm reference; it is not on hand, so nothing here
 * hangs on it — CLAUDE.md §17.5.)
 * These four laws are the *universal* tier of [dk.eusrbin.fuzzy.laws.TNormLaws]
 * — they hold for every t-norm, and nothing else does (CLAUDE.md §7).
 *
 * Zadeh 1965 §II's intersection `min(f_A, f_B)` (eq. 5) is one instance of this
 * (the Gödel t-norm, [TNorms.GODEL]); his §IV "algebraic product" `f_A · f_B`
 * is another ([TNorms.PRODUCT]). He filed them separately without noting they
 * were the same shape. CLAUDE.md §6: "Min/max are not a special mechanism —
 * they are just the Gödel t-norm."
 *
 * ## Precondition, not validation
 *
 * `a` and `b` **must** lie in `[0,1]`. This is not checked — see [Degrees] for
 * why, and for the boundary helpers if you need them.
 *
 * ## Implementing this interface
 *
 * `TNorm` is a `fun interface` (CLAUDE.md §9), so a Java 8 lambda or a Clojure
 * `reify` satisfies it directly, with no boxing:
 *
 * ```java
 * TNorm einstein = (a, b) -> (a * b) / (2 - (a + b - a * b));
 * ```
 * ```clojure
 * (reify TNorm (apply [_ a b] (* a b)))
 * ```
 *
 * Having written one, hand it to `TNormLaws.verify(einstein)` from the
 * `fuzzy-laws` artifact to check it actually is a t-norm (CLAUDE.md §7).
 *
 * [TConorm] is a **separate type** despite having exactly this shape. That is
 * deliberate (CLAUDE.md §9): the types exist to stop you mixing them up.
 */
public fun interface TNorm {

    /**
     * Applies the norm. `a` and `b` must be in `[0,1]`; the result is in `[0,1]`.
     *
     * This is the named method. [invoke] is Kotlin-only sugar over it
     * (CLAUDE.md §9: "Operators are Kotlin-only sugar: always ship a named
     * method alongside").
     */
    public fun apply(a: Double, b: Double): Double

    /** Kotlin sugar for [apply], letting you write `T(a, b)`. Java callers use [apply]. */
    public operator fun invoke(a: Double, b: Double): Double = apply(a, b)

    /**
     * The **residuum** of this t-norm — its canonically induced implication.
     *
     * ```
     * a ⇒ b  =  sup { z ∈ [0,1] | T(a, z) ≤ b }
     * ```
     *
     * Source: Bergmann 2008, §11.7 and §12 (residuated lattices, BL-algebras).
     *
     * CLAUDE.md §5 is emphatic that this is **derived, not configured**: "for a
     * left-continuous t-norm the residuum is *determined*. The API computes it;
     * it is not a free parameter of [Algebra]." That decision is why this is a
     * method on `TNorm` rather than a constructor argument somewhere — you
     * cannot pair a t-norm with the wrong residuum, because you are never asked
     * to supply one.
     *
     * The defining property is an **adjunction** (`T(a,−) ⊣ (a ⇒ −)`):
     *
     * ```
     * T(a, z) ≤ b   ⟺   z ≤ (a ⇒ b)
     * ```
     *
     * verified by [dk.eusrbin.fuzzy.laws.ResiduumLaws].
     *
     * ## Left-continuity is a real precondition
     *
     * The adjunction holds **iff** `T` is left-continuous. The supremum above is
     * always *computable*, but for a non-left-continuous `T` it is not attained
     * and the adjunction genuinely fails — no residuum exists. [TNorms.DRASTIC]
     * is the standard example, and `ResiduumLaws.check(TNorms.DRASTIC)` reports
     * a real failure rather than a bug. See [TNorms.DRASTIC].
     *
     * ## The default implementation
     *
     * This default computes the supremum by bisection, exploiting monotonicity
     * of `T(a, ·)`: the set `{ z | T(a,z) ≤ b }` is a down-set, so its supremum
     * is the boundary between the `z` that satisfy the inequality and those that
     * do not. It converges to adjacent doubles, and costs ~60 calls to [apply].
     *
     * Every named t-norm in [TNorms] **overrides** this with its closed form,
     * so you only pay for bisection on a t-norm you wrote yourself — where a
     * correct-by-construction residuum you did not have to derive is a fair
     * trade for ~60 multiplications. Override it if you know the closed form.
     *
     * ## Known limitation: subnormal inputs
     *
     * Bisection reads the supremum off [apply], so it computes the supremum of
     * the **floating-point** t-norm, which parts company with the real one when
     * `apply` underflows. Concretely, for the Product t-norm at
     * `a = Double.MIN_VALUE`, `b = 0`:
     *
     * - the real residuum is `b/a = 0`;
     * - but `MIN_VALUE · 0.5` underflows to exactly `0.0`, so in `double`
     *   arithmetic `T(a, z) ≤ 0` holds for nearly every `z`, and bisection
     *   dutifully reports a supremum near `1`.
     *
     * Neither answer is a bug in the bisection; the float function genuinely has
     * that flat region. It does mean **the generic residuum is unreliable in the
     * subnormal range**, roughly `|a| < 1e-300`, for t-norms that multiply.
     *
     * This affects only t-norms that inherit this default: every named t-norm
     * overrides it, and no meaningful membership degree is subnormal. If it
     * matters to you, override with the closed form — which is the advice
     * anyway.
     */
    public fun residuum(a: Double, b: Double): Double {
        // Fast path and correctness guard in one: if T(a,1) ≤ b then every
        // z ∈ [0,1] satisfies the inequality by monotonicity, so the sup is 1.
        // For any t-norm with the boundary condition T(a,1) = a, this is a ≤ b.
        if (apply(a, 1.0) <= b) return 1.0

        var lo = 0.0
        var hi = 1.0
        var steps = 0
        while (steps < RESIDUUM_BISECTION_STEPS) {
            val mid = 0.5 * (lo + hi)
            // Adjacent doubles reached — no further refinement is representable.
            if (mid <= lo || mid >= hi) break
            if (apply(a, mid) <= b) lo = mid else hi = mid
            steps++
        }
        // `lo` is the greatest sampled z with T(a,z) ≤ b, i.e. the supremum.
        return lo
    }
}
