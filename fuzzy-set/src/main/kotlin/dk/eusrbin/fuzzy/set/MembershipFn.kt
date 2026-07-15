package dk.eusrbin.fuzzy.set

import dk.eusrbin.fuzzy.algebra.Degrees
import kotlin.math.max

/**
 * A **membership function** `f_A : X → [0,1]` — a fuzzy set.
 *
 * > "A fuzzy set (class) `A` in `X` is characterized by a membership
 * > (characteristic) function `f_A(x)` which associates with each point in `X` a
 * > real number in the interval `[0,1]`, with the value of `f_A(x)` at `x`
 * > representing the 'grade of membership' of `x` in `A`."
 *
 * Source: Zadeh 1965, §II (p.339).
 *
 * Take that definition literally: **a fuzzy set *is* its membership function.**
 * There is no `FuzzySet` class wrapping one, because Zadeh's sentence does not
 * describe a set that *has* a membership function — it says the function
 * characterises the set completely. Nothing else is carried.
 *
 * ## The domain is not part of a set's identity
 *
 * CLAUDE.md §15.1, which is the hinge of this whole module. `X` appears as a type
 * parameter; the *domain* — the thing that can enumerate or sample `X` — does not
 * appear at all. It is supplied at the call site of the operations that need it:
 *
 * ```kotlin
 * val a: MembershipFn<String> = MembershipFn { if (it == "warm") 0.8 else 0.2 }
 * a.height(over = Enumerable.of("cold", "warm", "hot"))   // needs a Domain
 * a.complement()                                          // does not
 * ```
 *
 * §3 originally proposed carrying the domain in the set's type
 * (`FuzzySet<X, D : Domain<X>>`). §15.1 rejects that, and its argument is §3's
 * own words: pointwise operations are *"representation-free ... **no domain
 * needed**"*. If a fuzzy set does not need a domain in order to *be* one, the
 * domain is not part of its identity.
 *
 * The compile-time guarantee §3 wanted survives intact, and gets cheaper: you
 * cannot ask for a supremum without producing a [Domain] to supply it, because
 * it is a required argument. See [Domain] for the rest of the seam.
 *
 * ## Implementing this interface
 *
 * A `fun interface` (§9), so a Java lambda or a Clojure `reify` satisfies it
 * directly:
 *
 * ```java
 * MembershipFn<String> warm = x -> x.equals("warm") ? 0.8 : 0.2;
 * ```
 * ```clojure
 * (reify MembershipFn (apply [_ x] (if (= x "warm") 0.8 0.2)))
 * ```
 *
 * ## Boxing
 *
 * §9: *"`MembershipFn<X>` **may box** X; the return must stay primitive."* The
 * return type is `double` and always will be — that is the value CLAUDE.md §4
 * protects, since the hot path is a supremum over a sampled grid. `X` boxes
 * because the JVM gives no choice for a generic parameter.
 *
 * For `X = ℝ` — Zadeh's common case, and the only one where the grid is millions
 * of points — use [DoubleMembershipFn], which takes a primitive `double` too.
 *
 * ## Preconditions
 *
 * Implementations **must** return a value in `[0,1]`. This is not checked, for
 * the reason given in [dk.eusrbin.fuzzy.algebra.Degrees]: validating on every
 * call puts a branch in the innermost loop. Use `Degrees.clamp` at your own
 * boundary if you are unsure of your data.
 */
