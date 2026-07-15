package dk.eusrbin.fuzzy.algebra

/**
 * A named bundle of `(T, S, N, ⇒)` — **the front door of this library**.
 *
 * Source: Bergmann 2008, §11.2 / §11.7 / §11.8 / §11.9 for the three named
 * systems; §12 for what makes them residuated lattices, BL- and MV-algebras.
 *
 * ## Why a bundle at all
 *
 * CLAUDE.md §6: *"the front door is a named `Algebra` bundling `(T, S, N, ⇒)` so
 * duality cannot be broken by accident. Mixing an arbitrary t-norm with an
 * arbitrary conorm silently breaks De Morgan; that footgun is not the default.
 * Raw t-norm parameters remain available as an escape hatch."*
 *
 * The failure mode being designed against is quiet, not loud. `TNorms.PRODUCT`
 * with `TConorms.GODEL` compiles, runs, returns plausible numbers in `[0,1]`,
 * and satisfies no De Morgan law. Nothing tells you. The bundle makes the
 * correct pairing the path of least resistance:
 *
 * - the three named constants are correct triples;
 * - [deMorgan] *derives* `S` from `(T, N)`, so it cannot be wrong;
 * - [of] lets you supply all three independently, and says so in its own KDoc.
 *
 * ## The implication is not a parameter
 *
 * There is no constructor here that takes an [Implication]. CLAUDE.md §5:
 * *"for a left-continuous t-norm the residuum is determined. The API computes
 * it; it is not a free parameter of `Algebra`."* [implication] is always
 * `tNorm`'s residuum. If you want an S- or QL-implication, build one explicitly
 * from [Implications] — it will be visibly a different object, which is the
 * intent.
 *
 * ## Usage
 *
 * ```java
 * Algebra a = Algebra.LUKASIEWICZ;
 * double conj = a.and(0.7, 0.6);       // 0.3
 * double impl = a.implies(0.7, 0.6);   // 0.9
 * ```
 * ```kotlin
 * with(Algebra.PRODUCT) { and(0.7, 0.6) }   // 0.42
 * ```
 *
 * @property name a human-readable name, used by `fuzzy-laws` when reporting.
 * @property tNorm the conjunction / intersection.
 * @property tConorm the disjunction / union.
 * @property negation the negation / complement.
 */
