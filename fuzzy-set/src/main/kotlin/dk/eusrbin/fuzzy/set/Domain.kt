package dk.eusrbin.fuzzy.set

/**
 * **The capability seam** — the ability to fold over `X`.
 *
 * CLAUDE.md §3 is the founding statement and §15 is the correction; read §15
 * first. §3's central observation stands and is the reason this type exists:
 * fuzzy-set operations split cleanly in two.
 *
 * | | | |
 * |---|---|---|
 * | **Pointwise** | complement, union, intersection, product, sum, hedges | need nothing |
 * | **Needs `Sup` over X** | height, support, core, α-cuts, cardinality, **containment, equality, emptiness** | need a `Domain` |
 *
 * > *"You cannot take a `Sup` over an uncountable X given only a black-box
 * > function."*
 *
 * The surprise in that table is the last row. `f_A ≤ f_B` looks pointwise and is
 * not: it is a **∀ over X**, so Zadeh's eq. (2) — containment — needs a domain,
 * as do equality and emptiness. People expect to live with that constraint on
 * `height`; they are startled by it on `⊂`. See [MembershipFn] for the other half
 * of §15.1, and note that union and intersection are *not* here — they stay
 * closed and domain-free.
 *
 * ## The seam is enforced by an argument, not a type parameter
 *
 * §15.1. A `Domain<X>` is a **parameter of the operations that need one**, never
 * part of a set's type:
 *
 * ```kotlin
 * fun <X> MembershipFn<X>.height(over: Domain<X>): Double     // decided
 * class FuzzySet<X, D : Domain<X>>                            // rejected (§3's shape)
 * ```
 *
 * §3 wanted "asking for a Sup over a domain you cannot search must be a **compile
 * error**, not a runtime failure". It still is — you cannot call `height` without
 * producing a `Domain<X>`, and for an opaque `X` no `Domain<X>` exists to
 * produce. The guarantee is delivered by the ordinary mechanism of a required
 * argument, and it comes out *stronger*: the same set can be analysed over
 * several domains — sample coarsely, then finely, and watch the answers converge
 * — which §3's shape made a type error.
 *
 * ## Two cases, not four
 *
 * §3 listed `Enumerable`, `Sampled`, `Parametric` and `Opaque`. Three of those
 * were wrong (§15.2, §15.3, §15.4):
 *
 * - **[Enumerable]** — fold over elements. A ∀ here is a *proof*.
 * - **[Sampled]** — fold over a grid on an interval of ℝ. Approximate: a ∀ here
 *   is a *search*.
 * - **[Product]** — `Domain<A> × Domain<B> → Domain<Pair<A,B>>`, for X×Y.
 * - `Opaque` — **deleted** (§15.2). It was the one case that could not answer the
 *   only question a `Domain` exists to answer. Under §15.1 an opaque `X` is
 *   simply one you have no `Domain` for, and every `Sup` is then unreachable —
 *   which is exactly what §3 asked for, with one fewer concept.
 * - `Parametric` — **deleted** (§15.3). It was a category error: [Enumerable] and
 *   [Sampled] describe *the carrier*, while `Parametric` described *the
 *   membership function* ("this is a triangle, its Sup is analytic"). Closed forms
 *   are **overrides on the function**, exactly as `TNorm.residuum` already does it
 *   — generic default, overridden where a closed form exists. Slice 2 gains no new
 *   pattern; it reuses slice 1's.
 *
 * `sealed`, so an exhaustive `when` is available and no fourth case can appear
 * from outside.
 *
 * ## Boxing
 *
 * [reduceDegrees] keeps the **accumulator and the degrees** primitive — that is
 * the path §4 protects, and the one that runs millions of times. `X` boxes in
 * [firstWhere] and [filter], which §9 permits (*"`MembershipFn<X>` may box X"*)
 * and which are bounded by their own output anyway. [Sampled] additionally avoids
 * boxing `X` inside its own loop when handed a [DoubleMembershipFn].
 */
public sealed interface Domain<X> {

    /**
     * Whether a ∀ over this domain is a **proof** or merely a **search**.
     *
     * `true` for [Enumerable] — it visits every element of `X`, so "no
     * counterexample" means there is none. `false` for [Sampled] — it visits a
     * grid, so "no counterexample" means only that none was found among the
     * points looked at. [Product] is exhaustive exactly when both factors are.
     *
     * This distinction is not cosmetic and is why §15.6 exists: over a sampled
     * domain, returning `true` from `isContainedIn` would assert a proof that was
     * never performed. The operations that quantify over `X` report a
     * counterexample instead, following §7's ethic — the same reason `fuzzy-laws`
     * reports counterexamples rather than booleans.
     */
    public val isExhaustive: Boolean

