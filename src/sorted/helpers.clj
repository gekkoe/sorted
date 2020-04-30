;;; General-use helper functions
(ns sorted.helpers
  (:require [clojure.spec.gen.alpha :as gen]
            [clojure.spec.alpha :as s]
            [failjure.core :as f]))

(defn gen-samples
  "Given a spec, generates a collection of n random samples.
  Returns a Failure object if unsuccessful."
  ([spec n]
   (if (s/get-spec spec)
     (f/if-let-ok?
      [samples (f/try* (gen/sample (s/gen spec) n))]
      samples
      (f/fail "Could not generate samples from spec `%s` with num-samples = %s"
              spec (str n)))
     (f/fail "`%s` is not a valid spec." spec))))

(s/def ::sample1 int?)
(s/def ::sample2 string?)
(def sample-gen (gen/frequency  ; Produce valid and invalid specs for testing.
                 [[1 (gen/return ::sample1)]
                  [1 (gen/return ::sample2)]
                  [1 (gen/return 42)]
                  [1 (s/gen any?)]]))
(s/def ::sample-spec (s/with-gen any? (constantly sample-gen)))

;; NOTE: While this spec does check out in every case I've tried, it starts
;;   taking an extremely long time to run when I attempt to run more than about
;;   22 tests in a given call to st/check-fn. Not certain what is causing this
;;   behavior, but in production code it would likely need to be ironed out or
;;   at least explained before release.
(s/fdef gen-samples
  :args (s/cat :spec ::sample-spec :n pos-int?)
  :ret (s/or :success coll?
             :failure f/failed?)
  :fn #(let [spec (s/get-spec (-> % :args :spec))
             ret  (-> % :ret second)]
         (if spec
           (every? (partial s/valid? spec) ret)
           (f/failed? ret))))