public class Algebra private constructor(
    public val name: String,
    public val tNorm: TNorm,
    public val tConorm: TConorm,
    public val negation: Negation,
) {

    /**
     * The residuum of [tNorm] — **derived, never supplied** (CLAUDE.md §5).
     *
     * @see TNorm.residuum
     */
    public val implication: Implication = Implications.residuum(tNorm)

    /**
     * The biresiduum of [tNorm]: `a ⇔ b = T(a ⇒ b, b ⇒ a)`.
     *
     * @see Implications.biresiduum
     */
    public val biresiduum: Implication = Implications.biresiduum(tNorm)

    /** `T(a, b)`. Named method for [TNorm.apply]; arguments must be in `[0,1]`. */
    public fun and(a: Double, b: Double): Double = tNorm.apply(a, b)

    /** `S(a, b)`. Named method for [TConorm.apply]; arguments must be in `[0,1]`. */
    public fun or(a: Double, b: Double): Double = tConorm.apply(a, b)

    /** `N(a)`. Named method for [Negation.apply]; argument must be in `[0,1]`. */
    public fun not(a: Double): Double = negation.apply(a)

    /** `a ⇒ b`, the residuum of [tNorm]. Arguments must be in `[0,1]`. */
    public fun implies(a: Double, b: Double): Double = implication.apply(a, b)

    /** `a ⇔ b`, the biresiduum of [tNorm]. Arguments must be in `[0,1]`. */
    public fun iff(a: Double, b: Double): Double = biresiduum.apply(a, b)

    override fun toString(): String = "Algebra.$name"

    public companion object {

        /**
         * The **Standard** algebra: `(min, max, 1 − x)`.
         *
         * Aliases, all naming this same object (CLAUDE.md §6):
         * - **Zadeh** — it is exactly Zadeh 1965 §II: complement `1 − f_A`
         *   (eq. 1), union `Max[f_A,f_B]` (eq. 3), intersection `Min[f_A,f_B]`
         *   (eq. 5).
         * - **Gödel** — it is the Gödel system of many-valued logic
         *   (Bergmann 2008, §11.8).
         *
         * That these are the same object is CLAUDE.md §6's central claim: Zadeh's
         * algebra is not a special mechanism, it is the default instantiation of
         * the one parameterised mechanism.
         *
         * The **only** algebra satisfying [dk.eusrbin.fuzzy.laws.StandardLaws] —
         * idempotence, distributivity (Zadeh eqs. 9, 10) and absorption hold here
         * and nowhere else (CLAUDE.md §7).
         *
         * Its implication is the Gödel residuum, *not* Kleene–Dienes
         * ([Implications.GODEL] vs [Implications.KLEENE_DIENES]) — §5 again.
         */
        @JvmField
        public val STANDARD: Algebra =
            Algebra("Standard", TNorms.GODEL, TConorms.GODEL, Negations.STANDARD)

        /** Alias for [STANDARD], under Zadeh's name. Same instance. */
        @JvmField
        public val ZADEH: Algebra = STANDARD

        /** Alias for [STANDARD], under Gödel's name. Same instance. */
        @JvmField
        public val GODEL: Algebra = STANDARD

        /**
         * The **Product** (Goguen) algebra: `(a·b, a + b − a·b, 1 − x)`.
         *
         * Sources: Zadeh 1965 §IV (algebraic product) + footnote 4 (its dual);
         * Bergmann 2008, §11.9.
         *
         * A BL-algebra ([dk.eusrbin.fuzzy.laws.BLAlgebraLaws]) but not an
         * MV-algebra, and emphatically **not** a distributive lattice —
         * `StandardLaws.check(Algebra.PRODUCT)` fails, by design and by
         * mathematics. CLAUDE.md §7 requires that failure be asserted as a test
         * of the test suite itself.
         */
        @JvmField
        public val PRODUCT: Algebra =
            Algebra("Product", TNorms.PRODUCT, TConorms.PROBABILISTIC_SUM, Negations.STANDARD)

        /** Alias for [PRODUCT], under Goguen's name. Same instance. */
        @JvmField
        public val GOGUEN: Algebra = PRODUCT

        /**
         * The **Łukasiewicz** algebra:
         * `(max(0, a+b−1), min(1, a+b), 1 − x)`.
         *
         * Source: Bergmann 2008, §11.2; §12 for MV-algebras.
         *
         * The one algebra satisfying [dk.eusrbin.fuzzy.laws.MVAlgebraLaws]
         * (CLAUDE.md §7). Its conorm is Zadeh's §IV algebraic sum with the
         * `min(1, ·)` he wrote as a side-condition (CLAUDE.md §6).
         */
        @JvmField
        public val LUKASIEWICZ: Algebra =
            Algebra("Lukasiewicz", TNorms.LUKASIEWICZ, TConorms.LUKASIEWICZ, Negations.STANDARD)

        /**
         * The three named algebras: [STANDARD], [PRODUCT], [LUKASIEWICZ].
         *
         * These are the algebras of the three fundamental continuous t-norms
         * (**Mostert–Shields**, [TNorms.CONTINUOUS_BASIS]). Useful for property
         * tests that want to range over "the built-ins" — `fuzzy-laws`' own
         * suite does exactly that.
         */
        @JvmField
        public val BUILT_INS: List<Algebra> = listOf(STANDARD, PRODUCT, LUKASIEWICZ)

        /**
         * Builds an algebra from [tNorm] and [negation], **deriving** the conorm
         * as the De Morgan dual `S(a,b) = N(T(N(a), N(b)))`.
         *
         * This is the safe constructor, and the one to reach for when
         * instantiating a t-norm you built with [TNorms.hamacher] or
         * [TNorms.ordinalSum]. The resulting triple satisfies De Morgan by
         * construction for any involutive [negation] — you cannot get the pairing
         * wrong, because you are not asked for it.
         *
         * ```kotlin
         * val einstein = Algebra.deMorgan("Einstein", TNorms.hamacher(2.0), Negations.STANDARD)
         * ```
         *
         * Involutivity of [negation] is required and unchecked (it is a property
         * of a function, not of a value). [dk.eusrbin.fuzzy.laws.DeMorganLaws]
         * samples it.
         *
         * @see TConorms.dualOf
         */
        @JvmStatic
        public fun deMorgan(name: String, tNorm: TNorm, negation: Negation): Algebra =
            Algebra(name, tNorm, TConorms.dualOf(tNorm, negation), negation)

        /**
         * Builds an algebra from three independently supplied operators.
         *
         * **This is the escape hatch, and it is sharp.** CLAUDE.md §6 keeps it
         * available deliberately — "raw t-norm parameters remain available as an
         * escape hatch" — while making clear it is not the front door.
         *
         * Nothing here checks that [tConorm] is the [negation]-dual of [tNorm].
         * If it is not, De Morgan fails silently: every result stays in `[0,1]`
         * and looks reasonable. Prefer [deMorgan], which cannot be wrong.
         *
         * Legitimate uses: reconstructing an algebra from the literature whose
         * conorm is stated independently; deliberately exploring a non-dual
         * triple. In both cases run [dk.eusrbin.fuzzy.laws.DeMorganLaws] against
         * the result and find out where you stand rather than assuming.
         */
        @JvmStatic
        public fun of(name: String, tNorm: TNorm, tConorm: TConorm, negation: Negation): Algebra =
            Algebra(name, tNorm, tConorm, negation)
    }
}
