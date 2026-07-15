package dk.eusrbin.fuzzy.laws

import dk.eusrbin.fuzzy.algebra.Algebra

/**
 * **The MV tier.** Chang's MV-algebra axioms — satisfied by
 * [Algebra.LUKASIEWICZ] and by no other built-in.
 *
 * Source: Bergmann 2008, §12 (MV-algebras). Chang (1958) for the axioms.
 *
 * An MV-algebra is `⟨A, ⊕, ¬, 0⟩` satisfying:
 *
 * ```
 * (MV1)  a ⊕ b = b ⊕ a
 * (MV2)  (a ⊕ b) ⊕ c = a ⊕ (b ⊕ c)
 * (MV3)  a ⊕ 0 = a
 * (MV4)  a ⊕ 1 = 1
 * (MV5)  ¬¬a = a
 * (MV6)  ¬0 = 1
 * (MV7)  a ⊕ ¬a = 1                                    excluded middle
 * (MV8)  ¬(¬a ⊕ b) ⊕ b = ¬(¬b ⊕ a) ⊕ a                 Chang's axiom
 * ```
 *
 * Here `⊕` is [Algebra.tConorm] and `¬` is [Algebra.negation]. For
 * [Algebra.LUKASIEWICZ] those are `min(1, a+b)` and `1 − a`, which is the
 * standard MV-algebra on `[0,1]`.
 *
 * CLAUDE.md §7's fourth tier, whose "Holds for" column reads, in full,
 * *"Łukasiewicz"*.
 *
 * ## The two axioms that do the separating
 *
 * MV1–MV6 are unremarkable: every built-in algebra satisfies them, because they
 * amount to "the conorm is a conorm and the negation is strong". The tier is
 * earned by the last two.
 *
 * **MV7, excluded middle**, `a ⊕ ¬a = 1`, is the one people find surprising —
 * fuzzy logic is supposed to *reject* excluded middle. It holds here because
 * Łukasiewicz's conorm is nilpotent: `min(1, a + (1−a)) = 1` exactly. It fails
 * for the others in the obvious place: at `a = 0.5`, Gödel gives
 * `max(0.5, 0.5) = 0.5` and Product gives `0.75`. So Zadeh's own algebra is
 * *not* an MV-algebra, and Łukasiewicz — the one that keeps excluded middle — is.
 *
 * **MV8, Chang's axiom**, is the characteristic one. Both sides compute
 * `max(a, b)` for Łukasiewicz. For Gödel take `a = 0.2, b = 0.9`: the left side
 * is `0.9`, the right `0.8`.
 *
 * ## Relationship to the other tiers
 *
 * MV ⊂ BL ⊂ residuated. Łukasiewicz passes [ResiduumLaws], [BLAlgebraLaws] and
 * this; Product passes the first two and fails this; nilpotent minimum passes
 * only the first. The tiers are a genuine hierarchy, and `fuzzy-laws`' own tests
 * pin every cell of it.
 *
 * Łukasiewicz is also the algebra whose biresiduum `a ⇔ b` is `1 − |a − b|` — a
 * metric. That is not a coincidence; it is what being an MV-algebra buys you.
 * See [dk.eusrbin.fuzzy.algebra.Implications.biresiduum].
 */
public object MVAlgebraLaws {

    private const val CITATION = "Bergmann 2008, §12 (MV-algebras); Chang 1958"

    /**
     * Checks [algebra] and **throws** on any failure.
     *
     * @throws LawViolationException if any MV axiom fails.
     */
    @JvmStatic
    @JvmOverloads
    public fun verify(
        algebra: Algebra,
        // MV5, MV7 and MV8 all go through `¬`, so — as in DeMorganLaws — the
        // negation's numerics set the floor. Tolerance's KDoc has the detail.
        tolerance: Tolerance = Tolerance.looserOf(
            Tolerance.forAlgebra(algebra),
            Tolerance.forNegation(algebra.negation),
        ),
        sampling: Sampling = Sampling.DEFAULT,
    ) {
        check(algebra, tolerance, sampling).assertHolds()
    }

    /** Checks [algebra] and **returns** a [LawReport]. Never throws. */
    @JvmStatic
    @JvmOverloads
    public fun check(
        algebra: Algebra,
        // MV5, MV7 and MV8 all go through `¬`, so — as in DeMorganLaws — the
        // negation's numerics set the floor. Tolerance's KDoc has the detail.
        tolerance: Tolerance = Tolerance.looserOf(
            Tolerance.forAlgebra(algebra),
            Tolerance.forNegation(algebra.negation),
        ),
        sampling: Sampling = Sampling.DEFAULT,
    ): LawReport {
        val checker = LawChecker("MVAlgebraLaws", algebra.toString(), tolerance, sampling)

        // ⊕ and ¬, named as the axioms name them, so the code below reads as
        // the axiom list it is implementing.
        fun oplus(a: Double, b: Double): Double = algebra.or(a, b)
        fun neg(a: Double): Double = algebra.not(a)

        checker.law2("(MV1) commutativity: a⊕b = b⊕a", CITATION) { a, b ->
            checker.eq(oplus(a, b), oplus(b, a), "a⊕b", "b⊕a")
        }

        checker.law3("(MV2) associativity: (a⊕b)⊕c = a⊕(b⊕c)", CITATION) { a, b, c ->
            checker.eq(oplus(oplus(a, b), c), oplus(a, oplus(b, c)), "(a⊕b)⊕c", "a⊕(b⊕c)")
        }

        checker.law1("(MV3) neutral: a⊕0 = a", CITATION) { a ->
            checker.eq(oplus(a, 0.0), a, "a⊕0", "a")
        }

        checker.law1("(MV4) annihilator: a⊕1 = 1", CITATION) { a ->
            checker.eq(oplus(a, 1.0), 1.0, "a⊕1", "1")
        }

        checker.law1("(MV5) involutivity: ¬¬a = a", CITATION) { a ->
            checker.eq(neg(neg(a)), a, "¬¬a", "a")
        }

        checker.law1("(MV6) ¬0 = 1", CITATION) { _ ->
            checker.eq(neg(0.0), 1.0, "¬0", "1")
        }

        // Fails at a = 0.5 for Gödel (0.5) and Product (0.75).
        checker.law1("(MV7) excluded middle: a⊕¬a = 1", CITATION) { a ->
            checker.eq(oplus(a, neg(a)), 1.0, "a⊕¬a", "1")
        }

        // Chang's axiom — the characteristic one. Both sides are max(a,b) for
        // Łukasiewicz; for Gödel, a = 0.2, b = 0.9 gives 0.9 vs 0.8.
        checker.law2("(MV8) Chang: ¬(¬a⊕b)⊕b = ¬(¬b⊕a)⊕a", CITATION) { a, b ->
            checker.eq(
                oplus(neg(oplus(neg(a), b)), b),
                oplus(neg(oplus(neg(b), a)), a),
                "¬(¬a⊕b)⊕b",
                "¬(¬b⊕a)⊕a",
            )
        }

        return checker.report()
    }
}
