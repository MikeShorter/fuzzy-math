package dk.eusrbin.fuzzy.defuzz

import dk.eusrbin.fuzzy.set.Domain
import dk.eusrbin.fuzzy.set.DoubleMembershipFn

/**
 * **Scalar summaries of a membership function** — the seam to a control layer
 * above (CLAUDE.md §11a), and nothing more: each operation reduces a fuzzy set
 * over a window of ℝ to a single point of ℝ.
 *
 * ## Names, and where the control vocabulary stops
 *
 * The *artifact* is named `fuzzy-defuzz` deliberately — §11a: the
 * substrate/control boundary should be *"legible in the module graph rather
 * than buried in a package"* — and the control word does its work there and
 * stops (§22.6, the same line §21.6 drew for "CRI"). Inside, the operations
 * carry mathematical names: [centroid], [bisector], [meanOfMaxima],
 * [smallestOfMaxima], [largestOfMaxima]. The control literature's acronyms
 * (COG, MOM, SOM, LOM) are not used.
 *
 * ## Authority
 *
 * **`Attributed:` throughout** (CLAUDE.md §22, §2). Zadeh 1965 predates
 * defuzzification entirely, and Bergmann 2008 contains zero occurrences of any
 * of it — both checked, not assumed. The formulas below are self-describing
 * arithmetic (a weighted mean; a half-mass point; the mean of a maximizing
 * set); the *names* and the notion of defuzzification belong to the control
 * literature — Mamdani & Assilian 1975, surveyed in Van Leekwijck & Kerre
 * 1999, **neither on hand** — and per §17.5 an unconsultable source is named,
 * not cited as though it could arbitrate.
 *
 * (*Centre of sums* is deliberately absent — §22.4: its natural signature
 * takes a collection of per-rule output sets, which is control structure in a
 * substrate signature, and no text on hand pins down which of its variant
 * formulas is meant.)
 *
 * ## Why these are statics, and the cost of that
 *
 * These are queries, and §21.3's rule would make them members of a subtype
 * with closed forms to override — which `FuzzyNumber` has (`T(l,m,r) → m` for
 * the mean of maxima, exactly §20.8's shape). They cannot be: §11a put this
 * module downstream of the type they query, and a query that cannot host its
 * own overrides **must not consume a virtual answer** — it can neither match
 * the fidelity nor correct it (§22.2, filling the gap in §21.3). So every
 * operation here computes at the one fidelity it owns — **the fold's** — and
 * never calls the virtual `height` or `maximalGradeSet`. The price, recorded
 * in §22.2: an O(n) converging answer where an O(1) exact one exists. The
 * knob, should it bite: `FuzzyNumber` gains analytic summaries in
 * `fuzzy-number`, additively.
 *
 * ## The domain is `Domain<Double>`, and that is §11a's restriction, delivered
 *
 * A centroid needs arithmetic on X, so X is `Double` — and `Product<A,B>` is a
 * `Domain<Pair<A,B>>`, never a `Domain<Double>`, so §11a's *"requires an
 * `Enumerable` or `Sampled` domain"* is enforced by the type system with no
 * guard (§22.5: exhaustiveness is computed (§16.4), but the element type
 * really is static).
 *
 * Over a [dk.eusrbin.fuzzy.set.Sampled] window the answers are grid estimates
 * that converge under refinement — the standing caveat. Over an
 * `Enumerable<Double>` they are **exact for the enumerated points as discrete
 * mass**, duplicates counted (§16.3's σ-count semantics): note the ratio
 * `Σxf/Σf` estimates the *continuum* centroid only because [Sampled]'s spacing
 * is uniform and cancels (§22.1) — a hand-rolled non-uniform "grid" as an
 * `Enumerable` weights by its own density, and that is the honest answer to
 * the question actually asked.
 *
 * ## Every operation here refuses a set with no found mass
 *
 * Each summary divides by a fold — `Σf`, or the maximal degree — and when the
 * fold finds nothing there is **no quotient to return; returning one would
 * invent it** (IEEE would answer `NaN`; `0.0` is a plausible-looking point of
 * X; §11a says this value reaches an actuator). So they throw, per §22.3's
 * rule: *an operation may refuse when the refusal costs nothing and the
 * alternative is a fabricated answer* — the branch is on the divisor already
 * in hand, so §4's hot path is untouched.
 *
 * Note what the exception does **not** claim: "no mass found on this domain"
 * is a report of what was done, not of what is true. Over a [Sampled] window a
 * set narrower than the grid spacing has mass the fold cannot see — the same
 * set's `height` may honestly return `1.0` through §20.8's override while
 * these operations refuse (§22.3; positive findings are facts, negative
 * findings are not). Over an `Enumerable` the zero fold *is* Zadeh's emptiness
 * (§II p.340), proven.
 *
 * **For a control layer** — the audience this module exists for — a zero fold
 * is the *no-rules-fired* state, and a controller that can reach it should
 * test with [dk.eusrbin.fuzzy.set.MembershipFn.checkEmptiness] first: that is
 * the shipped, `Verdict`-shaped answer to "is there mass?", and it short-
 * circuits. The exception is for the caller who did not ask.
 */
