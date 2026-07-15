package dk.eusrbin.fuzzy.set

import dk.eusrbin.fuzzy.algebra.Degrees
import kotlin.math.min

/**
 * A membership function on the reals — `f_A : ℝ → [0,1]`, with **no boxing at
 * all**.
 *
 * Source: Zadeh 1965, §II. Zadeh's own examples are overwhelmingly on ℝ (§V's
 * Figures 1, 4 and 5; the "class of real numbers much greater than 1", p.339),
 * which is why §9 singles this case out:
 *
 * > *"Provide a `DoubleMembershipFn` specialisation for `X = ℝ` (Zadeh's common
 * > case) to avoid boxing the argument too."*
 *
 * ## Why a second type rather than `MembershipFn<Double>`
 *
 * `MembershipFn<Double>` boxes its argument: `X` is a generic parameter, erased
 * to `Object`, so every call allocates a `java.lang.Double`. On the path CLAUDE.md
 * §4 cares about — a supremum over a [Sampled] grid, "millions of calls" — that
 * is millions of allocations to hand the JIT.
 *
 * `applyAsDouble` takes a primitive because it is **not** an override of a generic
 * method: its signature is `(D)D` outright. That is the guarantee, and it is why
 * the primitive entry point carries its own name rather than reusing [apply].
 * The name follows `java.util.function.DoubleUnaryOperator.applyAsDouble`, so it
 * reads as idiom rather than invention from Java.
 *
 * ## And yet it *is* a `MembershipFn<Double>`
 *
 * Deliberately a subtype, unlike the JDK's `DoubleUnaryOperator` (which is not a
 * `Function<Double,Double>`, because Java cannot express the bridge). Kotlin can,
 * so every domain-generic operation — [Domain], height, α-cuts, containment —
 * accepts a `DoubleMembershipFn` with no adapter. There is one API, not two.
 *
 * The cost is that reaching it *through* `MembershipFn<Double>` boxes again, via
 * the inherited [apply]. That is unavoidable and correct: the boxing is the price
 * of the generic call, not of this type. Code on the hot path takes the primitive
 * road deliberately — [Sampled] checks for this type once, outside its loop,
 * rather than per element.
 *
 * ```java
 * DoubleMembershipFn nearZero = x -> Math.exp(-x * x);
 * nearZero.applyAsDouble(0.5);              // primitive: no allocation
 * nearZero.height(Sampled.of(-3, 3, 1024)); // MembershipFn<Double>: works
 * ```
 * ```clojure
 * (reify DoubleMembershipFn (applyAsDouble [_ x] (Math/exp (- (* x x)))))
 * ```
 *
 * ## Preconditions
 *
 * Must return a value in `[0,1]`; unchecked, per [MembershipFn].
 */
