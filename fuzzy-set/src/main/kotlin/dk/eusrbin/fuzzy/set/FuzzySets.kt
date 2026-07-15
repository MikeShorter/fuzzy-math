package dk.eusrbin.fuzzy.set

import dk.eusrbin.fuzzy.algebra.Algebra
import dk.eusrbin.fuzzy.algebra.Degrees
import dk.eusrbin.fuzzy.algebra.Negation
import dk.eusrbin.fuzzy.algebra.Negations
import dk.eusrbin.fuzzy.algebra.TConorm
import dk.eusrbin.fuzzy.algebra.TNorm
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow

/**
 * The **combinators** — operations that build a new fuzzy set from old ones.
 *
 * Zadeh 1965 §II (complement, union, intersection) and §IV (the algebraic
 * operations), plus the hedges from Bergmann §16.1. Every equation number here
 * has been read off the paper (CLAUDE.md §17.1); none is from memory.
 *
 * ## Why these are here and the analysis is on `MembershipFn`
 *
 * CLAUDE.md §16.5 draws the line as **queries vs combinators**, and it coincides
 * exactly with **overridable vs not**:
 *
 * - a *query* — `height`, `alphaCut`, `checkContainment` — asks something about a
 *   set, and a parametric function may know the answer analytically. §15.3
 *   requires those be overridable, so they are members of [MembershipFn].
 * - a *combinator* — everything in this object — builds a new set. The union of
 *   two triangular numbers is not triangular, so there is nothing to override.
 *
 * They are `@JvmStatic` rather than extension functions because §9 says
 * *"extension functions are sugar only, never core operations"*.
 *
 * ## Everything is pointwise, and needs no `Domain`
 *
 * CLAUDE.md §3: these are *"representation-free. Work over **any** X, lazily,
 * given only a membership function. No domain needed."* Every result is a lazy
 * [MembershipFn] that computes nothing until asked — `union(a, b)` allocates one
 * object and evaluates neither argument.
 *
 * That is the whole reason §15.1 keeps the domain out of a set's type: these stay
 * trivially closed, with no domain algebra to invent for the result.
 *
 * ## The algebra is a parameter, and Zadeh's is the default
 *
 * CLAUDE.md §6: *"Min/max are not a special mechanism — they are just the Gödel
 * t-norm. There is one parameterised mechanism, and Zadeh's algebra is its
 * default instantiation."* So [union] takes an [Algebra] defaulting to
 * [Algebra.STANDARD], and Zadeh 1965 §II falls out as the default rather than
 * being special-cased. **There is no `min`/`max` anywhere in this module** —
 * `fuzzy-set` does not reimplement `fuzzy-algebra`, it instantiates it.
 */
public object FuzzySets {

    // ---- Zadeh §II: the defining operations --------------------------------

    /**
     * `A'` — the **complement**. `f_A' = 1 − f_A`.
     *
     * **Source:** Zadeh 1965, §II eq. **(1)**, p.340, verbatim (for the default
     * [Negations.STANDARD]).
     *
     * Parameterised by [negation], because a complement is a [Negation] — §6's
     * one-mechanism rule. Zadeh 1965 defines only the standard case; the Sugeno
     * and Yager families are reachable here and are not what he wrote.
     */
    @JvmStatic
    @JvmOverloads
    public fun <X> complement(
        a: MembershipFn<X>,
        negation: Negation = Negations.STANDARD,
    ): MembershipFn<X> = MembershipFn { x -> negation.apply(a.apply(x)) }

    /**
     * `A ∪ B` — the **union**. `f_C(x) = Max[f_A(x), f_B(x)]`.
     *
     * **Source:** Zadeh 1965, §II eq. **(3)**, p.340 (abbreviated `f_A ∨ f_B` at
     * eq. (4)) — for the default [Algebra.STANDARD], whose conorm *is* `Max`.
     *
     * Zadeh's own justification for `Max` is worth knowing, because it is not
     * arbitrary (p.341): *"The union of A and B is the smallest fuzzy set
     * containing both A and B."* `Max` is forced by that requirement, not chosen.
     *
     * A union is a [TConorm]; pass another [Algebra] to get another one.
     */
    @JvmStatic
    @JvmOverloads
    public fun <X> union(
        a: MembershipFn<X>,
        b: MembershipFn<X>,
        algebra: Algebra = Algebra.STANDARD,
    ): MembershipFn<X> = union(a, b, algebra.tConorm)

