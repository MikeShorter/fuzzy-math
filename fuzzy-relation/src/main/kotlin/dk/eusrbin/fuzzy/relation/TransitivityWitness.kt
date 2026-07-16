package dk.eusrbin.fuzzy.relation

/**
 * A concrete refutation of T-transitivity: a path `x → via → y` whose composed
 * degree exceeds the direct one.
 *
 * T-transitivity requires `T(f_R(x, v), f_R(v, y)) ≤ f_R(x, y)` for all
 * `x, v, y` — equivalently `R ∘ R ⊆ R`, composition (Zadeh p.346) and
 * containment (eq. (2)), both `Source:`. **Attributed:** the name
 * "T-transitivity" (Zadeh 1971, not on hand — CLAUDE.md §21.7).
 *
 * ## Why the degrees are carried
 *
 * [ConvexityWitness][dk.eusrbin.fuzzy.set.ConvexityWitness]'s reason (§19.1,
 * §19.7): for a non-min t-norm the composed degree is **arithmetic**, not
 * selection, so recomputing it through a different code path might round
 * differently and the witness would fail to reproduce. Carrying both degrees is
 * what lets a reader — and `fuzzy-laws`' self-consistency law — re-derive the
 * failure exactly, rather than trust the report (§7).
 *
 * Note what a witness attests (CLAUDE.md §21.7): the **IEEE** inequality
 * `T(f_R(x,via), f_R(via,y)) > f_R(x,y)` over `double` degrees — the only
 * inequality the machine can attest, in
 * [checkEquality][dk.eusrbin.fuzzy.set.MembershipFn.checkEquality]'s stance.
 *
 * @property x the start of the path.
 * @property via the intermediate point.
 * @property y the end of the path.
 * @property composed `T(f_R(x, via), f_R(via, y))` — what the composition
 *   achieves through [via].
 * @property direct `f_R(x, y)` — what transitivity requires to be at least
 *   [composed], and is not.
 */
public class TransitivityWitness<X>(
    public val x: X,
    public val via: X,
    public val y: X,
    public val composed: Double,
    public val direct: Double,
) {
    /** How far the composed degree exceeds the direct one. Always positive. */
    public val excess: Double
        get() = composed - direct

    override fun toString(): String =
        "TransitivityWitness($x → $via → $y: " +
            "T(f(x,via), f(via,y)) = $composed > f(x,y) = $direct, excess $excess)"
}
