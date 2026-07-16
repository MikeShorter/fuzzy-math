package dk.eusrbin.fuzzy.number

import dk.eusrbin.fuzzy.algebra.Degrees
import dk.eusrbin.fuzzy.set.ConvexityWitness
import dk.eusrbin.fuzzy.set.Domain
import dk.eusrbin.fuzzy.set.DoubleMembershipFn
import dk.eusrbin.fuzzy.set.Enumerable
import dk.eusrbin.fuzzy.set.Sampled
import dk.eusrbin.fuzzy.set.Verdict

/**
 * A **fuzzy number**: a fuzzy set on ℝ whose α-cuts are closed bounded intervals.
 *
 * **Attributed:** the fuzzy-number concept and the LR representation are Dubois &
 * Prade (1978); the theorem that α-cut arithmetic agrees with the extension
 * principle for continuous monotone operations is Nguyen (1978). **Neither is on
 * hand** (CLAUDE.md §17.5, §20.6), so neither is cited as though it could
 * arbitrate. **Zadeh 1965 has no fuzzy arithmetic at all.**
 *
 * **`Source:`** for what does reduce to a text we have read: `Γ_α = {x | f_A(x) ≥
 * α}` is Zadeh eq. **(24)**, §V p.347, and his restriction to `α ∈ (0,1]` is his
 * own — see [alphaCutInterval]. Convexity is eq. **(25)**. `f(x) = sup{α | x ∈
 * Γ_α}` derives from (24) by two substitutions (§18.2).
 *
 * ## The definition is the design
 *
 * *Normal, convex, with bounded `Γ_α` for every `α ∈ (0,1]`* — which is exactly
 * "the α-cuts are closed bounded intervals". That is not a convenience; it is
 * **why α-cut arithmetic works at all**, and it is why [alphaCutInterval] is the
 * one abstract member: everything else follows from it.
 *
 * ```java
 * FuzzyNumber about2 = TriangularNumber.of(1.0, 2.0, 3.0);
 * about2.alphaCutInterval(0.5);   // [1.5, 2.5] — exact, no Domain needed
 * about2.applyAsDouble(1.5);      // 0.5        — primitive, no boxing (§9)
 * ```
 *
 * ## What this type may and may not override — CLAUDE.md §20.8
 *
 * The rule that governs every member below, and the one §15.3 needed and lacked:
 *
 * > **Is the universe fixed by the operation, or supplied by the caller?**
 *
 * - **Fixed** — [findNonConvexity], [findNonStrongConvexity]. Convexity is a
 *   property of `f` on **ℝ**; the `Domain` only proposes candidate endpoints. A
 *   closed-form proof answers the same question, so these override and **may
 *   ignore the domain**. [findNonConvexity] returning [Verdict.Proven] is honest
 *   here and nowhere else (§20.5).
 * - **Supplied** — [height], [isNormal]. *"Sup over **what**?"* is answered by the
 *   argument. `Sampled(-1, 2)` and `Sampled(2, 3)` are **different questions**, and
 *   `TriangularNumber(-0.5, 0.5, 1.5).height(Sampled(2, 3))` is `0.0` — the
 *   triangle is identically zero there. So these override by **reading the
 *   carrier, not by ignoring it**.
 * - **Two questions** — `support`, `core`, `maximalGradeSet`, `alphaCut`,
 *   `sigmaCount`. Not overridden at all (§20.1(b)); each gets a second name.
 *   [alphaCutInterval] and [coreInterval] are those names.
 *
 * §15.3's worked example — *"overrides `height` … and **ignores the domain
 * entirely**"* — is superseded by §20.8 for exactly this reason. It inherited that
 * phrase from §3's `Parametric`, the concept §15.3 existed to delete.
 */
public interface FuzzyNumber : DoubleMembershipFn {