    /** [union] with a raw [TConorm] — the escape hatch of §6, minus the bundle's guarantees. */
    @JvmStatic
    public fun <X> union(
        a: MembershipFn<X>,
        b: MembershipFn<X>,
        tConorm: TConorm,
    ): MembershipFn<X> = MembershipFn { x -> tConorm.apply(a.apply(x), b.apply(x)) }

    /**
     * `A ∩ B` — the **intersection**. `f_C(x) = Min[f_A(x), f_B(x)]`.
     *
     * **Source:** Zadeh 1965, §II eq. **(5)**, p.341 (abbreviated `f_A ∧ f_B` at
     * eq. (6)) — for the default [Algebra.STANDARD], whose t-norm *is* `Min`.
     *
     * Dual to [union] in Zadeh's own framing (p.341): *"the intersection of A and
     * B is the largest fuzzy set which is contained in both A and B."*
     *
     * An intersection is a [TNorm]; pass another [Algebra] to get another one.
     */
    @JvmStatic
    @JvmOverloads
    public fun <X> intersection(
        a: MembershipFn<X>,
        b: MembershipFn<X>,
        algebra: Algebra = Algebra.STANDARD,
    ): MembershipFn<X> = intersection(a, b, algebra.tNorm)

    /** [intersection] with a raw [TNorm] — the escape hatch of §6, minus the bundle's guarantees. */
    @JvmStatic
    public fun <X> intersection(
        a: MembershipFn<X>,
        b: MembershipFn<X>,
        tNorm: TNorm,
    ): MembershipFn<X> = MembershipFn { x -> tNorm.apply(a.apply(x), b.apply(x)) }

    // ---- Zadeh §IV: the algebraic operations -------------------------------

    /**
     * `AB` — the **algebraic product**. `f_AB = f_A f_B`.
     *
     * **Source:** Zadeh 1965, §IV eq. **(14)**, p.344.
     *
     * CLAUDE.md §6's central observation lives here: this **is** the Product
     * t-norm, so `algebraicProduct(a, b)` and
     * `intersection(a, b, Algebra.PRODUCT)` are the same function. Zadeh filed
     * §II's intersection and §IV's product separately, without noting they were
     * one mechanism at two parameter values. `fuzzy-laws` asserts they agree.
     *
     * Zadeh notes eq. **(15)**: `AB ⊂ A ∩ B` — the product is contained in the
     * (Min) intersection. That is a law, and `fuzzy-laws` checks it.
     */
    @JvmStatic
    public fun <X> algebraicProduct(a: MembershipFn<X>, b: MembershipFn<X>): MembershipFn<X> =
        MembershipFn { x -> a.apply(x) * b.apply(x) }

    /**
     * `A + B` — the **algebraic sum**. `f_{A+B} = f_A + f_B`.
     *
     * **Source:** Zadeh 1965, §IV eq. **(16)**, p.344.
     *
     * ## This operation is partial, and that is Zadeh's own doing
     *
     * Read eq. (16) with its proviso, because the proviso is the interesting part:
     *
     * > *"provided the sum `f_A + f_B` is less than or equal to unity. Thus,
     * > unlike the algebraic product, the algebraic sum is meaningful only when
     * > the condition `f_A(x) + f_B(x) ≤ 1` is satisfied for all x."*
     *
     * So `A + B` is **not defined** where the sum exceeds 1, and this function
     * will happily return a value outside `[0,1]` there — which is not a degree.
     * It is faithful to the paper, and the paper's operation is partial.
     *
     * **The precondition is a ∀ over X**, exactly like containment, so it needs a
     * [Domain] to check — see [checkAlgebraicSumDefined], which makes Zadeh's
     * side-condition executable rather than a remark.
     *
     * CLAUDE.md §6 reads the proviso as the tell: *"His `f_A + f_B ≤ 1`
     * side-condition is an uncapped Łukasiewicz conorm missing its `min(1, ·)`."*
     * If you want the total operation, that is
     * `union(a, b, Algebra.LUKASIEWICZ)` — `min(1, f_A + f_B)` — which agrees
     * with this wherever this is defined, and is a degree everywhere. `fuzzy-laws`
     * asserts that agreement.
     */
    @JvmStatic
    public fun <X> algebraicSum(a: MembershipFn<X>, b: MembershipFn<X>): MembershipFn<X> =
        MembershipFn { x -> a.apply(x) + b.apply(x) }

