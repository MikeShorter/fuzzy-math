package dk.eusrbin.fuzzy.set

/**
 * A membership function on the reals — `f_A : ℝ → [0,1]`, with **no boxing at
 * all**.
 *
 * Source: Zadeh 1965, §II. Zadeh's own examples are overwhelmingly on ℝ (§V's
 * Figures 1, 4 and 5; the "class of real numbers much greater than 1", p.339),
 * which is why §9 singles this case out:
 *
 * > *"Provide a `DoubleMembershipFn` specialisation for `X = ℝ` (Zadeh's common
 * > case) to avoid boxing the argument too."*
 *
 * ## Why a second type rather than `MembershipFn<Double>`
 *
 * `MembershipFn<Double>` boxes its argument: `X` is a generic parameter, erased
 * to `Object`, so every call allocates a `java.lang.Double`. On the path CLAUDE.md
 * §4 cares about — a supremum over a [Sampled] grid, "millions of calls" — that
 * is millions of allocations to hand the JIT.
 *
 * `applyAsDouble` takes a primitive because it is **not** an override of a generic
 * method: its signature is `(D)D` outright. That is the guarantee, and it is why
 * the primitive entry point carries its own name rather than reusing [apply].
 * The name follows `java.util.function.DoubleUnaryOperator.applyAsDouble`, so it
 * reads as idiom rather than invention from Java.
 *
 * ## And yet it *is* a `MembershipFn<Double>`
 *
 * Deliberately a subtype, unlike the JDK's `DoubleUnaryOperator` (which is not a
 * `Function<Double,Double>`, because Java cannot express the bridge). Kotlin can,
 * so every domain-generic operation — [Domain], height, α-cuts, containment —
 * accepts a `DoubleMembershipFn` with no adapter. There is one API, not two.
 *
 * The cost is that reaching it *through* `MembershipFn<Double>` boxes again, via
 * the inherited [apply]. That is unavoidable and correct: the boxing is the price
 * of the generic call, not of this type. Code on the hot path takes the primitive
 * road deliberately — [Sampled] checks for this type once, outside its loop,
 * rather than per element.
 *
 * ```java
 * DoubleMembershipFn nearZero = x -> Math.exp(-x * x);
 * nearZero.applyAsDouble(0.5);              // primitive: no allocation
 * nearZero.height(Sampled.of(-3, 3, 1024)); // MembershipFn<Double>: works
 * ```
 * ```clojure
 * (reify DoubleMembershipFn (applyAsDouble [_ x] (Math/exp (- (* x x)))))
 * ```
 *
 * ## Preconditions
 *
 * Must return a value in `[0,1]`; unchecked, per [MembershipFn].
 */
public fun interface DoubleMembershipFn : MembershipFn<Double> {

    /**
     * The grade of membership of [x] — `f_A(x)` — taking and returning a
     * primitive `double`.
     *
     * The single abstract method: a lambda you write implements *this*, and
     * [apply] is supplied for you.
     */
    public fun applyAsDouble(x: Double): Double

    /**
     * The boxed [MembershipFn] contract, satisfied by delegating to
     * [applyAsDouble].
     *
     * Present so that a `DoubleMembershipFn` is usable wherever a
     * `MembershipFn<Double>` is wanted. Prefer [applyAsDouble] when the static
     * type is known — this one boxes, that one does not.
     */
    override fun apply(x: Double): Double = applyAsDouble(x)

    /** Kotlin sugar for [applyAsDouble], letting you write `A(x)` with no boxing. */
    override operator fun invoke(x: Double): Double = applyAsDouble(x)
}
