package dk.eusrbin.fuzzy.laws

import dk.eusrbin.fuzzy.number.FuzzyNumber
import dk.eusrbin.fuzzy.set.Domain
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * **The fuzzy-number tier.** What `alphaCutInterval` must satisfy — and the suite
 * that turns CLAUDE.md §20.2(ii)'s *unchecked precondition* into a checked one.
 *
 * **Source:** Zadeh 1965, §II eq. **(24)** p.347 for the α-cut itself; §V p.348 for
 * boundedness. **Attributed** (§17.5, §20.6): the fuzzy-number *family* — LR,
 * triangular, α-cut arithmetic — is Dubois & Prade (1978) and Nguyen (1978),
 * **neither on hand**. So the laws below are stated from what we have read, and
 * nothing is cited as arbitrating that cannot.
 *
 * ## Why this suite exists — the precondition nobody can check
 *
 * `AlphaCutNumber` requires its cut map to be **nested** (`α₁ < α₂ ⟹ Γ_{α₂} ⊆
 * Γ_{α₁}`) and says, at length, that it cannot verify this at construction. §4's
 * *"construction validates"* governs **values**; a function's behaviour over
 * uncountably many α was never in its scope. The shipped precedent is
 * `TConorms.dualOf`, which requires an involutive negation and defers to
 * `DeMorganLaws` in the same words.
 *
 * This is where that deferral is honoured. And nestedness earns a law rather than
 * a shrug because it is **refutable in one direction** (§16.4): a witness
 * `(α₁ < α₂, Γ_{α₂} ⊄ Γ_{α₁})` disproves it *absolutely*, whatever the grid. That
 * makes it convexity-shaped, not §19.6-boundedness-shaped.
 *
 * ```java
 * @Test void myCutMapIsNested() {
 *     FuzzyNumberLaws.verify(AlphaCutNumber.of("mine", a -> myCuts(a)), Sampled.of(-5, 5));
 * }
 * ```
 *
 * ## What is deliberately NOT here
 *
 * **§20.8's carrier law.** `height` must read the domain rather than fold over it,
 * and the check for that lives in [MembershipFnLaws] — `override == fold` when
 * `over.isExhaustive` — because that is where it has teeth. A fuzzy number that
 * ignores its domain and returns its global height is caught there, over an
 * `Enumerable`, exactly.
 *
 * A *refinement sandwich* was considered for the `Sampled` case — `coarse fold ≤
 * height(over) ≤ a much finer fold` — and **rejected as unsound**: a triangle
 * peaking at `0.5` on a grid that steps over the peak folds to `0.9999`, so the
 * true answer `1.0` fails its own law. The gap is `O(step × slope)`, so the law
 * would need a *resolution* tolerance, and there is no Lipschitz constant here to
 * derive one from. §8 calibrates tolerances **per algebra, in one place**;
 * inventing a per-grid species to rescue a redundant law is how that decision gets
 * eroded. The `isExhaustive` law already catches the defect.
 */
public object FuzzyNumberLaws {

    private const val NESTED_CITATION =
        "Zadeh 1965, §V p.348 (Γ_α nested and bounded); CLAUDE.md §20.2(ii)"
    private const val CUT_CITATION =
        "Zadeh 1965, §II eq. (24) p.347; CLAUDE.md §20.1, §18.3 (two questions, two names)"
    private const val NORMAL_CITATION =
        "Zadeh 1965, §V p.349 (a fuzzy number is normal); CLAUDE.md §20.2"

    /**
     * Checks [n] over [over] and **throws** [LawViolationException] on failure.
     *
     * @param tolerance defaults to [Tolerance.GENERAL]. It is not decorative here:
     *   `GaussianNumber`'s cut endpoints round-trip through `√`, `ln` and `exp`, so
     *   `f(Γ_α.lower)` lands at `α ± ε` — `α = 0.9` gives `0.89999999999999991`,
     *   and **not systematically**: `α = 0.5` round-trips exactly. §20.2(iii).
     */
    @JvmStatic
    @JvmOverloads
    public fun verify(
        n: FuzzyNumber,
        over: Domain<Double>,
        tolerance: Tolerance = Tolerance.GENERAL,
    ) {
        check(n, over, tolerance).assertHolds()
    }

