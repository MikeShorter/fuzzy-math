package dk.eusrbin.fuzzy.set

/**
 * A concrete refutation of convexity: two points, a weight, and the two degrees
 * that fail to satisfy Zadeh's eq. (25).
 *
 * **Source:** Zadeh 1965, §V eq. **(25)**, p.347:
 *
 * ```
 * f_A[λx₁ + (1 − λ)x₂]  ≥  Min[f_A(x₁), f_A(x₂)]     for all x₁, x₂ ∈ X, λ ∈ [0,1]
 * ```
 *
 * A witness is a triple `(x₁, x₂, λ)` where that fails — the membership at the
 * interpolated point dips *below* the lesser of the endpoints. Fig. 4 (p.347) is
 * a picture of one: the non-convex set on the right has a valley between two
 * peaks, and any `x₁`, `x₂` straddling the valley witness it.
 *
 * ## Why a triple, and why this type exists
 *
 * CLAUDE.md §19.1. Every other ∀ in this module witnesses with a single `x`, so
 * [Verdict]`<X>` could tie the witness type to the domain's element type. Convexity
 * cannot: it quantifies over **two** points *and* a weight, while the domain
 * searched is still `Domain<Double>`. Hence `Verdict<ConvexityWitness>`, and
 * hence `Verdict.of(witness, exhaustive)` taking a boolean rather than a domain.
 *
 * Both degrees are carried, not just the gap, because §7's ethic is that the
 * counterexample is the product: `atSegment` and `minEndpoints` are what let a
 * reader re-derive the failure by hand rather than trust the report.
 *
 * @property x1 the first point.
 * @property x2 the second point.
 * @property lambda the weight `λ ∈ [0,1]`; the interpolated point is [at].
 * @property atSegment `f_A[λx₁ + (1 − λ)x₂]` — the membership on the segment.
 * @property minEndpoints `Min[f_A(x₁), f_A(x₂)]` — what eq. (25) requires it to
 *   reach, and does not.
 */
public class ConvexityWitness(
    public val x1: Double,
    public val x2: Double,
    public val lambda: Double,
    public val atSegment: Double,
    public val minEndpoints: Double,
) {
    /**
     * The interpolated point at which eq. (25) fails.
     *
     * Computed as `x₂ + λ(x₁ − x₂)`, which is algebraically `λx₁ + (1 − λ)x₂` and
     * **is not the same in IEEE 754**. The search uses this form and so must this:
     * at `x₁ = x₂ = 3.0, λ = 0.2` the textbook form yields `3.0000000000000004`,
     * which would put [at] somewhere the search never looked and make the witness
     * fail to reproduce. It is also why a *convex* Gaussian was briefly refuted at
     * `x₁ = x₂` — see [DoubleMembershipFn.findNonConvexity].
     */
    public val at: Double
        get() = x2 + lambda * (x1 - x2)

    /** How far below eq. (25)'s requirement the segment dips. Always positive. */
    public val shortfall: Double
        get() = minEndpoints - atSegment

    override fun toString(): String =
        "ConvexityWitness(x₁=$x1, x₂=$x2, λ=$lambda → x=$at: " +
            "f_A(x) = $atSegment < Min[f_A(x₁), f_A(x₂)] = $minEndpoints, short by $shortfall)"
}