    /**
     * Whether Zadeh's side-condition on [algebraicSum] — `f_A(x) + f_B(x) ≤ 1`
     * **for all x** — holds over [over].
     *
     * **Source:** Zadeh 1965, §IV eq. (16)'s proviso, p.344.
     *
     * Eq. (16) is meaningful *"only when the condition is satisfied for all x"*.
     * That is a ∀ over X, so it takes a [Domain] and returns a [Verdict] for the
     * reasons in §15.6 — over a [Sampled] grid, "no counterexample" is not a
     * proof, and a witness is a witness regardless.
     *
     * [Verdict.Refuted]'s witness is an `x` where `A + B` leaves `[0,1]` and
     * therefore is not a fuzzy set.
     */
    @JvmStatic
    public fun <X> checkAlgebraicSumDefined(
        a: MembershipFn<X>,
        b: MembershipFn<X>,
        over: Domain<X>,
    ): Verdict<X> = Verdict.of(over.firstWhere { x -> a.apply(x) + b.apply(x) > 1.0 }, over)

    /**
     * `|A − B|` — the **absolute difference**. `f_{|A−B|} = |f_A − f_B|`.
     *
     * **Source:** Zadeh 1965, §IV, p.344. **No equation number** — it is a prose
     * definition sitting between eq. (16) and eq. (17), and CLAUDE.md §17.1
     * records that inventing one would have been easy and wrong.
     *
     * Zadeh's note: *"in the case of ordinary sets `|A − B|` reduces to the
     * relative complement of `A ∩ B` in `A ∪ B`."*
     *
     * Total, unlike [algebraicSum]: `|f_A − f_B|` is in `[0,1]` for any degrees.
     */
    @JvmStatic
    public fun <X> absoluteDifference(a: MembershipFn<X>, b: MembershipFn<X>): MembershipFn<X> =
        MembershipFn { x -> abs(a.apply(x) - b.apply(x)) }

    /**
     * `A ⊖ B` — the **bounded difference**. `f_{A⊖B} = Max[0, f_A − f_B]`.
     *
     * **Attributed:** standard t-norm-literature vocabulary. **This is *not* in
     * Zadeh 1965** — §IV (pp.344–346) has been read in full and contains no such
     * operation; it is later terminology (CLAUDE.md §17.3).
     *
     * **Source, for the mathematics:** it is not a new mechanism, and is fully
     * determined by things we have read —
     *
     * ```
     * A ⊖ B  =  T_Łukasiewicz(A, B')        Bergmann §11.2 + Zadeh eq. (1)
     * ```
     *
     * since `max(0, f_A + (1 − f_B) − 1) = max(0, f_A − f_B)`. That identity is
     * CLAUDE.md §6's thesis in one line: Zadeh's operations, and this one, are
     * instantiations of a single parameterised mechanism.
     *
     * Implemented as `max(0, f_A − f_B)` directly rather than as
     * `intersection(a, complement(b), Algebra.LUKASIEWICZ)`, which computes the
     * same function through three roundings instead of one and is therefore
     * *less* accurate. The identity is asserted by `fuzzy-laws` instead — an
     * executable claim beats a comment, and beats a slower implementation.
     */
    @JvmStatic
    public fun <X> boundedDifference(a: MembershipFn<X>, b: MembershipFn<X>): MembershipFn<X> =
        MembershipFn { x -> max(0.0, a.apply(x) - b.apply(x)) }

