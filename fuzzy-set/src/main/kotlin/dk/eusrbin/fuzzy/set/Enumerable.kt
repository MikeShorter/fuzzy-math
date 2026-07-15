package dk.eusrbin.fuzzy.set

/**
 * A [Domain] you can fold over element by element — a finite `X`, listed.
 *
 * CLAUDE.md §15.4. The straightforward case, and the only one where a ∀ over `X`
 * is a **proof** rather than a search ([isExhaustive] is `true`). Zadeh's own
 * finite examples live here: *"the class of all real numbers which are much
 * greater than 1"* is [Sampled], but a linguistic term set — `{cold, warm, hot}` —
 * is this.
 *
 * ```java
 * Domain<String> terms = Enumerable.of("cold", "warm", "hot");
 * MembershipFn<String> warm = x -> x.equals("warm") ? 1.0 : 0.3;
 * ```
 *
 * ## Duplicates and order
 *
 * Elements are held in the order given, **including duplicates** — this is a
 * *list* of points to fold over, not a mathematical set of them. That matters,
 * because it is visible in the answers: σ-count sums `f(x)` over what is
 * enumerated, so a domain listing `"warm"` twice counts it twice. The height
 * would not change; the cardinality would.
 *
 * Deduplicating silently would be the wrong favour — it would make
 * `Enumerable.of(a, a)` and `Enumerable.of(a)` behave identically while looking
 * different, and hide a caller's bug rather than surface it. Pass a `Set` if you
 * want set semantics; the constructor takes any `Collection` and will use it as
 * given.
 */
public class Enumerable<X> private constructor(
    private val elements: List<X>,
) : Domain<X> {

    /** A ∀ over an enumerable domain visits every element, so it is a proof. */
    override val isExhaustive: Boolean
        get() = true

    /** How many elements this domain folds over. */
    public val size: Int
        get() = elements.size

    /**
     * The elements, in fold order.
     *
     * Already an immutable copy taken at construction, so this hands it back
     * directly — no second copy, and nothing a caller can mutate.
     */
    override fun elements(): List<X> = elements

    override fun reduceDegrees(fn: MembershipFn<X>, initial: Double, reducer: DegreeReducer): Double {
        var accumulator = initial
        // Indexed loop over a List: no Iterator allocation, and the accumulator
        // stays a primitive double throughout (§4).
        for (i in elements.indices) {
            accumulator = reducer.apply(accumulator, fn.apply(elements[i]))
        }
        return accumulator
    }

    override fun firstWhere(predicate: DomainPredicate<X>): X? {
        for (i in elements.indices) {
            val x = elements[i]
            if (predicate.test(x)) return x
        }
        return null
    }

    override fun filter(predicate: DomainPredicate<X>): List<X> {
        val result = ArrayList<X>()
        for (i in elements.indices) {
            val x = elements[i]
            if (predicate.test(x)) result.add(x)
        }
        return result
    }

    override fun toString(): String = "Enumerable($size elements)"

    public companion object {

        /**
         * A domain over [elements], in the collection's own iteration order.
         *
         * Copied on construction, so later mutation of [elements] cannot change
         * what a domain means. Duplicates are kept — see the class KDoc.
         *
         * @throws IllegalArgumentException if [elements] is empty. An empty domain
         *   makes `height` a supremum over nothing, which is `−∞` mathematically
         *   and `0.0` by this fold's `initial` — two different lies. Rejecting it
         *   is the only honest option.
         */
        @JvmStatic
        public fun <X> of(elements: Collection<X>): Enumerable<X> {
            require(elements.isNotEmpty()) {
                "An Enumerable domain must have at least one element: a supremum over " +
                    "the empty set is not a degree in [0,1]"
            }
            // Copy, then wrap unmodifiable. The copy stops a caller's later
            // mutation from changing what a domain means; the wrapper stops a
            // Java caller casting the result of elements() back to ArrayList and
            // reaching into our state. `List.copyOf` would do both in one, but
            // rejects null elements, and nothing here needs X to be non-null.
            return Enumerable(java.util.Collections.unmodifiableList(ArrayList(elements)))
        }

        /** Vararg overload of [of], for `Enumerable.of("cold", "warm", "hot")`. */
        @JvmStatic
        public fun <X> of(vararg elements: X): Enumerable<X> = of(elements.asList())
    }
}
