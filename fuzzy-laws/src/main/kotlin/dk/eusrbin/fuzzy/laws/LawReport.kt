package dk.eusrbin.fuzzy.laws

/**
 * A concrete input tuple that breaks a law, with an explanation.
 *
 * A law suite that says "failed" has told you almost nothing. The counterexample
 * is the product — CLAUDE.md §7 puts these suites in the hands of a user who
 * wrote their own t-norm, and "your t-norm is not associative" is only useful
 * with the triple that proves it.
 *
 * @property inputs the degrees that witness the violation, in the order the
 *   law's statement names them.
 * @property detail a rendered explanation, including the observed values, the
 *   gap, and the tolerance that was applied.
 */
public class Counterexample(
    public val inputs: DoubleArray,
    public val detail: String,
) {
    override fun toString(): String = "at (${inputs.joinToString(", ")}): $detail"
}

/**
 * The outcome of checking a single law against a single subject.
 *
 * @property law the law's name, e.g. `"associativity"`.
 * @property citation where the law comes from — CLAUDE.md §2: *"This library
 *   implements published mathematics. Each operation traces to a source."* A law
 *   suite that cannot cite itself is an opinion.
 * @property counterexample `null` iff the law held across every sampled input.
 */
public class LawResult(
    public val law: String,
    public val citation: String,
    public val counterexample: Counterexample?,
) {
    /** `true` iff no counterexample was found. */
    public val holds: Boolean
        get() = counterexample == null

    override fun toString(): String =
        if (holds) "✓ $law  [$citation]" else "✗ $law  [$citation]\n      $counterexample"
}

/**
 * The outcome of checking a whole suite against a subject.
 *
 * Returned by every `check` method in this package. Its counterpart `verify`
 * throws instead — see [LawViolationException] for which to use when.
 *
 * @property suite the suite's name, e.g. `"TNormLaws"`.
 * @property subject what was checked, as rendered by its `toString`.
 * @property tolerance the tolerance applied — reported because a pass is only
 *   meaningful alongside the epsilon it was obtained under (CLAUDE.md §8).
 * @property results one entry per law, in the order the suite states them.
 */
public class LawReport(
    public val suite: String,
    public val subject: String,
    public val tolerance: Tolerance,
    public val results: List<LawResult>,
) {

    /** The laws that failed. Empty iff [holds]. */
    public val failures: List<LawResult>
        get() = results.filter { !it.holds }

    /** `true` iff every law held. */
    public val holds: Boolean
        get() = results.all { it.holds }

    /**
     * Throws [LawViolationException] if any law failed; otherwise returns.
     *
     * This is what `verify` calls. Use it when you have a report in hand and
     * want to turn it into a test failure.
     */
    public fun assertHolds() {
        if (!holds) throw LawViolationException(this)
    }

    override fun toString(): String = buildString {
        append(if (holds) "✓ " else "✗ ")
        append(suite)
        append(" — ")
        append(subject)
        append("  (")
        append(tolerance)
        append(")")
        for (result in results) {
            append("\n    ")
            append(result)
        }
    }
}

/**
 * Thrown by the `verify` methods when a law fails.
 *
 * Extends [AssertionError] rather than [RuntimeException] so that it reads as a
 * test failure in every JVM test framework — JUnit, TestNG, kotest, or a
 * Clojure `deftest` — without `fuzzy-laws` having to know which one you use.
 * That matters because CLAUDE.md §7 makes this a *published* artifact rather
 * than an internal test folder: it has to land correctly in a test runner it was
 * never told about.
 *
 * @property report the full report, for programmatic inspection.
 */
public class LawViolationException(
    public val report: LawReport,
) : AssertionError(
    buildString {
        append(report.failures.size)
        append(" of ")
        append(report.results.size)
        append(" laws failed for ")
        append(report.subject)
        append(" — ")
        append(report.suite)
        append(" (")
        append(report.tolerance)
        append(")")
        for (failure in report.failures) {
            append("\n  ✗ ")
            append(failure.law)
            append("  [")
            append(failure.citation)
            append("]\n      ")
            append(failure.counterexample)
        }
    },
)