    /**
     * `(A, B; Λ)` — the **convex combination** of `A` and `B` by `Λ`.
     *
     * **Source:** Zadeh 1965, §IV eqs. **(17)** and **(18)**, p.345:
     *
     * ```
     * (A, B; Λ) = ΛA + Λ'B                                            (17)
     * f_{(A,B;Λ)}(x) = f_Λ(x)f_A(x) + [1 − f_Λ(x)]f_B(x),   x ∈ X     (18)
     * ```
     *
     * ## Λ is a **fuzzy set**, not a scalar
     *
     * The thing most likely to be got wrong, and CLAUDE.md §17.2 records that
     * reading the paper is what caught it. Eq. (17) says *"Let A, B, and **Λ** be
     * arbitrary **fuzzy sets**"* — so the weighting varies with `x`. A scalar λ
     * appears in the paper only inside eq. (20)'s proof, and in the introductory
     * sentence about combining **vectors**, which is where the scalar reading
     * comes from and why it is wrong.
     *
     * The scalar case is the special case where Λ is constant — see the
     * [convexCombination] overload taking a `Double`, which is sugar for
     * `Λ = constant(λ)` and not the definition.
     *
     * ## It needs no vector space
     *
     * Despite the name, this is **pointwise arithmetic on degrees** — nothing is
     * combined in `X`. CLAUDE.md §15.5's vector-space requirement is about §V's
     * convexity (eq. 25, `f_A[λx₁ + (1−λ)x₂]`, which *does* form a segment in X);
     * eq. (18) forms a segment in `[0,1]`, between two degrees. So this belongs in
     * 2a and convexity waits for 2b.
     *
     * Zadeh's eq. **(19)**: `A ∩ B ⊂ (A, B; Λ) ⊂ A ∪ B` for all Λ — a law, and
     * `fuzzy-laws` checks it.
     */
    @JvmStatic
    public fun <X> convexCombination(
        a: MembershipFn<X>,
        b: MembershipFn<X>,
        lambda: MembershipFn<X>,
    ): MembershipFn<X> = MembershipFn { x ->
        val weight = lambda.apply(x)
        weight * a.apply(x) + (1.0 - weight) * b.apply(x)
    }

    /**
     * [convexCombination] with a **constant** Λ — `λf_A(x) + (1 − λ)f_B(x)`.
     *
     * Sugar for `convexCombination(a, b, constant(lambda))`, and **not the
     * definition** — see the three-set overload, and §17.2. This is the shape
     * eq. (20)'s proof uses (*"Min[f_A(x), f_B(x)] ≤ λf_A(x) + (1 − λ)f_B(x) ≤
     * Max[f_A(x), f_B(x)]"*, p.345), so it is Zadeh's too — as a special case.
     *
     * @throws IllegalArgumentException if [lambda] is not a degree in `[0,1]`.
     */
    @JvmStatic
    public fun <X> convexCombination(
        a: MembershipFn<X>,
        b: MembershipFn<X>,
        lambda: Double,
    ): MembershipFn<X> {
        Degrees.requireDegree(lambda, "lambda")
        return convexCombination(a, b, constant(lambda))
    }

    // ---- Hedges: Bergmann §16.1 --------------------------------------------

    /**
     * `f_A^p` — the **power** hedge, the family the others are instances of.
     *
     * **Source:** Bergmann 2008, §16.1 — hedges are *"a function mapping
     * membership degrees (values in the unit interval `[0..1]`) to membership
     * degrees"*, and he gives `I(very)(n) = n²` and `n⁴` for `very very`. This is
     * that family, named.
     *
     * **Attributed:** the term "hedge" itself — Bergmann §16.1 n.1: *"The term was
     * coined by the linguist George Lakoff (1973)."* (That attribution *is*
     * checked: it is in a source on hand.)
     *
     * Raising the exponent raises the membership threshold — §16.1: *"`very` …
     * systematically produces new vague concepts by raising the threshold for
     * membership in fuzzy sets."* Below 1 it lowers it.
     *
     * @param p the exponent, which must be positive. `p = 1` is `a` itself.
     * @throws IllegalArgumentException if [p] is not finite and positive.
     */
    @JvmStatic
    public fun <X> power(a: MembershipFn<X>, p: Double): MembershipFn<X> {
        require(p.isFinite() && p > 0.0) { "Hedge exponent must be finite and positive, but was $p" }
        return MembershipFn { x -> a.apply(x).pow(p) }
    }