    /** Checks [n] over [over] and **returns** a [LawReport]. Never throws. */
    @JvmStatic
    @JvmOverloads
    public fun check(
        n: FuzzyNumber,
        over: Domain<Double>,
        tolerance: Tolerance = Tolerance.GENERAL,
    ): LawReport {
        val checker = LawChecker("FuzzyNumberLaws", "$n over $over", tolerance, Sampling.DEFAULT)
        val epsilon = tolerance.epsilon

        // (1) NESTEDNESS — the precondition AlphaCutNumber cannot check.
        //
        // α = 0 is excluded, not overlooked: Γ_0 is all of ℝ for every fuzzy set,
        // which is why alphaCutInterval's contract is (0,1] and why
        // requireCutLevel rejects it. A law that asked about Γ_0 would be asking
        // about a set the API declines to name.
        checker.law2("nested: α₁ < α₂ ⟹ Γ_{α₂} ⊆ Γ_{α₁}", NESTED_CITATION) { a1, a2 ->
            val lower = min(a1, a2)
            val upper = max(a1, a2)
            if (lower <= 0.0 || lower == upper) {
                null
            } else {
                val wide = n.alphaCutInterval(lower)
                val narrow = n.alphaCutInterval(upper)
                if (narrow.lower >= wide.lower - epsilon && narrow.upper <= wide.upper + epsilon) {
                    null
                } else {
                    "Γ_$upper = $narrow is not contained in Γ_$lower = $wide — " +
                        "the cuts are not nested, so applyAsDouble's bisection is searching " +
                        "a predicate that is not monotone in α and its answer means nothing"
                }
            }
        }

        // (2) THE TWO NAMES AGREE — §20.1, §18.3.
        //
        // `alphaCut(over, α)` asks *which grid points* have f ≥ α; `alphaCutInterval(α)`
        // asks *where f ≥ α is*. Two questions — which is exactly why they have two
        // names. But they are two questions about ONE set, so on the grid they must
        // give the same answer, and that is what makes the split honest rather than
        // an excuse.
        checker.law1("alphaCut and alphaCutInterval agree, point by point", CUT_CITATION) { alpha ->
            if (alpha <= 0.0) {
                null
            } else {
                val interval = n.alphaCutInterval(alpha)
                val fromGrid = n.alphaCut(over, alpha).toSet()
                val witness = over.elements().firstOrNull { x ->
                    // §20.2(iii) — skip the boundary band. A point where f(x) sits
                    // within ε of α IS the boundary, and there `x in interval` (a
                    // computed endpoint) and `f(x) >= alpha` (an exact comparison)
                    // may honestly disagree. §14.6(a): the exactness belongs to the
                    // comparison, not to the value.
                    //
                    // Same shape as ResiduumLaws skipping its induced-order band.
                    // The band is where the answer is genuinely ambiguous, not where
                    // it is inconvenient — outside it, this law is strict.
                    if (abs(n.applyAsDouble(x) - alpha) <= epsilon) {
                        false
                    } else {
                        (x in interval) != (x in fromGrid)
                    }
                }
                if (witness == null) {
                    null
                } else {
                    val inInterval = witness in interval
                    "x = $witness has f(x) = ${n.applyAsDouble(witness)} and α = $alpha, " +
                        "but it is ${if (inInterval) "inside" else "outside"} alphaCutInterval($alpha) = $interval " +
                        "and ${if (inInterval) "absent from" else "present in"} alphaCut(over, $alpha) — " +
                        "the two names disagree about one set, well outside the ±$epsilon boundary band"
                }
            }
        }

        // (3) NORMAL — f = 1 somewhere, and coreInterval says where.
        //
        // Not an idle restatement of `alphaCutInterval(1.0)`: it is the one law that
        // ties the cut map back to `f`. A cut map can be perfectly nested and
        // describe a different function entirely; this is where that shows.
        checker.law1("normal: f = 1 across the core interval", NORMAL_CITATION) { _ ->
            val core = n.coreInterval()
            checker.eq(n.applyAsDouble(core.lower), 1.0, "f(coreInterval().lower = ${core.lower})", "1")
                ?: checker.eq(n.applyAsDouble(core.upper), 1.0, "f(coreInterval().upper = ${core.upper})", "1")
                ?: checker.eq(
                    n.applyAsDouble(core.midpoint),
                    1.0,
                    "f(coreInterval().midpoint = ${core.midpoint})",
                    "1",
                )
        }

        // (4) The core is where the cuts converge. Γ_1 ⊆ Γ_α for every α, which (1)
        // already covers — but only for α values the pool happens to draw. This
        // states the endpoint directly, and it is the cheapest catch for a cut map
        // whose α = 1 case is special-cased wrongly.
        checker.law1("the core is contained in every cut", NESTED_CITATION) { alpha ->
            if (alpha <= 0.0) {
                null
            } else {
                val core = n.coreInterval()
                val cut = n.alphaCutInterval(alpha)
                if (core.lower >= cut.lower - epsilon && core.upper <= cut.upper + epsilon) {
                    null
                } else {
                    "the core $core is not contained in Γ_$alpha = $cut"
                }
            }
        }

        return checker.report()
    }
}
