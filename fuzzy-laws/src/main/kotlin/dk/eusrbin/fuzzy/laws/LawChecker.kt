package dk.eusrbin.fuzzy.laws

import kotlin.math.abs

/**
 * Internal scaffolding shared by the suites: iterate the pool, collect the first
 * counterexample per law, assemble a [LawReport].
 *
 * Each `law*` method takes a predicate that returns `null` when the law holds at
 * that point, or a detail string explaining the violation. Returning the
 * explanation from the point of failure — rather than a boolean — is what lets
 * [Counterexample] say *how* a law broke and by how much, without every suite
 * re-deriving it.
 *
 * Checking stops at the first counterexample **per law**, but every law in a
 * suite is checked. A report says which laws hold, not merely whether the
 * subject is sound — [LawReport] for `StandardLaws` against `Algebra.PRODUCT`
 * showing idempotence and distributivity failing while nothing else does is the
 * useful answer (CLAUDE.md §7).
 */
internal class LawChecker(
    private val suite: String,
    private val subject: String,
    val tolerance: Tolerance,
    sampling: Sampling,
) {
    private val pool: DoubleArray = sampling.pool()
    private val results = ArrayList<LawResult>()

    /** Checks [predicate] at every degree in the pool. */
    fun law1(law: String, citation: String, predicate: (Double) -> String?) {
        for (a in pool) {
            val detail = predicate(a) ?: continue
            results += LawResult(law, citation, Counterexample(doubleArrayOf(a), detail))
            return
        }
        results += LawResult(law, citation, null)
    }

    /** Checks [predicate] at every pair from the pool. */
    fun law2(law: String, citation: String, predicate: (Double, Double) -> String?) {
        for (a in pool) {
            for (b in pool) {
                val detail = predicate(a, b) ?: continue
                results += LawResult(law, citation, Counterexample(doubleArrayOf(a, b), detail))
                return
            }
        }
        results += LawResult(law, citation, null)
    }

    /** Checks [predicate] at every triple from the pool. */
    fun law3(law: String, citation: String, predicate: (Double, Double, Double) -> String?) {
        for (a in pool) {
            for (b in pool) {
                for (c in pool) {
                    val detail = predicate(a, b, c) ?: continue
                    results += LawResult(law, citation, Counterexample(doubleArrayOf(a, b, c), detail))
                    return
                }
            }
        }
        results += LawResult(law, citation, null)
    }

    /**
     * `null` if `lhs = rhs` within [tolerance]; otherwise an explanation naming
     * both sides, the gap, and the epsilon that was not met.
     */
    fun eq(lhs: Double, rhs: Double, lhsExpr: String, rhsExpr: String): String? =
        if (tolerance.eq(lhs, rhs)) {
            null
        } else {
            "$lhsExpr = $lhs, but $rhsExpr = $rhs  (Δ = ${abs(lhs - rhs)}, ε = ${tolerance.epsilon})"
        }

    /** `null` if `lhs ≤ rhs` within [tolerance]; otherwise an explanation. */
    fun leq(lhs: Double, rhs: Double, lhsExpr: String, rhsExpr: String): String? =
        if (tolerance.leq(lhs, rhs)) {
            null
        } else {
            "$lhsExpr = $lhs, which exceeds $rhsExpr = $rhs  (Δ = ${lhs - rhs}, ε = ${tolerance.epsilon})"
        }

    fun report(): LawReport = LawReport(suite, subject, tolerance, results)
}