public fun interface DoubleMembershipFn : MembershipFn<Double> {

    /**
     * The grade of membership of [x] — `f_A(x)` — taking and returning a
     * primitive `double`.
     *
     * The single abstract method: a lambda you write implements *this*, and
     * [apply] is supplied for you.
     */
    public fun applyAsDouble(x: Double): Double

    /**
     * The boxed [MembershipFn] contract, satisfied by delegating to
     * [applyAsDouble].
     *
     * Present so that a `DoubleMembershipFn` is usable wherever a
     * `MembershipFn<Double>` is wanted. Prefer [applyAsDouble] when the static
     * type is known — this one boxes, that one does not.
     */
    override fun apply(x: Double): Double = applyAsDouble(x)

    /** Kotlin sugar for [applyAsDouble], letting you write `A(x)` with no boxing. */
    override operator fun invoke(x: Double): Double = applyAsDouble(x)

    // ---- Zadeh §V: the parts that need a vector space ----------------------
    //
    // CLAUDE.md §15.5 is the finding that put these here rather than on
    // MembershipFn. Convexity is
    //
    //     f_A[λx₁ + (1 − λ)x₂] ≥ Min[f_A(x₁), f_A(x₂)]            (25)
    //
    // and `λx₁ + (1 − λ)x₂` needs **scalar multiplication and addition on X**. A
    // Domain<X> can search X; it cannot form the line segment between two of its
    // points. Zadeh states the precondition himself (p.347): "we assume for
    // concreteness that X is a real Euclidean space Eⁿ".
    //
    // §15.5 decides ℝ¹ only, and a VectorSpace<X> capability is an explicit
    // non-goal. `Double` is the one X in this library with the arithmetic, so
    // these live here — on the specialisation — and not one level up. That is
    // the whole reason this type is more than a boxing optimisation.
    //
    // §19.5 and §19.6 record what did NOT come with them: strict convexity is
    // vacuous in ℝ¹ (every convex subset of ℝ is strictly convex), and
    // boundedness is unsamplable from a bounded window in either direction.

    /**
     * Searches for a refutation of **convexity** — eq. (25).
     *
     * **Source:** Zadeh 1965, §V eq. **(25)**, p.347:
     *
     * ```
     * f_A[λx₁ + (1 − λ)x₂]  ≥  Min[f_A(x₁), f_A(x₂)]     ∀ x₁, x₂ ∈ X, ∀ λ ∈ [0,1]
     * ```
     *
     * Zadeh gives two equivalent definitions and proves them equal (p.347). This
     * uses the second — *"an alternative and more direct definition"*, which he
     * credits to E. Berlekamp in n.5 — because it is the one a sampler can
     * attack: eq. (24)'s "every `Γ_α` is convex" quantifies over `α` as well.
     *
     * Note his warning: *"this definition does **not** imply that `f_A(x)` must be
     * a convex function of x."* A convex fuzzy set is generally a *concave*
     * function — the triangle `1 − |x − 1|` is convex as a fuzzy set. If that
     * reads backwards, it is because "convex" here qualifies the **set**, via its
     * α-cuts, not the graph.
     *
     * ## Why a [Verdict] and not a boolean
     *
     * CLAUDE.md §15.6: convexity quantifies over uncountably many `x` **and** `λ`,
     * so *"returning `true` asserts a proof we did not perform."* §19.1 then
     * supersedes §15.6's `Counterexample?` with [Verdict], because a nullable
     * witness conflates `Proven` with `NotRefuted` — the same argument §16.4
     * already won for containment.
     *
     * Over a [Sampled] domain this returns [Verdict.Refuted] or
     * [Verdict.NotRefuted], **never [Verdict.Proven]** — a grid cannot prove a ∀
     * over two uncountable quantifiers. That is not a defect of the report; it is
     * the truth about what was done. A witness, though, refutes **absolutely**:
     * sampling is lossy in one direction only.
     *
     * ## What is sampled, and what is not
     *
     * `x₁` and `x₂` come from [over]'s grid and `λ` from [weights], but
     * `f_A[λx₁ + (1 − λ)x₂]` is evaluated **off-grid** — the interpolated point is
     * a real number, and `f_A` is a function, so no interpolation or lookup is
     * involved. The domain supplies candidates; it does not constrain where the
     * function is asked.
     *
     * @param over supplies the candidate endpoints. `n²` pairs.
     * @param weights the `λ` values to try. The single-argument overload supplies
     *   nine interior weights — `λ ∈ {0,1}` is excluded because it makes eq. (25)
     *   trivially `f_A(xᵢ) ≥ Min[…]`, true for free and a wasted pass.
     */
    public fun findNonConvexity(
        over: Domain<Double>,
        weights: DoubleArray,
    ): Verdict<ConvexityWitness> = searchForConvexityFailure(over, weights, strict = false)

    /**
     * [findNonConvexity] at the default weights.
     *
     * Written out rather than a defaulted parameter with `@JvmOverloads`, which
     * Kotlin rejects on interface methods. This is the overload that annotation
     * would have generated, and it delegates *virtually* — so an implementation
     * overriding only the two-argument form is still reached through this one.
     */
    public fun findNonConvexity(over: Domain<Double>): Verdict<ConvexityWitness> =
        findNonConvexity(over, DEFAULT_WEIGHTS)

    /**
     * Searches for a refutation of **strong convexity** — p.349.
     *
     * **Source:** Zadeh 1965, §V p.349 — *"A fuzzy set A is **strongly convex**
     * if, for any two **distinct** points `x₁` and `x₂`, and any `λ` in the
     * **open** interval `(0,1)`, `f_A[λx₁ + (1 − λ)x₂] > Min[f_A(x₁), f_A(x₂)]`."*
     * **No equation number.**
     *
     * Eq. (25) with `>` for `≥`, and with the endpoints and `λ ∈ {0,1}` excluded —
     * both exclusions are Zadeh's, and both are necessary: at `x₁ = x₂` or
     * `λ ∈ {0,1}` the strict inequality is unsatisfiable, so including them would
     * refute every set alive.
     *
     * Zadeh's caveat, worth repeating because it is counter-intuitive: *"strong
     * convexity does not imply strict convexity or vice-versa."* They are
     * independent, not a tier — which is part of why §19.5 could drop strict
     * convexity without disturbing this.
     *
     * Strong convexity is what p.350's corollary needs: *"If `X = E¹` and A is
     * strongly convex, then the point at which M is essentially attained is
     * unique."* — the one theorem in §V stated for `E¹` specifically, and so the
     * one this slice can actually check.
     */
    public fun findNonStrongConvexity(
        over: Domain<Double>,
        weights: DoubleArray,
    ): Verdict<ConvexityWitness> = searchForConvexityFailure(over, weights, strict = true)

    /** [findNonStrongConvexity] at the default weights. See [findNonConvexity] on why this is spelled out. */
    public fun findNonStrongConvexity(over: Domain<Double>): Verdict<ConvexityWitness> =
        findNonStrongConvexity(over, DEFAULT_WEIGHTS)

    /**
     * `D = 1 − M` where `M = Sup_x Min[f_A(x), f_B(x)]` — the **degree of
     * separation** of this set and [other].
     *
     * **Source:** Zadeh 1965, §V p.352 — **the THEOREM, not the definition.**
     * That distinction is the whole KDoc (CLAUDE.md §19.3), so read on before
     * using it.
     *
     * > *"**Theorem.** Let A and B be **bounded convex** fuzzy sets in Eⁿ, with
     * > maximal grades `M_A` and `M_B` … Let M be the maximal grade for the
     * > intersection `A ∩ B` (`M = Sup_x Min[f_A(x), f_B(x)]`). Then `D = 1 − M`."*
     *
     * ## This is not the definition of D, and the difference matters
     *
     * Zadeh **defines** the degree of separability at eqs. **(31)** and **(32)**,
     * p.351: `D = 1 − M̃` where `M̃ = Inf_H M_H` — an infimum over **every
     * hypersurface H in Eⁿ**. That is not computable, and not approximable by
     * sampling: there is no grid over the space of hypersurfaces.
     *
     * What is computable is the theorem's right-hand side, and it equals `D`
     * **only when both sets are bounded and convex**. Hand this a non-convex set
     * and it returns a number that is arithmetic, well-defined, and **not their
     * degree of separation**. Nothing will complain — which is exactly the class
     * of silent-wrong-answer §7 exists for.
     *
     * ## The preconditions, and how to check them
     *
     * - **Convex** — [findNonConvexity] on both. A [Verdict.Refuted] means this
     *   number is meaningless; [Verdict.NotRefuted] means nobody found a problem
     *   on that grid, which is the most a sampler can offer.
     * - **Bounded** — **this library cannot check it** (CLAUDE.md §19.6). A
     *   [Sampled] window is bounded, so every `Γ_α` it can see is bounded, and a
     *   witness to unboundedness would have to live outside the window. Neither
     *   provable nor refutable by sampling; you must know it.
     *
     * Unchecked at call time, per CLAUDE.md §4's split: construction validates,
     * operations do not — and the caller may well already know their sets are
     * convex, having built them that way.
     *
     * ## The intuition, in Zadeh's words
     *
     * > *"In plain words, the theorem states that the highest degree of separation
     * > of two convex fuzzy sets A and B that can be achieved with a hyperplane in
     * > Eⁿ is one minus the maximal grade in the intersection `A ∩ B`."*
     *
     * Fig. 5 (p.352) is the `n = 1` picture. Disjoint-ish sets barely overlap, `M`
     * is small, and `D` is near 1.
     */
    public fun separationDegree(other: MembershipFn<Double>, over: Domain<Double>): Double {
        // M = Sup_x Min[f_A, f_B] — the maximal grade of the intersection.
        // Min is the Gödel t-norm; FuzzySets.intersection defaults to
        // Algebra.STANDARD, whose tNorm IS Min (§6). No min() written here.
        val intersection = FuzzySets.intersection(this, other)
        return 1.0 - intersection.height(over)
    }

    /** Shared by [findNonConvexity] and [findNonStrongConvexity]; `strict` selects `>` over `≥`. */
    private fun searchForConvexityFailure(
        over: Domain<Double>,
        weights: DoubleArray,
        strict: Boolean,
    ): Verdict<ConvexityWitness> {
        for (weight in weights) Degrees.requireDegree(weight, "weight")
        val points = over.elements()
        for (x1 in points) {
            for (x2 in points) {
                // Strong convexity is stated for DISTINCT points only (p.349);
                // at x₁ = x₂ the strict inequality is unsatisfiable, so including
                // it would refute every set alive.
                if (strict && x1 == x2) continue
                // Arithmetic min, NOT the Gödel t-norm — and the distinction is
                // real, not pedantry. Eq. (25)'s Min compares ONE set's degrees at
                // TWO points; there is no second set, so there is nothing to
                // intersect. Contrast separationDegree above, where "the maximal
                // grade for the intersection A ∩ B" genuinely IS an intersection
                // and goes through the algebra (§6). Routing this through
                // TNorms.GODEL would typecheck and would mean something else.
                val minEndpoints = min(applyAsDouble(x1), applyAsDouble(x2))
                for (weight in weights) {
                    // `x₂ + λ(x₁ − x₂)`, NOT `λx₁ + (1 − λ)x₂`. Algebraically
                    // identical; in IEEE 754 they are not, and the difference is
                    // a FALSE REFUTATION.
                    //
                    // At x₁ = x₂ = 3.0, λ = 0.2 the textbook form gives
                    // 0.2·3.0 + 0.8·3.0 = 3.0000000000000004 — not 3.0. So `at`
                    // drifts off the point, f(at) < f(x₁) for any decreasing f,
                    // and eq. (25) "fails" at x₁ = x₂ where it is trivially true.
                    // The Gaussian, which is convex, was refuted this way.
                    //
                    // This form is exact whenever x₁ = x₂ (the delta is exactly
                    // zero) and at λ = 0 (returns x₂ untouched). ConvexityWitness.at
                    // computes it the same way — they must agree, or the witness
                    // does not reproduce.
                    val at = x2 + weight * (x1 - x2)
                    val atSegment = applyAsDouble(at)
                    val holds = if (strict) atSegment > minEndpoints else atSegment >= minEndpoints
                    if (!holds) {
                        return Verdict.Refuted(
                            ConvexityWitness(x1, x2, weight, atSegment, minEndpoints),
                        )
                    }
                }
            }
        }
        // NEVER Proven from a Sampled grid: two uncountable quantifiers, and the
        // grid saw neither exhaustively. `over.isExhaustive` is false for every
        // Sampled, so this resolves to NotRefuted — correctly.
        return Verdict.noWitness(over.isExhaustive)
    }
}

/**
 * The `λ` values [DoubleMembershipFn.findNonConvexity] tries by default.
 *
 * Nine interior weights. `λ ∈ {0, 1}` is deliberately absent: eq. (25) at `λ = 1`
 * reads `f_A(x₁) ≥ Min[f_A(x₁), f_A(x₂)]`, which is true for free — a wasted pass
 * for `findNonConvexity`, and an *unsatisfiable* one for
 * [DoubleMembershipFn.findNonStrongConvexity], whose `>` Zadeh explicitly
 * restricts to the **open** interval `(0,1)` for exactly this reason.
 *
 * The midpoint `0.5` is included and is the one that matters most: Zadeh's own
 * strict-convexity definition (p.349) is stated in terms of midpoints, and a
 * valley between two peaks — Fig. 4's non-convex set — is deepest there.
 */
private val DEFAULT_WEIGHTS: DoubleArray =
    doubleArrayOf(0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9)
