package dk.eusrbin.fuzzy.algebra

/**
 * The three implication families, and the named members worth naming.
 *
 * Source: Bergmann 2008, §11.7 (implication), §12 (residuated lattices).
 * See [Implication] for the family table and for why Zadeh 1965 has none of this.
 *
 * CLAUDE.md §5 governs the shape of this object: *"R-implications are derived;
 * S-implications (`N(x) S y`) and QL-implications (`S(N(x), T(x,y))`) are
 * provided as separate named families."* So [residuum] is a thin accessor onto
 * [TNorm.residuum] — the computation lives on the t-norm, where it cannot be
 * detached from it — while [sImplication] and [qlImplication] are genuine
 * constructions taking their operators as arguments.
 */
public object Implications {

    /**
     * The **R-implication** (residuum) of [tNorm]: `sup { z | T(a,z) ≤ b }`.
     *
     * Adapts [TNorm.residuum] to the [Implication] type. It computes nothing
     * itself — CLAUDE.md §5 puts the residuum on the t-norm precisely so that it
     * cannot be supplied independently of one.
     *
     * Requires [tNorm] to be left-continuous for the adjunction to hold; see
     * [TNorm.residuum] and [TNorms.DRASTIC].
     */
    @JvmStatic
    public fun residuum(tNorm: TNorm): Implication =
        Implication { a, b -> tNorm.residuum(a, b) }.named("Implication.residuum($tNorm)")

    /**
     * The **S-implication** (strong / KD-implication) from [tConorm] and
     * [negation]:
     *
     * ```
     * I(a, b) = S(N(a), b)
     * ```
     *
     * Source: Bergmann 2008, §11.7.
     *
     * This is the direct fuzzification of the classical tautology
     * `a → b ≡ ¬a ∨ b`. It needs an involutive [negation] to behave; see
     * [Negation].
     *
     * It is **not** in general the residuum of the dual t-norm. It coincides for
     * Łukasiewicz and diverges for Gödel — compare [GODEL] with [KLEENE_DIENES],
     * which are the R- and S-implications of the same algebra and are different
     * functions.
     */
    @JvmStatic
    public fun sImplication(tConorm: TConorm, negation: Negation): Implication =
        Implication { a, b -> tConorm.apply(negation.apply(a), b) }
            .named("Implication.S($tConorm, $negation)")

    /**
     * The **QL-implication** (quantum logic) from [tConorm], [tNorm] and
     * [negation]:
     *
     * ```
     * I(a, b) = S(N(a), T(a, b))
     * ```
     *
     * Source: Bergmann 2008, §11.7.
     *
     * The fuzzification of `a → b ≡ ¬a ∨ (a ∧ b)` — classically the same
     * tautology as the S-family's, and fuzzily a different function. That the
     * two diverge is the whole reason both families are named rather than one
     * being derived from the other.
     *
     * QL-implications are not always genuine fuzzy implications: monotonicity in
     * the first argument can fail depending on the triple. Check yours with
     * `fuzzy-laws` rather than assuming.
     */
    @JvmStatic
    public fun qlImplication(tConorm: TConorm, tNorm: TNorm, negation: Negation): Implication =
        Implication { a, b -> tConorm.apply(negation.apply(a), tNorm.apply(a, b)) }
            .named("Implication.QL($tConorm, $tNorm, $negation)")

    /**
     * The **biresiduum** of [tNorm] — fuzzy biconditional, `a ⇔ b`:
     *
     * ```
     * a ⇔ b = T(a ⇒ b, b ⇒ a)
     * ```
     *
     * Source: Bergmann 2008, §12 (residuated lattices, MV-algebras).
     *
     * The natural equality-degree of a fuzzy logic: `1` exactly when `a = b`
     * (for a left-continuous [tNorm]), decreasing as they diverge. For
     * [TNorms.LUKASIEWICZ] it is `1 − |a − b|` — the algebra whose biresiduum is
     * a metric, which is why Łukasiewicz is the one with an MV-algebra
     * (CLAUDE.md §7).
     */
    @JvmStatic
    public fun biresiduum(tNorm: TNorm): Implication =
        Implication { a, b -> tNorm.apply(tNorm.residuum(a, b), tNorm.residuum(b, a)) }
            .named("Implication.biresiduum($tNorm)")