public object Defuzzifiers {

    /**
     * The **centroid** (centre of gravity) — `Σ x·f(x) / Σ f(x)` over the
     * domain's points.
     *
     * **Attributed:** the name and the notion (control literature, §22).
     * The formula is a weighted mean and needs no source.
     *
     * Over a [dk.eusrbin.fuzzy.set.Sampled] window this estimates
     * `∫x·f dx / ∫f dx`: the uniform spacing `h` cancels in the ratio exactly
     * (§22.1 — which is also why this module ships **no** standalone
     * integral). Converges under refinement; §14.6's tests pin `4/3` for
     * `T(0,1,3)`.
     *
     * @throws IllegalArgumentException if no mass was found — see the object
     *   KDoc; test first with `checkEmptiness` if an empty set is an expected
     *   state.
     */
    @JvmStatic
    public fun centroid(fn: DoubleMembershipFn, over: Domain<Double>): Double {
        val points = over.elements()
        var mass = 0.0
        var moment = 0.0
        for (i in points.indices) {
            val x = points[i]
            val degree = fn.applyAsDouble(x)
            mass += degree
            moment += x * degree
        }
        requireMassFound(mass, "centroid", over)
        return moment / mass
    }

    /**
     * The **bisector** — the first domain point at which the running mass
     * reaches half the total: the grid's answer to "where does the area split
     * in two".
     *
     * **Attributed:** the name (control literature, §22).
     *
     * The returned point is a **sample** of the true bisector — the exact
     * split almost always lands between grid points — converging under
     * refinement ([dk.eusrbin.fuzzy.set.Sampled]'s standing caveat, and §22.2
     * records the honest null: this is one question at two fidelities, not
     * two questions). The comparison is against half the *sum*, not half an
     * integral: the spacing cancels on both sides (§22.1).
     *
     * @throws IllegalArgumentException if no mass was found — see the object
     *   KDoc.
     */
    @JvmStatic
    public fun bisector(fn: DoubleMembershipFn, over: Domain<Double>): Double {
        val points = over.elements()
        var mass = 0.0
        for (i in points.indices) {
            mass += fn.applyAsDouble(points[i])
        }
        requireMassFound(mass, "bisector", over)
        val half = mass / 2.0
        var running = 0.0
        for (i in points.indices) {
            val x = points[i]
            running += fn.applyAsDouble(x)
            if (running >= half) return x
        }
        // Unreachable: the full sum is `mass >= half` and degrees are finite.
        // Kept as a real failure rather than a silent fallback, per §7.
        throw IllegalStateException("bisector: running sum never reached half of $mass over $over")
    }

