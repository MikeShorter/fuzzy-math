package dk.eusrbin.fuzzy.algebra

import kotlin.math.max
import kotlin.math.min

/**
 * The named t-conorms — the [Negations.STANDARD]-duals of [TNorms].
 *
 * Sources: Zadeh 1965 §II (union, eq. 3) and §IV + footnote 4 (algebraic sum);
 * Bergmann 2008 §11.7; Klement, Mesiar & Pap 2000.
 *
 * Each entry here is the dual of the same-named t-norm under `N(x) = 1 − x`:
 *
 * ```
 * S(a, b) = 1 − T(1 − a, 1 − b)
 * ```
 *
 * so the pairings ([GODEL]/[TNorms.GODEL], [PROBABILISTIC_SUM]/[TNorms.PRODUCT],
 * [LUKASIEWICZ]/[TNorms.LUKASIEWICZ]) satisfy De Morgan by construction. The
 * closed forms below are what that formula simplifies to; they are not
 * independent choices. [dk.eusrbin.fuzzy.laws.DeMorganLaws] checks this rather
 * than trusting it.
 *
 * If you are pairing a t-norm with a conorm yourself, prefer [dualOf] or
 * [Algebra.deMorgan] over picking two names off a list — CLAUDE.md §6: "Mixing
 * an arbitrary t-norm with an arbitrary conorm silently breaks De Morgan; that
 * footgun is not the default."
 */
public object TConorms {

    /**
     * The **Gödel** conorm, `S(a,b) = max(a,b)`: the least t-conorm, dual to
     * [TNorms.GODEL].
     *
     * Source: Zadeh 1965, §II eq. (3), p.340 — his *union*,
     * `f_C(x) = Max[f_A(x), f_B(x)]`. Abbreviated `f_C = f_A ∨ f_B` at eq. (4).
     *
     * The only idempotent t-conorm, and half of the only pair for which
     * distributivity and absorption hold (CLAUDE.md §7,
     * [dk.eusrbin.fuzzy.laws.StandardLaws]).
     */
    @JvmField
    public val GODEL: TConorm = TConorm { a, b -> max(a, b) }.named("TConorm.Godel(max)")

    /** Alias for [GODEL], under the name Zadeh 1965 §II uses. Same instance. */
    @JvmField
    public val MAXIMUM: TConorm = GODEL

    /**
     * The **probabilistic sum**, `S(a,b) = a + b − a·b`: dual to [TNorms.PRODUCT].
     *
     * Source: Zadeh 1965, §IV footnote 4 — where it appears as the algebraic sum
     * variant `f_A + f_B − f_A f_B`, filed as a separate curiosity.
     *
     * CLAUDE.md §6 retroactively unifies this with the rest: footnote 4's dual
     * "*is* its conorm — he filed them separately without noticing they were the
     * same shape."
     */
    @JvmField
    public val PROBABILISTIC_SUM: TConorm =
        TConorm { a, b -> a + b - a * b }.named("TConorm.ProbabilisticSum")

    /** Alias for [PROBABILISTIC_SUM], under Goguen's name. Same instance. */
    @JvmField
    public val GOGUEN: TConorm = PROBABILISTIC_SUM

    /**
     * The **Łukasiewicz** conorm (bounded sum), `S(a,b) = min(1, a + b)`: dual to
     * [TNorms.LUKASIEWICZ].
     *
     * Source: Bergmann 2008, §11.2; §12 for its role as MV-algebraic `⊕`.
     *
     * This is Zadeh's §IV *algebraic sum* with its cap restored. CLAUDE.md §6:
     * his `f_A + f_B` "side-condition `f_A + f_B ≤ 1` is an uncapped Łukasiewicz
     * conorm missing its `min(1, ·)`" — the side-condition exists to keep the
     * result inside `[0,1]`, which is precisely what `min(1, ·)` does
     * unconditionally.
     *
     * The `⊕` of [dk.eusrbin.fuzzy.laws.MVAlgebraLaws].
     */
    @JvmField
    public val LUKASIEWICZ: TConorm = TConorm { a, b -> min(1.0, a + b) }.named("TConorm.Lukasiewicz")

    /**
     * The **Drastic** sum: the *greatest* t-conorm, dual to [TNorms.DRASTIC].
     *
     * ```
     * S(a,b) = b     if a = 0
     *        = a     if b = 0
     *        = 1     otherwise
     * ```
     *
     * **Attributed:** classical; the greatest t-conorm. Not on hand — see
     * CLAUDE.md §2 and §17.5.
     */
    @JvmField
    public val DRASTIC: TConorm = object : TConorm {
        override fun apply(a: Double, b: Double): Double = when {
            a == 0.0 -> b
            b == 0.0 -> a
            else -> 1.0
        }

        override fun toString(): String = "TConorm.Drastic"
    }

