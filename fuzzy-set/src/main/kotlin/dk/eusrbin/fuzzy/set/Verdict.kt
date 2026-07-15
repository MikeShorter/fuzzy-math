package dk.eusrbin.fuzzy.set

/**
 * The outcome of a **∀ over X** — containment, equality, or emptiness.
 *
 * Three values, because there are three things that can be true and a boolean
 * can hold only two. CLAUDE.md §16.4, and §15.6 before it:
 *
 * > *"Convexity quantifies over all `α ∈ (0,1]` **and** all `x` — both
 * > uncountable. Sampled, it can only ever report *'no counterexample found'*.
 * > Returning `true` asserts a proof we did not perform."*
 *
 * ## Why three and not two
 *
 * Four situations exist, and they collapse to three answers:
 *
 * | | witness found | no witness |
 * |---|---|---|
 * | [Enumerable] | [Refuted] | **[Proven]** |
 * | [Sampled] | [Refuted] | **[NotRefuted]** |
 *
 * The left column is the part people miss: **a witness found on a grid disproves
 * the claim absolutely.** Sampling is lossy in one direction only. You cannot
 * prove a ∀ by checking 1024 points — but a single counterexample settles it,
 * grid or not, so [Refuted] carries no asterisk about where it came from.
 *
 * The right column is where the domain matters, and it is the whole reason this
 * type exists. Over an [Enumerable] the fold visited every element of `X`, so "no
 * witness" means there is none: [Proven]. Over a [Sampled] it visited a grid, so
 * "no witness" means only that none turned up among the points looked at:
 * [NotRefuted]. Collapsing those two into `null` — or into `true` — states
 * something nobody checked.
 *
 * ## Reading it
 *
 * ```kotlin
 * when (val v = a.checkContainment(b, over)) {
 *     is Verdict.Proven -> "⊂, proved"
 *     is Verdict.Refuted -> "not ⊂ — witness at ${v.witness}"
 *     is Verdict.NotRefuted -> "no counterexample on this grid; try a finer one"
 * }
 * ```
 * ```java
 * // Sealed types and instanceof patterns are both final in Java 17 (§14.1),
 * // so this is idiom rather than ceremony:
 * if (v instanceof Verdict.Refuted<Double> r) { report(r.getWitness()); }
 * ```
 *
 * When the domain is an [Enumerable] and you want the boolean that is honest
 * there, the analysis operations ship one as an overload — see
 * [MembershipFn.isContainedIn]. That overload exists *because* [Enumerable] is
 * the case where a boolean tells no lie.
 *
 * ## Note what this is not
 *
 * Not a `Boolean?`, not an `Optional<X>`, and not `fuzzy-laws`'
 * [dk.eusrbin.fuzzy.laws.LawReport] — though it shares that artifact's ethic
 * (§7): a result you can act on beats a result you can only believe. It is
 * `fuzzy-set`'s own type and `fuzzy-set` depends on nothing but `fuzzy-algebra`
 * (§10).
 *
 * @param X the domain element type; [Refuted] carries one.
 */
public sealed interface Verdict<X> {

    /**
     * `true` for [Proven] only.
     *
     * The deliberately awkward accessor. It is here because a caller sometimes
     * genuinely wants "did this hold, yes or no", and forcing them to write the
     * `when` by hand would only make them write a worse one. But note what it
     * does: it maps [NotRefuted] to `false`, which is *not* what [NotRefuted]
     * means — "no counterexample found" is not "the claim is false".
     *
     * If you find yourself reaching for this on a [Sampled] domain, you want the
     * `when`. If you are on an [Enumerable], you want [MembershipFn.isContainedIn]
     * and its friends, which are honest by construction.
     */
    public val isProven: Boolean

    /** The witness, for [Refuted]; `null` otherwise. */
    public val witness: X?

    /**
     * The ∀ holds, and this is a **proof**.
     *
     * Only reachable from an exhaustive domain ([Domain.isExhaustive]) — an
     * [Enumerable], or a [Product] of exhaustive factors. The fold visited every
     * element of `X` and found no witness, so no witness exists.
     */
    public class Proven<X> : Verdict<X> {
        override val isProven: Boolean get() = true
        override val witness: X? get() = null
        override fun toString(): String = "Proven"
    }

    /**
     * The ∀ **fails**, and here is the [witness] that breaks it.
     *
     * Absolute, whatever the domain. A counterexample is a counterexample: found
     * on a 16-point grid it disproves the claim exactly as thoroughly as one found
     * by exhaustive enumeration. This is the asymmetry that makes three values
     * necessary rather than fussy — refutation is exact even where proof is not.
     *
     * @property witness the `x` at which the claim fails.
     */
    public class Refuted<X>(override val witness: X) : Verdict<X> {
        override val isProven: Boolean get() = false
        override fun toString(): String = "Refuted(witness=$witness)"
    }

    /**
     * **No counterexample was found** — which is not the same as there being none.
     *
     * The honest result of a ∀ over a non-exhaustive domain: a [Sampled] grid, or
     * a [Product] with a sampled factor. The claim may hold; it may fail between
     * two grid points. What is known is only that the points looked at did not
     * break it.
     *
     * The right response is usually to look harder — refine the grid and ask
     * again. §15.1's decision to keep the domain out of a set's type is what makes
     * that a one-line change rather than a type error:
     *
     * ```kotlin
     * a.checkContainment(b, Sampled.of(0.0, 1.0, 1_024))
     * a.checkContainment(b, Sampled.of(0.0, 1.0, 1_048_576))   // same set, finer look
     * ```
     */
    public class NotRefuted<X> : Verdict<X> {
        override val isProven: Boolean get() = false
        override val witness: X? get() = null
        override fun toString(): String = "NotRefuted"
    }

    public companion object {

        /**
         * [Proven] if the ∀ was exhaustive, [NotRefuted] otherwise — the "no
         * witness found" case, resolved against what the search can promise.
         *
         * The one place the exhaustive / [Verdict] correspondence is decided, so
         * that no analysis operation has to get it right on its own.
         *
         * The witness type `W` is **not** tied to any domain's element type
         * (CLAUDE.md §19.1). Containment's witness is an `x`, but convexity's is
         * a *triple* `(x₁, x₂, λ)` — and the domain that was searched is
         * `Domain<Double>` either way. Since nothing here reads the domain except
         * its `isExhaustive`, taking the boolean directly is both more honest and
         * more general than taking a domain we would only ask one question of.
         */
        @JvmStatic
        public fun <W> noWitness(exhaustive: Boolean): Verdict<W> =
            if (exhaustive) Proven() else NotRefuted()

        /**
         * [Refuted] if [witness] is non-null, otherwise `noWitness(exhaustive)`.
         *
         * The shape every ∀ operation in this module reduces to: search for a
         * counterexample, then let this decide what the absence of one means.
         */
        @JvmStatic
        public fun <W> of(witness: W?, exhaustive: Boolean): Verdict<W> =
            if (witness != null) Refuted(witness) else noWitness(exhaustive)

        /**
         * [noWitness] for the common case where the witness *is* a domain element
         * — containment, equality, emptiness. Sugar over `noWitness(domain.isExhaustive)`.
         */
        @JvmStatic
        public fun <X> noWitness(domain: Domain<X>): Verdict<X> = noWitness(domain.isExhaustive)

        /**
         * [of] for the common case where the witness *is* a domain element.
         * Sugar over `of(witness, domain.isExhaustive)`.
         */
        @JvmStatic
        public fun <X> of(witness: X?, domain: Domain<X>): Verdict<X> =
            of(witness, domain.isExhaustive)
    }
}
