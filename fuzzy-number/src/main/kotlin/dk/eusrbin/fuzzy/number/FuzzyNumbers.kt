package dk.eusrbin.fuzzy.number

/**
 * Fuzzy arithmetic, by **α-cut interval arithmetic**, exactly.
 *
 * **Attributed:** Dubois & Prade (1978) for the method; **Nguyen (1978)** for the
 * theorem that makes it legitimate — that for continuous monotone operations,
 * cut-wise interval arithmetic agrees with Zadeh's extension principle. **Neither
 * is on hand** (CLAUDE.md §17.5, §20.6), so neither is cited as though it could
 * arbitrate, and the claims below are asserted by `fuzzy-laws` rather than by a
 * reference. **Zadeh 1965 has no fuzzy arithmetic.**
 *
 * The nearest thing we *have* read is eq. **(23)** (§IV, p.346) — a fuzzy set
 * induced by a mapping, `f_B(y) = Max_{x ∈ T⁻¹(y)} f_A(x)`. That is the extension
 * principle for a *unary* map; its binary form, and Nguyen's equivalence, are
 * later work.
 *
 * ## Every operation here is exact — CLAUDE.md §20.3
 *
 * `(A ∗ B)_α = A_α ∗ B_α`, cut-wise, with no approximation anywhere. Where the
 * result stays in the input's family it is returned in that family; where it does
 * not, an [AlphaCutNumber] carries the exact cut map. **Nothing silently becomes a
 * triangle.**
 *
 * That matters most where every mainstream library gives up. `T(1,2,3) × T(1,2,3)`
 * has α-cuts `[(1+α)², (3−α)²]` — quadratic, not linear, so **not a triangle**.
 * The usual answer is `T(1,4,9)`, and note where it errs:
 *
 * ```
 * α      exact             T(1,4,9)          error
 * 0.0    [1.000, 9.000]    [1.000, 9.000]    0.000    ← the support: agrees
 * 0.5    [2.250, 6.250]    [2.500, 6.500]    0.250    ← the interior: wrong
 * 1.0    [4.000, 4.000]    [4.000, 4.000]    0.000    ← the peak: agrees
 * ```
 *
 * **Exact at both endpoints and wrong in between** — which is precisely why it
 * ships everywhere and nobody notices. §7 exists so that this library is not that.
 */
public object FuzzyNumbers {

    /**
     * `A + B`, exactly. `(A+B)_α = A_α + B_α`.
     *
     * **Closed on every family here**, and returned in-family — this is not an
     * optimisation, it is the exact answer wearing its own name:
     *
     * - **triangular** — `T(l₁+l₂, m₁+m₂, r₁+r₂)`. Verified cut-wise at α ∈ {0, ½, 1}.
     * - **trapezoidal** — `Tz(a₁+a₂, b₁+b₂, c₁+c₂, d₁+d₂)`. Likewise.
     * - **Gaussian** — `N(m₁+m₂, σ₁+σ₂)`. Verified.
     *
     * **The Gaussian case is worth a second look, and it is CLAUDE.md §0 in one
     * line.** Fuzzy spreads add **linearly**: `σ = σ₁ + σ₂`. Probabilistic ones add
     * **in quadrature**: `σ = √(σ₁² + σ₂²)`. For `σ₁ = 2, σ₂ = 4` that is `6`
     * against `4.47` — a 34% disagreement, and neither is a rounding error. §0:
     * *"the notion of a fuzzy set is completely non-statistical in nature."* If you
     * expected quadrature, you were doing probability.
     *
     * Mixed families fall through to the general form, which is equally exact.
     */
    @JvmStatic
    public fun add(a: FuzzyNumber, b: FuzzyNumber): FuzzyNumber = when {
        a is TriangularNumber && b is TriangularNumber ->
            TriangularNumber.of(a.l + b.l, a.m + b.m, a.r + b.r)
        a is TrapezoidalNumber && b is TrapezoidalNumber ->
            TrapezoidalNumber.of(a.a + b.a, a.b + b.b, a.c + b.c, a.d + b.d)
        a is GaussianNumber && b is GaussianNumber ->
            GaussianNumber.of(a.mean + b.mean, a.sigma + b.sigma)
        else -> AlphaCutNumber.of("($a + $b)") { alpha ->
            a.alphaCutInterval(alpha) + b.alphaCutInterval(alpha)
        }
    }