    /**
     * The **mean of maxima** — the mean of the domain points attaining the
     * fold's maximal degree.
     *
     * **Attributed:** the name; "MOM" is not used (§22.6).
     *
     * ## Deliberately *not* `mean(maximalGradeSet(over))`
     *
     * [dk.eusrbin.fuzzy.set.MembershipFn.maximalGradeSet] filters against the
     * **virtual** `height` — and for any function with an analytic `height`
     * override whose peak misses the grid (every fuzzy number over every
     * grid, §20.9's own example), that set is **empty**: the inherited `1.0`
     * is a fidelity no grid point can meet. §22.2's rule: a query that cannot
     * host its own overrides must not consume a virtual answer. This filters
     * against **the fold's own maximum** — question and filter at one
     * fidelity — which a nonempty domain always attains, and which converges
     * to the true mean of maxima under refinement.
     *
     * @throws IllegalArgumentException if the maximal degree found is `0` —
     *   the set is zero at every point the fold visited; see the object KDoc.
     */
    @JvmStatic
    public fun meanOfMaxima(fn: DoubleMembershipFn, over: Domain<Double>): Double {
        val points = over.elements()
        val maximal = maximalDegreeFound(fn, points, "meanOfMaxima", over)
        var sum = 0.0
        var count = 0
        for (i in points.indices) {
            val x = points[i]
            if (fn.applyAsDouble(x) >= maximal) {
                sum += x
                count++
            }
        }
        return sum / count
    }

    /**
     * The **smallest of maxima** — the least domain point attaining the fold's
     * maximal degree. See [meanOfMaxima] for the fidelity rule this family
     * follows (§22.2); "SOM" is not used (§22.6).
     *
     * Smallest/largest are by **value**, not fold order — over an
     * `Enumerable` the enumeration order is the caller's business, the number
     * line's order is not.
     *
     * @throws IllegalArgumentException if the maximal degree found is `0`.
     */
    @JvmStatic
    public fun smallestOfMaxima(fn: DoubleMembershipFn, over: Domain<Double>): Double {
        val points = over.elements()
        val maximal = maximalDegreeFound(fn, points, "smallestOfMaxima", over)
        var smallest = Double.POSITIVE_INFINITY
        for (i in points.indices) {
            val x = points[i]
            if (fn.applyAsDouble(x) >= maximal && x < smallest) smallest = x
        }
        return smallest
    }

    /**
     * The **largest of maxima** — the greatest domain point attaining the
     * fold's maximal degree. See [smallestOfMaxima].
     *
     * @throws IllegalArgumentException if the maximal degree found is `0`.
     */
    @JvmStatic
    public fun largestOfMaxima(fn: DoubleMembershipFn, over: Domain<Double>): Double {
        val points = over.elements()
        val maximal = maximalDegreeFound(fn, points, "largestOfMaxima", over)
        var largest = Double.NEGATIVE_INFINITY
        for (i in points.indices) {
            val x = points[i]
            if (fn.applyAsDouble(x) >= maximal && x > largest) largest = x
        }
        return largest
    }

    /**
     * The maxima family's first pass: the maximal degree the fold finds, with
     * the §22.3 refusal when it is zero. The comparison in the second pass is
     * `>=` against this value, which at least one point attains by
     * construction — the value came from these points.
     */
    private fun maximalDegreeFound(
        fn: DoubleMembershipFn,
        points: List<Double>,
        operation: String,
        over: Domain<Double>,
    ): Double {
        var maximal = 0.0
        for (i in points.indices) {
            val degree = fn.applyAsDouble(points[i])
            if (degree > maximal) maximal = degree
        }
        requireMassFound(maximal, operation, over)
        return maximal
    }

    /**
     * §22.3's refusal. Note the wording: "no mass **found**" reports what the
     * fold did, not what is true of the set — over a sampled window the two
     * can differ, and `checkEmptiness` over the same domain says `NotRefuted`,
     * not `Proven`, for exactly that reason.
     */
    private fun requireMassFound(found: Double, operation: String, over: Domain<Double>) {
        require(found > 0.0) {
            "$operation: no mass found on this domain — f is zero at every point $over folds " +
                "over, so there is no $operation to return, and returning one would invent it " +
                "(CLAUDE.md §22.3). If an empty or off-window set is an expected state, test " +
                "with checkEmptiness(over) first."
        }
    }
}