    /**
     * `f_A²` — **concentration**, the hedge `very`.
     *
     * **Source:** Bergmann 2008, §16.1, verbatim: *"The particular function that's
     * usually used for `very` is the square function."* This is literally his
     * `I(very)`.
     *
     * **Attributed:** the name "concentration" (and the CON/DIL nomenclature),
     * which Bergmann does not use — the whole book contains no occurrence of it
     * (CLAUDE.md §17.4).
     *
     * Iterate for `very very`: `concentration(concentration(a))` is `f^4`, which
     * is §16.1's own example.
     */
    @JvmStatic
    public fun <X> concentration(a: MembershipFn<X>): MembershipFn<X> = power(a, 2.0)

    /**
     * `f_A^0.5` — **dilation**, loosely `more or less`.
     *
     * **Source, for the mathematics:** [power] at `p = 0.5` — an instance of a
     * verified operation, needing nothing Bergmann §16.1 does not give.
     *
     * **Attributed:** the name "dilation" *and the choice of `0.5`*. Neither is in
     * either source on hand (CLAUDE.md §17.4). Bergmann's own threshold-lowering
     * hedge is `close-to`, and it is a different function — `min(1, n + 0.1)`, a
     * linear shift, not a root.
     *
     * Naming a function is not a claim about mathematics, which is why this ships
     * where **intensification** did not: that one is piecewise (`2f²` below `0.5`,
     * `1 − 2(1−f)²` above), so it cannot be recovered from anything §16.1 covers,
     * and its *formula* would have been the unverified part.
     */
    @JvmStatic
    public fun <X> dilation(a: MembershipFn<X>): MembershipFn<X> = power(a, 0.5)

    // ---- Constructions ------------------------------------------------------

    /**
     * The set with `f_A(x) = degree` everywhere.
     *
     * `constant(0.0)` is Zadeh's **empty** set — §II, p.340: *"A fuzzy set is
     * empty if and only if its membership function is identically zero on X."*
     * `constant(1.0)` is `X` itself, as a fuzzy set.
     *
     * @throws IllegalArgumentException if [degree] is not in `[0,1]`.
     */
    @JvmStatic
    public fun <X> constant(degree: Double): MembershipFn<X> {
        Degrees.requireDegree(degree, "degree")
        // `{ _ -> degree }`, not `{ degree }`: a SAM conversion needs the
        // parameter to be accounted for, even when it is ignored.
        return MembershipFn { _ -> degree }
    }

    /**
     * Rebuilds a set from its α-cuts: `f_A(x) = sup { α | x ∈ Γ_α }`.
     *
     * The inverse of [MembershipFn.decompose], and **derived from Zadeh's eq.
     * (24)** rather than needing a source of its own — `sup { α | f_A(x) ≥ α }`
     * *is* `f_A(x)`. **Attributed:** the name "representation theorem".
     *
     * Exact only if the levels include every degree the set attains: nothing can
     * come back that is not among them. `decompose(over)` with no explicit levels
     * guarantees that, and `fuzzy-laws` asserts the round trip.
     *
     * `x` not in any cut gives `0.0` — correct, since `sup ∅ = 0` here by the
     * convention that a point in no cut has no membership.
     *
     * Costs `O(levels × |cut|)` per evaluation: it is a linear scan with
     * `List.contains`, so this is for round-tripping and inspection, not a hot
     * path.
     */
    @JvmStatic
    public fun <X> fromDecomposition(levels: List<AlphaLevel<X>>): MembershipFn<X> {
        val snapshot = levels.toList()
        return MembershipFn { x ->
            var supremum = 0.0
            for (level in snapshot) {
                if (level.alpha > supremum && level.cut.contains(x)) supremum = level.alpha
            }
            supremum
        }
    }
}