    /**
     * `A − B`, exactly. `(A−B)_α = A_α − B_α = [a₁−b₂, a₂−b₁]`.
     *
     * Closed on triangles — `T(l₁−r₂, m₁−m₂, r₁−l₂)`, note the **crossed feet** —
     * and on trapezoids and Gaussians (`N(m₁−m₂, σ₁+σ₂)`: the spreads still **add**,
     * because subtracting an uncertain quantity makes you *less* certain, not more).
     *
     * ## `X − X` is not zero, and CLAUDE.md §20.4 is about you
     *
     * The thing a user will report as a bug. For `X = T(1,2,3)`:
     *
     * ```
     * α = 0.0   X_α = [1.00, 3.00]   X_α − X_α = [−2.00, +2.00]
     * α = 0.5   X_α = [1.50, 2.50]   X_α − X_α = [−1.00, +1.00]
     * α = 1.0   X_α = [2.00, 2.00]   X_α − X_α = [ 0.00,  0.00]
     * ```
     *
     * `X − X = T(−2, 0, 2)` — a fuzzy number spread symmetrically about zero, width
     * `2(r−l)` at the support. **Only at `α = 1` is it `{0}`.**
     *
     * **α-cut arithmetic has no cancellation.** The two `X`s are treated as
     * independent quantities that merely happen to share a range — the same trap
     * ordinary interval arithmetic has, for the same reason. It is not a bug, and
     * it is not fixable within this method: `subtract(x, x)` cannot know its two
     * arguments are the same *quantity* rather than the same *range*.
     *
     * If you want cancellation you want symbolic algebra, not interval arithmetic.
     *
     * **Attributed/derived** — Zadeh 1965 does not cover it (§18.2's discipline).
     */
    @JvmStatic
    public fun subtract(a: FuzzyNumber, b: FuzzyNumber): FuzzyNumber = when {
        a is TriangularNumber && b is TriangularNumber ->
            TriangularNumber.of(a.l - b.r, a.m - b.m, a.r - b.l)
        a is TrapezoidalNumber && b is TrapezoidalNumber ->
            TrapezoidalNumber.of(a.a - b.d, a.b - b.c, a.c - b.b, a.d - b.a)
        a is GaussianNumber && b is GaussianNumber ->
            GaussianNumber.of(a.mean - b.mean, a.sigma + b.sigma)
        else -> AlphaCutNumber.of("($a − $b)") { alpha ->
            a.alphaCutInterval(alpha) - b.alphaCutInterval(alpha)
        }
    }

    /**
     * `A × B`, **exactly** — and the result is **not** a triangle. CLAUDE.md §20.3.
     *
     * `(A×B)_α = A_α × B_α`, the four-endpoint interval product. The result is a
     * genuine fuzzy number — its α-cuts are nested, since `A_α` and `B_α` are —
     * so it is exactly representable by its cut map, and [AlphaCutNumber] recovers
     * the membership function from it (§18.2's inversion of eq. (24)).
     *
     * **Triangular numbers are closed under `+` and `−` and NOT under `×`.** The
     * sides go quadratic. There is deliberately no in-family fast path here,
     * because there is no correct one.
     *
     * ## Why not return an approximating triangle, "loudly typed"?
     *
     * Rejected in §20.3, and the reason is worth restating: a type named
     * `ApproximateTriangle` invites exactly the reasoning §7 exists to prevent —
     * *"it is labelled, so it must be fine."* An approximation should be an
     * explicit, named, lossy projection **of** the exact answer, where the caller
     * states the loss — not what `×` hands you by default.
     *
     * ## The exact answer is sometimes writable, not merely computable
     *
     * For `T(1,2,3)²`, inverting both branches of `Γ_α = [(1+α)², (3−α)²]` gives
     *
     *     μ(y) = min(√y − 1, 3 − √y)     on [1, 9]
     *
     * which **round-trips the exact α-cut at all 101 sampled levels**, and gives
     * `μ(2.25) = 0.5` where the triangle gives `0.4167`. Symbolic inversion in
     * general is out of scope — but it settles that [AlphaCutNumber] is
     * *representing something real* rather than papering over a hole.
     *
     * **Cost:** each `applyAsDouble` on the result is a bisection over α (~50
     * evaluations of the cut map). That is the honest trade, and it is stated
     * rather than hidden.
     */
    @JvmStatic
    public fun multiply(a: FuzzyNumber, b: FuzzyNumber): FuzzyNumber =
        AlphaCutNumber.of("($a × $b)") { alpha ->
            a.alphaCutInterval(alpha) * b.alphaCutInterval(alpha)
        }

    /**
     * `A × k` for a crisp [factor] — closed on every family, and exact.
     *
     * Scaling by a real number is not multiplication by a fuzzy number: there is
     * only one quantity that is uncertain, so no spread compounds and no family is
     * left. A negative factor mirrors, which is why the triangular feet swap.
     */
    @JvmStatic
    public fun scale(a: FuzzyNumber, factor: Double): FuzzyNumber {
        require(factor.isFinite()) { "Scale factor must be finite, but was $factor" }
        return when {
            a is TriangularNumber && factor >= 0.0 ->
                TriangularNumber.of(a.l * factor, a.m * factor, a.r * factor)
            a is TriangularNumber ->
                TriangularNumber.of(a.r * factor, a.m * factor, a.l * factor)
            a is GaussianNumber ->
                GaussianNumber.of(a.mean * factor, a.sigma * kotlin.math.abs(factor))
            else -> AlphaCutNumber.of("($a × $factor)") { alpha ->
                a.alphaCutInterval(alpha).scale(factor)
            }
        }
    }

    /**
     * `−A`. Sugar for `scale(a, -1.0)`; mirrors about the origin.
     *
     * Note `add(a, negate(a))` is **not** crisp zero — it is `subtract(a, a)` by
     * another route, and §20.4 applies unchanged.
     */
    @JvmStatic
    public fun negate(a: FuzzyNumber): FuzzyNumber = scale(a, -1.0)
}
