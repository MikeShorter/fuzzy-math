(ns fuzzy.conformance-test
  "The §9 conformance suite — CLAUDE.md §23.4.

  fuzzy-laws for the JVM-interop contract: every test here is a sentence from
  the record (§9, §14.4, §16.1, §16.4, §16.5, §21.2) exercised from Clojure
  against the real artifacts, with no Kotlin and no kotest anywhere near it.
  A regression — a member losing its `default` body, a signature going
  hostile, the -jvm-default knob drifting (§23.2) — fails `./gradlew check`
  here before it can reach a publish.

  Also the deps.edn documentation-by-example the BOM cannot be (§23.5 —
  tools.deps has no BOM import): a Clojure consumer depends on the artifacts
  they want, per artifact:

      {:deps {dk.eusrbin/fuzzy-algebra {:mvn/version \"...\"}
              dk.eusrbin/fuzzy-laws    {:mvn/version \"...\"}}}"
  (:require [clojure.test :refer [deftest is testing run-tests]])
  (:import [dk.eusrbin.fuzzy.algebra TNorm]
           [dk.eusrbin.fuzzy.set MembershipFn DoubleMembershipFn Enumerable Sampled
            Verdict Verdict$Proven Verdict$Refuted Verdict$NotRefuted]
           [dk.eusrbin.fuzzy.number TriangularNumber]
           [dk.eusrbin.fuzzy.relation FuzzyRelations]
           [dk.eusrbin.fuzzy.defuzz Defuzzifiers]
           [dk.eusrbin.fuzzy.laws TNormLaws LawViolationException]))

(defn- close? [^double expected ^double actual ^double epsilon]
  (< (Math/abs (- expected actual)) epsilon))

;; -- §9: "Clojure reifys them in one line" -----------------------------------

(def lukasiewicz
  (reify TNorm (apply [_ a b] (max 0.0 (+ a b -1.0)))))

(deftest reify-a-tnorm-in-one-line
  (testing "§9 verbatim: one method implemented, the abstract one"
    (is (= 0.5 (.apply ^TNorm lukasiewicz 0.7 0.8))))
  (testing "the inherited default body — residuum by generic bisection"
    (is (close? 0.8 (.residuum ^TNorm lukasiewicz 0.5 0.3) 1e-9))))

;; -- §14.4: the laws artifact, consumed with no test framework adopted -------

(deftest laws-accept-a-clojure-tnorm
  (is (.getHolds (TNormLaws/check lukasiewicz))))

(deftest laws-reject-a-broken-tnorm
  ;; §7's test-of-the-test, from the consumer's side: the average is monotone
  ;; and commutative and is NOT a t-norm, and the suite must say so.
  (let [broken (reify TNorm (apply [_ a b] (/ (+ a b) 2.0)))]
    (is (thrown? LawViolationException (TNormLaws/verify broken)))
    (testing "§14.4's mechanism: it reads as a test failure in ANY runner"
      (is (thrown? AssertionError (TNormLaws/verify broken))))))

;; -- §16.5: one abstract method, ~17 inherited analysis members --------------

(def warm (reify MembershipFn (apply [_ x] (if (= x "warm") 0.8 0.2))))
(def terms (Enumerable/of ["cold" "warm" "hot"]))

(deftest membership-fn-inherits-the-lot
  (is (= 0.8 (.height ^MembershipFn warm terms)))
  (is (close? 1.2 (.sigmaCount ^MembershipFn warm terms) 1e-12))
  (is (= 3 (count (.support ^MembershipFn warm terms)))))

;; -- §16.1: the primitive path, for the consumer it named --------------------

(def triangle-fn
  (reify DoubleMembershipFn
    (applyAsDouble [_ x] (max 0.0 (- 1.0 (Math/abs (- x 1.0)))))))

(def window (Sampled/of 0.0 2.0 201))

(deftest double-membership-fn-via-applyAsDouble
  (is (< 0.99 (.height ^DoubleMembershipFn triangle-fn window)))
  (is (instance? Verdict$NotRefuted
                 (.findNonConvexity ^DoubleMembershipFn triangle-fn window))))

(deftest defuzzifiers-accept-a-clojure-set
  (is (close? 1.0 (Defuzzifiers/centroid triangle-fn window) 1e-6))
  (is (close? 1.0 (Defuzzifiers/meanOfMaxima triangle-fn window) 1e-6)))

(deftest defuzzifiers-refuse-zero-found-mass
  ;; §22.3's refusal, observed from the seam's far side.
  (let [nothing (reify DoubleMembershipFn (applyAsDouble [_ _] 0.0))]
    (is (thrown-with-msg? IllegalArgumentException #"no mass found"
                          (Defuzzifiers/centroid nothing window)))))

;; -- §16.4: Verdict, priced from outside --------------------------------------

(deftest verdict-reads-as-three-lines-of-condp
  (let [verdict (.checkContainment ^MembershipFn warm warm terms)]
    (is (= :proven
           (condp instance? verdict
             Verdict$Proven     :proven
             Verdict$Refuted    :refuted
             Verdict$NotRefuted :not-refuted))))
  (let [smaller (reify MembershipFn (apply [_ _] 0.1))
        refuted (.checkContainment ^MembershipFn warm smaller terms)]
    (is (instance? Verdict$Refuted refuted))
    (testing "§7: the witness is the product, and it is reachable"
      (is (= "cold" (.getWitness ^Verdict refuted))))))

;; -- §21.2: kotlin.Pair in a published signature ------------------------------

(def near
  (reify MembershipFn
    (apply [_ p]
      (let [^kotlin.Pair p p
            x (double (.getFirst p))
            y (double (.getSecond p))]
        (Math/exp (- (Math/abs (- x y))))))))

(def points (Enumerable/of [0.0 1.0 2.0]))

(deftest kotlin-pair-relations-work-with-stated-friction
  ;; The §23.3 pricing, executable: one type hint, two accessor calls, and
  ;; everything composes. (near ∘ near)(0, 2) routes through v = 1: e⁻¹.
  (let [composed (FuzzyRelations/compose near near points)]
    (is (close? (Math/exp -1.0)
                (.apply ^MembershipFn composed (kotlin.Pair. 0.0 2.0))
                1e-12)))
  (let [verdict (FuzzyRelations/findNonTransitivity near points)
        witness (.getWitness verdict)]
    (is (instance? Verdict$Refuted verdict))
    (testing "§19.7(3): the witness re-derives by hand, from Clojure"
      (is (= (.getComposed witness)
             (min (.apply ^MembershipFn near (kotlin.Pair. (.getX witness) (.getVia witness)))
                  (.apply ^MembershipFn near (kotlin.Pair. (.getVia witness) (.getY witness))))))
      (is (> (.getComposed witness) (.getDirect witness))))))

;; -- §23.3 qualification 1, pinned: 1.12 FI coercion does NOT apply ----------

(deftest bare-fns-do-not-coerce-to-the-sams
  ;; Not a §9 breach — §9 promised reify, and reify works — but the assumption
  ;; that Clojure 1.12's functional-interface coercion covers these interfaces
  ;; is FALSE today, and this pin is what announces a Clojure release that
  ;; changes it (at which point sugar wrappers lose their justification — the
  ;; §23.4 knob's own retirement condition).
  (is (thrown? ClassCastException
               (Defuzzifiers/centroid (fn [_] 0.5) window))))

;; -- fuzzy-number: parametric types, closed-form overrides, consumed as-is ---

(deftest fuzzy-number-overrides-visible-from-clojure
  (let [t (TriangularNumber/of 0.0 1.0 3.0)]
    (testing "§20.8's height override reads the carrier — analytic, off the peak"
      (is (close? 0.25 (.height ^TriangularNumber t (Sampled/of 2.5 3.5 101)) 1e-12)))
    (testing "§22.1's h-cancellation, from the consumer's side"
      (is (close? (/ 4.0 3.0)
                  (Defuzzifiers/centroid t (Sampled/of 0.0 3.0 1024))
                  1e-6)))))

;; -----------------------------------------------------------------------------

(defn -main [& _]
  (let [{:keys [fail error] :as summary} (run-tests 'fuzzy.conformance-test)]
    (println summary)
    (System/exit (if (zero? (+ fail error)) 0 1))))