    /**
     * `Γ_α = {x | f(x) ≥ α}` — **exact**, and needing no [Domain].
     *
     * **Source:** Zadeh 1965, §V eq. **(24)**, p.347.
     *
     * The one abstract member, and the type's whole content: everything else here
     * derives from it. It is [MembershipFn.alphaCut]'s **second name**, not its
     * replacement — that one asks *"which grid points are in `Γ_α`"* and this one
     * asks *"what **is** `Γ_α`"*. Two questions (§18.3, §20.1(b)).
     *
     * ## `α ∈ (0,1]`, and Zadeh's restriction is not fastidiousness
     *
     * `Γ_0 = {x | f(x) ≥ 0}` is **all of ℝ**, for every fuzzy set, since every
     * degree is `≥ 0`. It carries no information and is not an interval anyone
     * wants. Zadeh restricts α to `(0,1]` throughout §V for this reason, and so
     * does this method.
     *
     * The **support** — `{x | f(x) > 0}`, the *strong* 0-cut — is a different set
     * and may be unbounded (a Gaussian's is ℝ). It is not `Γ_0` and is not
     * reachable from here.
     *
     * @throws IllegalArgumentException if [alpha] is not in `(0, 1]`.
     */
    public fun alphaCutInterval(alpha: Double): Interval

    /**
     * `Γ_1` — the interval on which this number attains full membership.
     *
     * The **second name** for `MembershipFn.core`/`maximalGradeSet` (§18.3,
     * §20.1(b)). `core(over)` asks which *grid points* have `f = 1`; this asks
     * where `f = 1` actually is. For a [TriangularNumber] that is the degenerate
     * `[m, m]`; for a trapezoid it is a genuine interval — which is precisely why
     * `List<Double>` could not have carried it and a second name was needed.
     *
     * Non-empty by construction: a fuzzy number is normal.
     */
    public fun coreInterval(): Interval = alphaCutInterval(1.0)

    // ---- §20.8, group "fixed": the universe is ℝ, so ignore the domain -----

    /**
     * **`Verdict.Proven`** — a fuzzy number is convex **by definition**.
     *
     * **Source:** Zadeh 1965, §V eq. **(24)** and **(25)**, p.347. Convexity *is* "every
     * `Γ_α` is convex", and in ℝ a convex set is an interval — which is what
     * [alphaCutInterval] returns by construction. No search is performed because
     * none is needed.
     *
     * ## This is CLAUDE.md §20.5's dividend, and it needed §20.8 to be honest
     *
     * §19.1 gave [Verdict] three values so an **exhaustive enumeration** could say
     * `Proven` where a grid could only say `NotRefuted`. §15.3 gave functions the
     * right to override analysis with closed forms. Neither knew about the other,
     * and they compose: **`Proven` means "a proof exists"**, not "the domain was
     * exhaustive". Enumeration was only ever *one way* to have one.
     *
     * So this returns `Proven` over a [Sampled] domain — the case §15.6 exists to
     * forbid *guessing* about — and it is not a guess.
     *
     * **§20.8 is why this override may ignore [over] while [height] may not.**
     * Convexity's universe is **ℝ, fixed by the operation**; `over` only proposes
     * candidate `x₁, x₂`. `height`'s universe is *supplied*. Same interface, two
     * kinds of member.
     *
     * `ConvexityLaws` cross-checks this the only way a suite honestly can (§20.5):
     * **`Proven` ⟹ the generic sampled search finds no witness.** It never asks
     * how the claim was reached — a witness is absolute (§16.4), so a lie is
     * caught, and that is enough.
     */
    override fun findNonConvexity(
        over: Domain<Double>,
        weights: DoubleArray,
    ): Verdict<ConvexityWitness> = Verdict.Proven()

    /**
     * Whether this number is **strongly** convex — and by default it is **not**.
     *
     * **Source:** Zadeh 1965, §V p.349. Strong convexity needs
     * `f[λx₁ + (1−λ)x₂] > Min[f(x₁), f(x₂)]` — *strictly* — for **any** two
     * distinct points in ℝ. A fuzzy number with bounded support has `f = 0` on
     * both tails, so any two points out there give `0 > 0`, which is false.
     *
     * The default therefore falls back to the **generic search**, which will find
     * that witness. It is not overridden with `Refuted` and a hand-built witness,
     * because an implementation *may* be strongly convex — a Gaussian is, having no
     * flat tail — and this interface cannot know which it has.
     *
     * §20.8: this member's universe is ℝ and fixed, so an override *may* ignore
     * the domain. `GaussianNumber` does exactly that.
     */
    // (no override — the generic search from DoubleMembershipFn is correct here)

