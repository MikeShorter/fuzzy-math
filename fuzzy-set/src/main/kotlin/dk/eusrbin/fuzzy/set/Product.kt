package dk.eusrbin.fuzzy.set

/**
 * The Cartesian product of two domains — `Domain<A> × Domain<B>`, folding over
 * `Pair<A, B>`.
 *
 * CLAUDE.md §15.4. Present in slice 2a because it **earns itself twice later**:
 * `fuzzy-relation` (§10) is defined on X×Y and needs exactly this for sup-T
 * composition, `Sup_v Min[f_A(u,v), f_B(v,w)]` (Zadeh p.346), and for the
 * extension principle (eq. 23). It costs one class now and unblocks a module
 * later.
 *
 * ```java
 * Domain<Pair<String, String>> pairs =
 *     Product.of(Enumerable.of("cold", "warm"), Enumerable.of("low", "high"));
 * ```
 *
 * ## Exhaustive exactly when both factors are
 *
 * A ∀ over `A × B` is a proof iff it is a proof over each factor, so
 * [isExhaustive] is the conjunction. `Product(Enumerable, Enumerable)` is a
 * genuine ∀ — that is the intended case. `Product(Sampled, ...)` is not, and the
 * result is honest about it.
 *
 * Note this is a **runtime** property, not a type-level one: the exhaustiveness
 * of a product depends on the domains handed to it, which no type parameter here
 * can know. That is §15.1's lesson applied to itself — capability belongs in a
 * value you must supply, not in a phantom type. See [Domain.isExhaustive] and the
 * containment operations for how it is consumed.
 *
 * ## Cost — read §15.4's wall before reaching for this
 *
 * A fold costs `|A| × |B|` evaluations. For two [Enumerable] term sets that is
 * nothing. For two [Sampled] grids at the default 1024 points it is **over a
 * million** evaluations per fold, and a third factor would make it a billion.
 * Grid sampling is a one-dimensional technique; this class does not change that,
 * and `Product(Sampled, Sampled)` compiles precisely because refusing to nest a
 * legal construction would be a lie of a different kind. It is documented as a
 * wall (§15.4), not fenced off.
 *
 * @property first the left factor.
 * @property second the right factor.
 */
public class Product<A, B> private constructor(
    public val first: Domain<A>,
    public val second: Domain<B>,
) : Domain<Pair<A, B>> {

    /** A ∀ over the product is a proof iff it is a proof over both factors. */
    override val isExhaustive: Boolean
        get() = first.isExhaustive && second.isExhaustive

    /**
     * Every `(a, b)`, [first] varying slowest.
     *
     * `|A| × |B|` pairs, all of them allocated. This is the method §15.4's wall
     * is about — see the class KDoc before calling it on two grids.
     */
    override fun elements(): List<Pair<A, B>> {
        val lefts = first.elements()
        val rights = second.elements()
        val result = ArrayList<Pair<A, B>>(lefts.size * rights.size)
        for (a in lefts) {
            for (b in rights) result.add(a to b)
        }
        return result
    }

    /**
     * Folds over every `(a, b)`, [first] varying slowest.
     *
     * Deliberately **not** `elements().fold(...)`. The outer factor's elements are
     * materialised once — `O(|A|)` — and the inner factor's own [reduceDegrees]
     * is then threaded the running accumulator, so the product's pairs are never
     * collected and the accumulator stays a primitive `double` across both levels.
     * Folding [elements] instead would allocate the whole `|A| × |B|` product to
     * produce one number.
     *
     * `Pair` allocation per point is unavoidable — the function being folded asks
     * for one — and is why §15.4 wants this for [Enumerable] factors rather than
     * [Sampled] ones. Note the inner call is [second]'s own `reduceDegrees`, so if
     * `second` is a [Sampled] it still takes its primitive path internally; only
     * the pair boxing is forced.
     */
    override fun reduceDegrees(
        fn: MembershipFn<Pair<A, B>>,
        initial: Double,
        reducer: DegreeReducer,
    ): Double {
        var accumulator = initial
        for (a in first.elements()) {
            accumulator = second.reduceDegrees(
                MembershipFn { b -> fn.apply(a to b) },
                accumulator,
                reducer,
            )
        }
        return accumulator
    }

    /**
     * The first `(a, b)` satisfying [predicate], or `null`.
     *
     * Short-circuits on both axes and materialises neither the product nor the
     * inner factor: the outer factor's elements are walked, and for each, the
     * inner factor's own short-circuiting [firstWhere] is asked for a partner.
     */
    override fun firstWhere(predicate: DomainPredicate<Pair<A, B>>): Pair<A, B>? {
        for (a in first.elements()) {
            val b = second.firstWhere { b -> predicate.test(a to b) }
            if (b != null) return a to b
        }
        return null
    }

    /**
     * Every `(a, b)` satisfying [predicate].
     *
     * Allocates only the pairs that match, plus the outer factor's elements —
     * the full product is never built.
     */
    override fun filter(predicate: DomainPredicate<Pair<A, B>>): List<Pair<A, B>> {
        val result = ArrayList<Pair<A, B>>()
        for (a in first.elements()) {
            for (b in second.filter { b -> predicate.test(a to b) }) {
                result.add(a to b)
            }
        }
        return result
    }

    override fun toString(): String = "Product($first × $second)"

    public companion object {

        /** The product of [first] and [second]. */
        @JvmStatic
        public fun <A, B> of(first: Domain<A>, second: Domain<B>): Product<A, B> =
            Product(first, second)
    }
}