    /**
     * The **nilpotent maximum**, `S(a,b) = max(a,b) if a + b < 1, else 1`: dual
     * to [TNorms.NILPOTENT_MINIMUM].
     *
     * **Attributed:** Fodor (1995). Not on hand — CLAUDE.md §17.5.
     */
    @JvmField
    public val NILPOTENT_MAXIMUM: TConorm =
        TConorm { a, b -> if (a + b < 1.0) max(a, b) else 1.0 }.named("TConorm.NilpotentMaximum")

    /** The [Negations.STANDARD]-duals of [TNorms.CONTINUOUS_BASIS], in the same order. */
    @JvmField
    public val CONTINUOUS_BASIS: List<TConorm> = listOf(LUKASIEWICZ, GODEL, PROBABILISTIC_SUM)

    /**
     * The **Hamacher** conorm family, dual to [TNorms.hamacher] under
     * [Negations.STANDARD], parameterised by `γ ≥ 0`:
     *
     * ```
     * S_γ(a,b) = (a + b − a·b − (1 − γ)·a·b) / (1 − (1 − γ)·a·b)
     * ```
     *
     * **Attributed:** Hamacher (1978). Not on hand — CLAUDE.md §17.5.
     *
     * `γ = 1` is [PROBABILISTIC_SUM] exactly; `γ = 2` is the Einstein sum.
     *
     * Implemented as `dualOf(TNorms.hamacher(γ), Negations.STANDARD)` rather
     * than by transcribing the closed form: the duality is the definition, so
     * deriving it removes any chance of the two drifting apart. The cost is two
     * subtractions.
     *
     * @param gamma the parameter `γ`, which must be `≥ 0`.
     * @throws IllegalArgumentException if [gamma] is negative or `NaN`.
     */
    @JvmStatic
    public fun hamacher(gamma: Double): TConorm =
        if (gamma == 1.0) {
            PROBABILISTIC_SUM
        } else {
            dualOf(TNorms.hamacher(gamma), Negations.STANDARD)
                .named("TConorm.Hamacher(γ=$gamma)")
        }

    /**
     * The **De Morgan dual** of [tNorm] with respect to [negation]:
     *
     * ```
     * S(a, b) = N(T(N(a), N(b)))
     * ```
     *
     * Source: Zadeh 1965 §III eqs. (7), (8) — De Morgan's laws for fuzzy sets;
     * Bergmann 2008, §11.7.
     *
     * This is the safe way to obtain a conorm: the resulting `(T, S, N)` triple
     * satisfies De Morgan **by construction**, for any [tNorm] and any
     * *involutive* [negation]. Contrast picking a `T` and an `S` off the list
     * independently, which CLAUDE.md §6 names as the footgun this library
     * declines to make default.
     *
     * Involutivity of [negation] is required and **not checked** here — it is a
     * property of a function, not of a value, so it cannot be checked at
     * construction, only sampled. That is exactly what
     * [dk.eusrbin.fuzzy.laws.DeMorganLaws] is for. Every negation in [Negations]
     * is involutive.
     *
     * @see Algebra.deMorgan for the bundled version, which is the front door.
     */
    @JvmStatic
    public fun dualOf(tNorm: TNorm, negation: Negation): TConorm =
        TConorm { a, b -> negation.apply(tNorm.apply(negation.apply(a), negation.apply(b))) }
            .named("TConorm.dualOf($tNorm, $negation)")

    /**
     * The **De Morgan dual** of [tConorm] with respect to [negation]:
     * `T(a,b) = N(S(N(a), N(b)))`. The inverse of [dualOf], and subject to the
     * same involutivity requirement.
     *
     * The resulting `TNorm` inherits [TNorm]'s generic bisection residuum — it
     * cannot know a closed form for a norm it has just derived.
     */
    @JvmStatic
    public fun toTNorm(tConorm: TConorm, negation: Negation): TNorm =
        TNorm { a, b -> negation.apply(tConorm.apply(negation.apply(a), negation.apply(b))) }
}

/**
 * Attaches a readable [toString] to a SAM-constructed [TConorm].
 *
 * Lambdas stringify as `TConorms$$Lambda$14/0x…`, which is worthless in a
 * [dk.eusrbin.fuzzy.laws.LawReport] — the report names the subject it failed on,
 * so the subject needs a name. Private, and applied only to the values above.
 */
private fun TConorm.named(name: String): TConorm = object : TConorm {
    override fun apply(a: Double, b: Double): Double = this@named.apply(a, b)

    override fun toString(): String = name
}