    // ---- §20.8, group "supplied": read the carrier, do not fold over it ----

    /**
     * `Sup_{x ∈ over} f(x)` — **read off the carrier**, not folded.
     *
     * **CLAUDE.md §20.8 is this method.** §15.3 said a `TriangularNumber` overrides
     * `height` *"with the analytic answer and ignores the domain entirely"*. That
     * is wrong, and the counterexample is flat:
     *
     * ```
     * TriangularNumber(-0.5, 0.5, 1.5).height(Sampled(2, 3))
     *   the triangle is identically ZERO on [2,3]
     *   correct answer: 0.0        "ignore the domain": 1.0
     * ```
     *
     * *"Sup over **what**?"* is answered by the argument, so the argument must be
     * read. [Domain] is **sealed** (§15.4), which is what makes that possible:
     *
     * - **[Sampled]** — the analytic Sup over the window `[lo, hi]`, in **O(1)**,
     *   via [supremumOver]. Exact, and **better than the fold**: for
     *   `TriangularNumber(-0.5, 0.5, 1.5)` over `Sampled(-1, 2, 1024)` this returns
     *   `1.0` where the fold returns `0.998534`, because the peak is not a grid
     *   point.
     * - **[Enumerable]** — **fall back to the fold.** There is no analytic
     *   shortcut: the universe *is* the element set, and folding it is not an
     *   approximation of anything. §15.3's promise simply does not apply.
     * - **[dk.eusrbin.fuzzy.set.Product]** — likewise the fold; a product of
     *   domains is not a window on ℝ.
     *
     * `MembershipFnLaws` checks `override ≥ fold` always, and `override == fold`
     * when `over.isExhaustive` (§20.7) — the tightening being the half with teeth.
     */
    override fun height(over: Domain<Double>): Double = when (over) {
        is Sampled -> supremumOver(over.lower, over.upper)
        // No shortcut exists, and pretending otherwise is exactly §20.8's bug.
        // Reaches MembershipFn's generic fold through the one direct supertype.
        else -> super<DoubleMembershipFn>.height(over)
    }

    /**
     * `Sup_{x ∈ [lo, hi]} f(x)` — the analytic supremum over a **window of ℝ**.
     *
     * The hook [height] dispatches to for a [Sampled] domain, and the one place an
     * implementation states what it knows about its own shape. The default is
     * conservative: **`1.0`**, which is a true upper bound for any fuzzy number
     * (they are normal) and therefore never *under*-reports — but it is useless,
     * and it is why every concrete number here overrides it.
     *
     * Note it is a bound, not a lie: `MembershipFnLaws`' `override ≥ fold` holds
     * for the default. An implementation that cannot do better should keep it;
     * one that can should not.
     *
     * @param lo the window's lower endpoint.
     * @param hi the window's upper endpoint, `≥ lo`.
     */
    public fun supremumOver(lo: Double, hi: Double): Double = 1.0

    /**
     * `height(over) == 1` — and it reads the carrier, because [height] does.
     *
     * A fuzzy number is normal **on ℝ**, so it is tempting to return `true` flat.
     * That is §20.8's bug in miniature: normal on ℝ does not mean normal on the
     * window you were handed. `TriangularNumber(-0.5, 0.5, 1.5)` is not normal
     * over `Sampled(2, 3)` — it is identically zero there.
     */
    override fun isNormal(over: Domain<Double>): Boolean = height(over) >= 1.0

    public companion object {

        /** Rejects an α outside `(0, 1]`, with the reason Zadeh gives. */
        @JvmStatic
        public fun requireCutLevel(alpha: Double): Double {
            Degrees.requireDegree(alpha, "alpha")
            require(alpha > 0.0) {
                "α-cut level must be in (0, 1], but was $alpha: Γ_0 = {x | f(x) ≥ 0} is all of ℝ " +
                    "for every fuzzy set, which is why Zadeh restricts α to (0,1] throughout §V"
            }
            return alpha
        }
    }
}
