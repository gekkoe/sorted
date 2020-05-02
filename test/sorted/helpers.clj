;;; General-use helper functions
(ns sorted.helpers
  (:require [clojure.spec.gen.alpha :as gen]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as st]
            [failjure.core :as f]))

;;;============================================================================
;;;                           P U B L I C
;;;============================================================================

(defn checks?
  "Checks a function against a spec and returns true if all checks pass.
  If f is not supplied, will assume that f has same qualified name as spec."
  ([spec n] (checks? (eval spec) spec n))
  ([f spec n]
   (f/if-let-ok? [attempt
                  (f/try*
                   (-> (st/check-fn
                        f
                        spec
                        {:clojure.spec.test.check/opts {:num-tests n}})
                       :clojure.spec.test.check/ret
                       :pass?))]
                 attempt
                 false)))

(defn contains-all?
  "Given a map and one or more keys, returns true if m contains all ks.
  Returns false if not, or if m is not a map."
  [m & ks]
  (if (map? m)
    (every? #(contains? m %) ks)
    false))

(defn gen-samples
  "Given a spec, generates a collection of n random samples.
  Returns a Failure object if unsuccessful."
  ([spec n]
   (if (s/get-spec spec)
     (f/if-let-ok?
      [samples (f/try* (gen/sample (s/gen spec) n))]
      samples
      (f/fail (str "Error in gen-samples: Could not "
                   "generate samples from spec `%s` "
                   "with num-samples = %s\n%s")
              spec n (f/message samples)))
     (f/fail "Error in gen-samples: `%s` is not a valid spec." spec))))

(defn verified?
  "Returns true if every one of n generated samples of spec is a valid
  should-be."
  [spec should-be n]
  (f/if-let-ok? [attempt (f/try*
                          (every? #(s/valid? (s/get-spec should-be) %)
                                  (gen-samples spec n)))]
                attempt
                false))

;;;============================================================================
;;;                              S P E C S
;;;============================================================================

(s/def ::sample1 int?)
(s/def ::sample2 string?)
(def spec-gen (gen/frequency  ; Produce valid and invalid specs for testing.
               [[2 (gen/return ::sample1)]
                [2 (gen/return ::sample2)]
                [1 (gen/return 42)]
                [1 (s/gen any?)]]))
(s/def ::sample-spec (s/with-gen any? (constantly spec-gen)))

(def fn-gen (gen/frequency  ; Produce valid and invalid fns for testing.
             [[1 (gen/return ::sample1)]
              [1 (gen/return ::sample2)]
              [3 (gen/return (constantly 42))]  ; Valid fn here
              [1 (s/gen any?)]]))
(s/def ::sample-fn (s/with-gen any? (constantly fn-gen)))

(def ^:private samples-num 500)

;; Not sure what I could put in :fn on this one that wouldn't just be a direct
;; repeat of the function itself. So I've just included some manual testing in
;; additional to the basic generative spec compliance tests.
(s/fdef checks?
  :args (s/cat :f ::sample-fn :spec ::sample-spec :n pos-int?)
  :ret (s/nilable boolean?))

(s/fdef contains-all?
  :args (s/cat :m map? :ks (s/* any?))
  :ret boolean?)

;; NOTE: While the following specs do check out in every case I've tried, they
;;   starts taking an extremely long time to run when I attempt to run more than
;;   about 22 tests in a given call to st/check-fn. I'm not certain what is
;;   causing this behavior, though I suspect it has to do with gen/frequency. In
;;   production code this would likely need to be ironed out or at least
;;   explained before release.

(s/fdef gen-samples
  :args (s/cat :spec ::sample-spec :n pos-int?)
  :ret (s/or :success coll?
             :failure f/failed?)
  :fn #(let [spec (s/get-spec (-> % :args :spec))
             ret  (-> % :ret second)]
         (if spec
           (every? (partial s/valid? spec) ret)
           (f/failed? ret))))

(s/fdef verified?
  :args (s/cat :spec ::sample-spec :should-be ::sample-spec :n pos-int?)
  :ret boolean?
  :fn #(let [spec-kw (-> % :args :spec)
             spec (s/get-spec spec-kw)
             should-be (s/get-spec (-> % :args :should-be))
             ret  (-> % :ret)]
         (if (and spec should-be)            ; We have valid specs so...
           (= ret (every? (partial s/valid? should-be)
                          (gen-samples spec-kw samples-num)))
           (not ret))))                      ; Invalid args should return false.
