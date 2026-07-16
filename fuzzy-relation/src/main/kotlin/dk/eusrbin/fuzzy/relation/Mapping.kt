package dk.eusrbin.fuzzy.relation

/**
 * Zadeh's `T : X → Y` — the crisp mapping that induces fuzzy sets in eqs. (22)
 * and (23).
 *
 * **Source:** Zadeh 1965, §IV p.346 — *"Let T be a mapping from X to a space
 * Y."* The mapping itself is ordinary and total; only the sets it induces are
 * fuzzy.
 *
 * Our own SAM rather than `java.util.function.Function`, for
 * [dk.eusrbin.fuzzy.set.DomainPredicate]'s reason: it carries this library's
 * KDoc and keeps a JDK type out of a published signature. Java lambdas satisfy
 * it identically:
 *
 * ```java
 * Mapping<Double, Double> square = x -> x * x;
 * ```
 *
 * (Named `Mapping` rather than Zadeh's `T` because `T` is a type parameter to
 * every JVM reader, and this library already uses it for t-norms.)
 */
public fun interface Mapping<X, Y> {

    /** The image of [x] under this mapping — Zadeh's `T(x)`. */
    public fun apply(x: X): Y
}
