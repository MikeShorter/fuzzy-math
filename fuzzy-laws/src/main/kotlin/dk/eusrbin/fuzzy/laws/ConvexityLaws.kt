package dk.eusrbin.fuzzy.laws

import dk.eusrbin.fuzzy.set.Domain
import dk.eusrbin.fuzzy.set.DoubleMembershipFn
import dk.eusrbin.fuzzy.set.Sampled
import dk.eusrbin.fuzzy.set.Verdict

/**
 * **The convexity tier** — Zadeh §V, ℝ¹-bound.
 *
 * **Source:** Zadeh 1965, §V eq. **(25)**, p.347 (convexity) and p.349 (strong
 * convexity). See [DoubleMembershipFn.findNonConvexity].
 *
 * ## This suite is deliberately small, and CLAUDE.md §19.7 is why
 *
 * §V has four theorems. **Three of them cannot be shipped as laws**, and the
 * reason is worth understanding before reaching for this:
 *
 * Every §V theorem is **conditional on convexity**, and over a [Sampled] grid
 * convexity is never [Verdict.Proven] — only [Verdict.NotRefuted] (§19.1). The
 * antecedent is never established, so a failure of the consequent indicts
 * nothing. Take p.347's *"if A and B are convex, so is their intersection"*: if
 * the grid refutes `A ∩ B`, the contrapositive says `A` or `B` is genuinely
 * non-convex and the grid missed it. That is a **sampling gap, not a bug** —
 * and a suite reporting it as a violation would be blaming the code for the
 * sampler.
 *
 * p.350's corollary is worse: it *fails* on a grid for genuinely strongly convex
 * sets. A triangle peaked between two grid points has two points tied for the
 * maximal grade, so its `maximalGradeSet` has two elements and "the point is
 * unique" reads false — while the set is impeccably strongly convex. The culprit
 * is *"essentially attained"*, the topological adverb §18.3 had to drop.
 *
 * p.352's separation theorem is `separationDegree`'s own implementation (§19.3).
 * Checking it would be circular.
 *
 * **So those four are tested against fixtures whose convexity is known by
 * construction** — `fuzzy-laws`' own tests — rather than shipped as laws about a
 * set a stranger sampled. §7's line: a suite must not report a sampling gap as a
 * violation.
 *
 * What remains is sound, and is what a consumer overriding §15.3's hooks
 * actually needs.
 */
public object ConvexityLaws {

    private const val CONVEXITY = "Zadeh 1965, §V eq. (25), p.347"
    private const val STRONG = "Zadeh 1965, §V p.349 (strong convexity)"
    private const val OVERRIDE = "CLAUDE.md §15.3 (closed forms are overrides); §7"

    /**
     * Checks [fn] over [over] and **throws** [LawViolationException] on failure.
     *
     * @param tolerance reported, not used — every law here is a comparison of
     *   [Verdict]s or a re-evaluation of `f` at a point, and neither rounds.
     *   [Tolerance.EXACT] says so.
     */
    @JvmStatic
    @JvmOverloads
    public fun verify(
        fn: DoubleMembershipFn,
        over: Domain<Double>,
        tolerance: Tolerance = Tolerance.EXACT,
    ) {
        check(fn, over, tolerance).assertHolds()
    }