public fun interface MembershipFn<X> {

    /**
     * The grade of membership of [x] in this set — Zadeh's `f_A(x)`.
     *
     * Must return a value in `[0,1]`. The named method; [invoke] is Kotlin-only
     * sugar over it (§9).
     */
    public fun apply(x: X): Double

    /** Kotlin sugar for [apply], letting you write `A(x)`. Java callers use [apply]. */
    public operator fun invoke(x: X): Double = apply(x)

    // ---- Analysis: everything below needs a Domain ------------------------
    //
    // CLAUDE.md §3's seam: these are the operations that cannot be answered from
    // a black-box function, because each is a fold or a ∀ over X. §15.1 supplies
    // the domain here, at the call site, rather than in this type's parameters.
    //
    // They are MEMBERS with default bodies, not extension functions, and §16.5
    // records why: §15.3 requires that a parametric function be able to override
    // them with a closed form ("a TriangularNumber overrides height with the
    // analytic answer and ignores the domain entirely"), and an extension cannot
    // be overridden. This is TNorm.residuum's shape from slice 1 — generic
    // default, overridden where a closed form exists — which is what §15.3 says
    // to reuse. `fun interface` survives: only `apply` is abstract.
    //
    // Operations that BUILD a new set from old ones are combinators, not
    // queries, and live in FuzzySets (§16.5).

    /**
     * `Sup_x f_A(x)` — the greatest degree this set attains over [over].
     *
     * **Source:** Zadeh 1965, §V p.348 — *"let `M = Sup_x f_A(x)`. (M will be
     * referred to as the **maximal grade** in A.)"*
     * **Attributed:** the name "height", which is later nomenclature; Zadeh has
     * no such word (CLAUDE.md §18.2).
     *
     * Over a [Sampled] domain this is a **lower bound** on the true supremum, not
     * the supremum: a spike between two grid points is missed. Refine and compare
     * — §15.1 keeps the domain out of this type precisely so that you can.
     */
    public fun height(over: Domain<X>): Double =
        over.reduceDegrees(this, 0.0) { accumulator, degree -> max(accumulator, degree) }

    /**
     * `Σ_x f_A(x)` — the **σ-count**, a fuzzy cardinality.
     *
     * **Attributed:** the name "σ-count" and the notion of a fuzzy set's
     * cardinality are standard, and neither source on hand defines them
     * (CLAUDE.md §18.2). The *arithmetic* is not in doubt — it is a sum — which
     * is why this ships where intensification did not (§17.4): an unverifiable
     * **name** and an unverifiable **formula** are different failures.
     *
     * Counts what the domain enumerates, duplicates included — see [Enumerable].
     * Over a [Sampled] domain this is a grid sum, **not an integral**: it scales
     * with the point count, so it is comparable across sets on one domain and
     * meaningless across domains of different resolution.
     */
    public fun sigmaCount(over: Domain<X>): Double =
        over.reduceDegrees(this, 0.0) { accumulator, degree -> accumulator + degree }

    /**
     * `Γ_α = {x | f_A(x) ≥ α}` — the **α-cut**.
     *
     * **Source:** Zadeh 1965, §V eq. **(24)**, p.347, verbatim. He introduces it
     * for `α ∈ (0,1]` while defining convexity (*"A is convex if and only if the
     * sets Γ_α … are convex for all α in the interval (0,1]"*), but the set
     * itself is domain-generic and needs none of §V's Eⁿ — which is why §15.5
     * places it in 2a while convexity waits for 2b.
     *
     * Over a [Sampled] domain the result is a *sample* of the cut: the α-cut of a
     * continuous function is an interval, and what comes back are the grid points
     * inside it.
     *
     * @throws IllegalArgumentException if [alpha] is not a degree in `[0,1]`.
     */
    public fun alphaCut(over: Domain<X>, alpha: Double): List<X> {
        Degrees.requireDegree(alpha, "alpha")
        return over.filter { x -> apply(x) >= alpha }
    }

    /**
     * `{x | f_A(x) > α}` — the **strong α-cut**.
     *
     * **Derived** from Zadeh's eq. (24) by reading `>` for `≥`; the paper defines
     * no such set, though it uses one ad hoc in the p.353 proof (*"the convex sets
     * `Γ_A = {x | f_A(x) > M}`"*). **Attributed:** the name.
     *
     * @throws IllegalArgumentException if [alpha] is not a degree in `[0,1]`.
     */
    public fun strongAlphaCut(over: Domain<X>, alpha: Double): List<X> {
        Degrees.requireDegree(alpha, "alpha")
        return over.filter { x -> apply(x) > alpha }
    }

    /**
     * `{x | f_A(x) > 0}` — the **support**.
     *
     * **Derived**: `strongAlphaCut(over, 0.0)`. **Attributed:** the name, which is
     * not in the paper (CLAUDE.md §18.2) — but the notion is, at p.342: *"it is
     * not meaningful to speak of a point x 'belonging' to a fuzzy set A except in
     * the trivial sense of `f_A(x)` being positive."*
     */
    public fun support(over: Domain<X>): List<X> = strongAlphaCut(over, 0.0)

    /**
     * `{x | f_A(x) = 1}` — the **core**, in its modern sense.
     *
     * **Attributed:** later nomenclature. **This is deliberately *not* Zadeh's
     * `C(A)`** — see [maximalGradeSet], which is. CLAUDE.md §18.3 ratifies
     * shipping both rather than letting one word mean two things.
     *
     * The two coincide **exactly when [isNormal]**, and differ for every
     * subnormal set: there this is **empty** while [maximalGradeSet] is not.
     */
    public fun core(over: Domain<X>): List<X> = over.filter { x -> apply(x) >= 1.0 }

    /**
     * `{x | f_A(x) = M}` where `M = height(over)` — **the set where the maximal
     * grade is attained**.
     *
     * **Source:** Zadeh 1965, §V p.349 — *"let `C(A)` be the set of all points in
     * X at which M is essentially attained. This set will be referred to as the
     * **core** of A."* — modulo *"essentially"*, see below. "Maximal grade" is
     * his own term (p.348), which is why this carries it rather than borrowing a
     * word he used for something else.
     *
     * **On "essentially attained".** Zadeh's is a topological notion resting on
     * p.348's ε-neighbourhood Lemma, and so needs Eⁿ. This takes the set where M
     * **is** attained, which is domain-generic. Over an [Enumerable] they agree —
     * a finite set attains its supremum. Over a [Sampled] they can differ, since
     * the true supremum may be approached between grid points and reached at none
     * of them; that is [Sampled]'s standing caveat rather than a new one.
     *
     * Costs two folds: one for the height, one for the filter.
     */
    public fun maximalGradeSet(over: Domain<X>): List<X> {
        val maximalGrade = height(over)
        return over.filter { x -> apply(x) >= maximalGrade }
    }

    /**
     * `Sup_x f_A(x) = 1` — whether this set reaches full membership somewhere.
     *
     * **Attributed:** "normal" is standard and is not in the paper. It derives
     * from [height], and it is the exact condition under which [core] and
     * [maximalGradeSet] are the same set — which is why it is worth naming rather
     * than leaving a reader to find out.
     */
    public fun isNormal(over: Domain<X>): Boolean = height(over) >= 1.0

    /**
     * `A ⊂ B ⟺ f_A ≤ f_B` — **containment**, as a [Verdict].
     *
     * **Source:** Zadeh 1965, §II eq. **(2)**, p.340.
     *
     * **This is not a pointwise operation, and that surprises people.** `f_A ≤ f_B`
     * is a **∀ over X**, so it needs a [Domain] where union and intersection do
     * not — CLAUDE.md §3 flags exactly this as the counter-intuitive half of the
     * seam.
     *
     * Returns a [Verdict] rather than a boolean because over a [Sampled] domain
     * "no counterexample" is not "contained" (§15.6, §16.4). Where a boolean is
     * honest — an [Enumerable] — [isContainedIn] ships one.
     */
    public fun checkContainment(other: MembershipFn<X>, over: Domain<X>): Verdict<X> =
        Verdict.of(over.firstWhere { x -> apply(x) > other.apply(x) }, over)

    /**
     * `A = B ⟺ f_A(x) = f_B(x) ∀x` — **equality**, as a [Verdict].
     *
     * **Source:** Zadeh 1965, §II p.340 — *"Two fuzzy sets A and B are equal …
     * if and only if `f_A(x) = f_B(x)` for all x in X."* **No equation number**:
     * it is a prose definition, and CLAUDE.md §17.1 records that inventing one
     * would have been easy and wrong.
     *
     * Note this is **exact** equality of degrees, as Zadeh writes it — no
     * tolerance. Two membership functions that agree to 1e-16 are not equal, and
     * a caller who wants them to be is asking a different question than the one
     * p.340 defines. (`fuzzy-laws` has the tolerance machinery for that question;
     * it is not this one.)
     */
    public fun checkEquality(other: MembershipFn<X>, over: Domain<X>): Verdict<X> =
        Verdict.of(over.firstWhere { x -> apply(x) != other.apply(x) }, over)

    /**
     * Whether `f_A` is identically zero — **emptiness**, as a [Verdict].
     *
     * **Source:** Zadeh 1965, §II p.340 — *"A fuzzy set is empty if and only if
     * its membership function is identically zero on X."* No equation number.
     */
    public fun checkEmptiness(over: Domain<X>): Verdict<X> =
        Verdict.of(over.firstWhere { x -> apply(x) > 0.0 }, over)

    /**
     * `A ⊂ B`, as a boolean — **[Enumerable] only**.
     *
     * The sugar §16.4 ratifies alongside [Verdict]: an [Enumerable] fold visits
     * every element of `X`, so "no counterexample" *is* a proof and a boolean
     * tells no lie. The type of the argument is what makes that safe — there is
     * deliberately no such overload for [Domain].
     *
     * Note what it costs: `false` **discards the witness**. When you want to know
     * *where* containment fails, use [checkContainment], which is this method's
     * own implementation.
     */
    public fun isContainedIn(other: MembershipFn<X>, over: Enumerable<X>): Boolean =
        checkContainment(other, over).isProven

    /** `A = B`, as a boolean — **[Enumerable] only**. See [isContainedIn] on why. */
    public fun isEqualTo(other: MembershipFn<X>, over: Enumerable<X>): Boolean =
        checkEquality(other, over).isProven

    /** Whether `f_A ≡ 0`, as a boolean — **[Enumerable] only**. See [isContainedIn] on why. */
    public fun isEmpty(over: Enumerable<X>): Boolean = checkEmptiness(over).isProven

    /**
     * The **decomposition** (representation) of this set into its α-cuts:
     * `A = ⋃_α α·Γ_α`.
     *
     * **Derived from Zadeh's eq. (24)**, and needs no source beyond it — the
     * theorem is near-tautologous once `Γ_α = {x | f_A(x) ≥ α}` is in hand:
     *
     * ```
     * sup { α | x ∈ Γ_α }  =  sup { α | f_A(x) ≥ α }  =  f_A(x)
     * ```
     *
     * **Attributed:** the names "decomposition" / "representation theorem", which
     * are not in the paper (CLAUDE.md §18.2).
     *
     * [FuzzySets.fromDecomposition] is the other direction, and
     * `fuzzy-laws` asserts the round trip.
     *
     * @param levels the α values to cut at. **Only these are recoverable** — the
     *   reconstruction can return no degree that is not among them, so the round
     *   trip is exact only if every degree the set attains appears here. The
     *   [decompose] overload taking no levels does that for you.
     * @throws IllegalArgumentException if any level is not a degree in `[0,1]`.
     */
    public fun decompose(over: Domain<X>, levels: DoubleArray): List<AlphaLevel<X>> {
        for (level in levels) Degrees.requireDegree(level, "level")
        return levels.map { alpha -> AlphaLevel(alpha, alphaCut(over, alpha)) }
    }

    /**
     * The decomposition at **every distinct degree this set attains** over [over],
     * which makes the round trip exact.
     *
     * `sup { α | f_A(x) ≥ α }` recovers `f_A(x)` exactly when `f_A(x)` is itself
     * among the levels — so cutting at the attained degrees is the level set that
     * loses nothing. Zero is excluded: `Γ_0` is the whole domain and contributes
     * nothing a reconstruction can use.
     *
     * Costs `O(|domain|²)` — one pass to collect the degrees, then one cut per
     * distinct degree. Fine for an [Enumerable] term set; for a [Sampled] grid at
     * the default 1024 points that is ~10⁶ evaluations, so pass your own levels
     * if you have them.
     */
    public fun decompose(over: Domain<X>): List<AlphaLevel<X>> {
        val attained = sortedSetOf<Double>()
        for (x in over.elements()) {
            val degree = apply(x)
            if (degree > 0.0) attained.add(degree)
        }
        return decompose(over, attained.toDoubleArray())
    }
}

/**
 * One level of a [MembershipFn.decompose] — an `α` and its cut `Γ_α`.
 *
 * **Source:** Zadeh 1965, §V eq. (24), p.347 for `Γ_α`. **Attributed:** the
 * pairing as a "level set", which is later vocabulary.
 *
 * @property alpha the level `α`.
 * @property cut `Γ_α = {x | f_A(x) ≥ α}`, over whatever domain produced it.
 */
public class AlphaLevel<X>(
    public val alpha: Double,
    public val cut: List<X>,
) {
    override fun toString(): String = "AlphaLevel(α=$alpha, |Γ_α|=${cut.size})"
}