    // ---- Named R-implications (residua of the basis) ------------------------

    /**
     * The **Gödel** implication: `I(a,b) = 1 if a ≤ b, else b`.
     * The residuum of [TNorms.GODEL]. Bergmann 2008, §11.8.
     */
    @JvmField
    public val GODEL: Implication = residuum(TNorms.GODEL).named("Implication.Godel")

    /**
     * The **Goguen** implication: `I(a,b) = 1 if a ≤ b, else b/a`.
     * The residuum of [TNorms.PRODUCT]. Bergmann 2008, §11.9.
     */
    @JvmField
    public val GOGUEN: Implication = residuum(TNorms.PRODUCT).named("Implication.Goguen")

    /**
     * The **Łukasiewicz** implication: `I(a,b) = min(1, 1 − a + b)`.
     * Bergmann 2008, §11.2.
     *
     * Unusually, both the R-implication of [TNorms.LUKASIEWICZ] *and* the
     * S-implication of ([TConorms.LUKASIEWICZ], [Negations.STANDARD]) — the two
     * families coincide here. That is a fact about Łukasiewicz, not a general
     * licence to treat R- and S-implications as interchangeable ([Implication]).
     */
    @JvmField
    public val LUKASIEWICZ: Implication = residuum(TNorms.LUKASIEWICZ).named("Implication.Lukasiewicz")

    // ---- Named S-implications ----------------------------------------------

    /**
     * The **Kleene–Dienes** implication: `I(a,b) = max(1 − a, b)`.
     *
     * The S-implication of ([TConorms.GODEL], [Negations.STANDARD]).
     * Bergmann 2008, §11.7.
     *
     * Worth comparing against [GODEL], the *R*-implication of the same algebra:
     * at `a = b = 0.5`, Kleene–Dienes gives `0.5` while Gödel gives `1`. Same
     * algebra, same classical limit, different fuzzy answer.
     */
    @JvmField
    public val KLEENE_DIENES: Implication =
        sImplication(TConorms.GODEL, Negations.STANDARD).named("Implication.KleeneDienes")

    /**
     * The **Reichenbach** implication: `I(a,b) = 1 − a + a·b`.
     *
     * The S-implication of ([TConorms.PROBABILISTIC_SUM], [Negations.STANDARD]).
     * Bergmann 2008, §11.7.
     */
    @JvmField
    public val REICHENBACH: Implication =
        sImplication(TConorms.PROBABILISTIC_SUM, Negations.STANDARD).named("Implication.Reichenbach")

    // ---- Named QL-implications ---------------------------------------------

    /**
     * The **Zadeh** implication: `I(a,b) = max(1 − a, min(a, b))`.
     *
     * The QL-implication of ([TConorms.GODEL], [TNorms.GODEL],
     * [Negations.STANDARD]).
     *
     * Named for Zadeh because it is the implication of his later compositional
     * rule of inference — **not** because it appears in Zadeh 1965, which has no
     * implication at all (CLAUDE.md §5, [Implication]). CRI itself belongs to
     * `fuzzy-relation` (CLAUDE.md §10), not here.
     *
     * A cautionary specimen: it is **not** monotone non-increasing in its first
     * argument (fix `b = 0`; then `I(a,0) = max(1−a, 0) = 1−a` is fine, but fix
     * `b = 1` and `I(a,1) = max(1−a, a)` is V-shaped, rising after `a = 0.5`).
     * So it fails the defining property of a fuzzy implication while still being
     * a useful and widely used function.
     */
    @JvmField
    public val ZADEH: Implication =
        qlImplication(TConorms.GODEL, TNorms.GODEL, Negations.STANDARD).named("Implication.Zadeh")
}

/**
 * Attaches a readable [toString] to a SAM-constructed [Implication].
 * See the equivalent in `TConorms.kt` for why.
 */
private fun Implication.named(name: String): Implication = object : Implication {
    override fun apply(a: Double, b: Double): Double = this@named.apply(a, b)

    override fun toString(): String = name
}