    /**
     * The elements this domain folds over, in fold order.
     *
     * For [Enumerable] these are the elements of `X`. For [Sampled] they are the
     * **grid points** — not ℝ, which no list could hold. For [Product], every
     * pair.
     *
     * The element-source primitive, and the reason it is on the interface rather
     * than being someone's private business: [Product] cannot form a pair without
     * asking a factor what is in it, and every operation that returns elements
     * rather than a number is ultimately a filter of this.
     *
     * **Materialises.** Boxing is proportional to the domain's size, which is
     * nothing for a term set and a million `Pair`s for `Product(Sampled,
     * Sampled)` at the default resolution — §15.4's wall, again. The operations
     * that matter do not call it: [reduceDegrees] never does, and [Enumerable],
     * [Sampled] and the default [firstWhere] all avoid materialising where they
     * can.
     */
    public fun elements(): List<X>

    /**
     * Folds a primitive `double` accumulator over the degrees `f(x)` for every
     * `x` in this domain.
     *
     * The one hot primitive, and everything that reduces to a number is built on
     * it — `height` is a max-fold, σ-count a sum-fold:
     *
     * ```kotlin
     * domain.reduceDegrees(f, 0.0) { acc, d -> max(acc, d) }   // Sup — Zadeh's height
     * domain.reduceDegrees(f, 0.0) { acc, d -> acc + d }       // σ-count
     * ```
     *
     * The accumulator is `double` throughout, never a boxed `R`, because this is
     * the loop CLAUDE.md §4 is about: *"the hot path is Sup over a sampled grid —
     * millions of calls. Boxing there destroys the performance story."*
     *
     * The reducer sees only degrees, never `x`. That is what lets [Sampled] run a
     * primitive loop, and it is sufficient for every aggregate in slice 2a. When
     * you need the elements themselves, use [firstWhere] or [filter].
     *
     * Iteration order is each implementation's own and is not part of the
     * contract; supply a reducer that does not care. (Every aggregate in Zadeh
     * §II–IV is commutative and associative, so none does.)
     */
    public fun reduceDegrees(fn: MembershipFn<X>, initial: Double, reducer: DegreeReducer): Double

    /**
     * The first `x` satisfying [predicate], or `null` if none was found.
     *
     * Short-circuits, which is what makes it the right primitive for
     * counterexamples: `null` from an [Enumerable] means the ∀ holds, while `null`
     * from a [Sampled] means only that no witness turned up on the grid — see
     * [isExhaustive].
     *
     * Boxes `x` per element. Sanctioned by §9, and the operations built on it —
     * containment, equality, emptiness — are not the hot path [reduceDegrees] is.
     *
     * The default scans [elements]; implementations that can short-circuit
     * without materialising override it, and all three do.
     */
    public fun firstWhere(predicate: DomainPredicate<X>): X? {
        for (x in elements()) {
            if (predicate.test(x)) return x
        }
        return null
    }

    /**
     * Every `x` satisfying [predicate], in this domain's own order.
     *
     * The primitive behind the crisp-set-valued operations: support, core, α-cut,
     * strong α-cut. Over a [Sampled] domain the result is a *sample* of the true
     * set, not the set — an α-cut of a continuous function is an interval, and
     * what comes back is the grid points inside it.
     */
    public fun filter(predicate: DomainPredicate<X>): List<X> {
        val result = ArrayList<X>()
        for (x in elements()) {
            if (predicate.test(x)) result.add(x)
        }
        return result
    }
}

/**
 * A fold step over degrees: `(accumulator, degree) → accumulator`.
 *
 * Primitive `double` in and out (§9), so [Domain.reduceDegrees] never boxes.
 *
 * Shaped exactly like [dk.eusrbin.fuzzy.algebra.TNorm] and
 * [dk.eusrbin.fuzzy.algebra.TConorm], and deliberately a **separate type** from
 * both, for the reason §9 keeps those two apart: *"the types exist to prevent
 * mixing them up."* A reducer is not a connective — `Sup` is a fold over a
 * domain, not a disjunction — and a t-conorm that wandered in here would
 * typecheck while meaning something else.
 */
public fun interface DegreeReducer {

    /** Combines the running [accumulator] with the next [degree]. */
    public fun apply(accumulator: Double, degree: Double): Double
}

/**
 * A predicate on the elements of a domain.
 *
 * Our own SAM rather than `java.util.function.Predicate` so that it carries this
 * library's KDoc and stays independent of a JDK type in a published signature.
 * Java lambdas satisfy it identically.
 */
public fun interface DomainPredicate<X> {

    /** `true` iff [x] is to be selected. */
    public fun test(x: X): Boolean
}