    /** Checks [fn] over [over] and **returns** a [LawReport]. Never throws. */
    @JvmStatic
    @JvmOverloads
    public fun check(
        fn: DoubleMembershipFn,
        over: Domain<Double>,
        tolerance: Tolerance = Tolerance.EXACT,
    ): LawReport {
        val checker = LawChecker("ConvexityLaws", "$fn over $over", tolerance, Sampling.DEFAULT)

        val convexity = fn.findNonConvexity(over)
        val strong = fn.findNonStrongConvexity(over)

        // (1) strong ⟹ convex. Sound because both searches see the same grid and
        // the same weights:
        //   - a convexity refutation has f[at] < Min[…], which is also a
        //     strong-convexity refutation (`<` is not `>`);
        //   - x₁ = x₂ cannot refute convexity, since there at = x₁ exactly and
        //     f(x₁) ≥ f(x₁). So every convexity witness has distinct endpoints,
        //     which is what strong convexity restricts to.
        // So Refuted here FORCES Refuted there — the contrapositive of Zadeh's own
        // p.349 relationship between the two definitions.
        //
        // That second bullet is only true because findNonConvexity interpolates as
        // `x₂ + λ(x₁ − x₂)`. With the textbook `λx₁ + (1 − λ)x₂` it is FALSE: at
        // x₁ = x₂ = 3.0, λ = 0.2 that form gives 3.0000000000000004, f(at) < f(x₁),
        // and convexity is refuted where strong convexity — which skips x₁ = x₂ —
        // is not. This law caught exactly that, on a Gaussian, which is convex.
        // The claim is a theorem about ℝ; the law is only sound because the code
        // was made to agree with ℝ at that one point.
        checker.law1("strong convexity ⟹ convexity", STRONG) { _ ->
            if (convexity is Verdict.Refuted && strong !is Verdict.Refuted) {
                "findNonConvexity found a witness (${convexity.witness}) but " +
                    "findNonStrongConvexity did not — impossible, since `<` refutes `>` too"
            } else {
                null
            }
        }

        // (2) The §15.3 override guard. Sound in ONE direction only: a witness is
        // absolute (§16.4), so an override claiming convexity where the generic
        // search found one is wrong. The converse is not a violation — an override
        // that refutes where the grid did not may simply know more.
        val generic = DoubleMembershipFn { x -> fn.applyAsDouble(x) }
        val genericConvexity = generic.findNonConvexity(over)
        checker.law1("override: findNonConvexity may not deny a witness the fold found", OVERRIDE) { _ ->
            if (genericConvexity is Verdict.Refuted && convexity !is Verdict.Refuted) {
                "the generic search found a witness (${genericConvexity.witness}) " +
                    "but this function's findNonConvexity returned $convexity — a witness is absolute"
            } else {
                null
            }
        }

        checker.law1("override: findNonStrongConvexity may not deny a witness the fold found", OVERRIDE) { _ ->
            val genericStrong = generic.findNonStrongConvexity(over)
            if (genericStrong is Verdict.Refuted && strong !is Verdict.Refuted) {
                "the generic search found a witness (${genericStrong.witness}) " +
                    "but this function's findNonStrongConvexity returned $strong"
            } else {
                null
            }
        }

        // (3) A reported witness must actually witness. Self-consistency of the
        // report — §7's ethic is that the counterexample is the product, so a
        // counterexample that does not reproduce is worse than none.
        checker.law1("a convexity witness reproduces", CONVEXITY) { _ ->
            val witness = (convexity as? Verdict.Refuted)?.witness ?: return@law1 null
            when {
                witness.atSegment >= witness.minEndpoints ->
                    "reported witness does not witness: atSegment = ${witness.atSegment} " +
                        "is not below minEndpoints = ${witness.minEndpoints}"
                fn.applyAsDouble(witness.at) != witness.atSegment ->
                    "witness does not reproduce: f_A(${witness.at}) = ${fn.applyAsDouble(witness.at)} " +
                        "but the witness reports atSegment = ${witness.atSegment}"
                witness.shortfall <= 0.0 ->
                    "witness reports a non-positive shortfall (${witness.shortfall})"
                else -> null
            }
        }

        // (4) Over a Sampled domain, convexity must NEVER come back Proven — two
        // uncountable quantifiers, and the grid saw neither exhaustively. This is
        // §15.6's whole point ("returning true asserts a proof we did not
        // perform"), asserted rather than trusted.
        checker.law1("a sampled ∀ is never Proven", "CLAUDE.md §15.6, §19.1") { _ ->
            if (!over.isExhaustive && convexity is Verdict.Proven) {
                "findNonConvexity returned Proven over a non-exhaustive domain ($over) — " +
                    "a grid cannot prove a ∀ over two uncountable quantifiers"
            } else {
                null
            }
        }

        return checker.report()
    }
}
