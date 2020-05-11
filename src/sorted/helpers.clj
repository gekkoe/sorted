;;; General-use helper functions
(ns sorted.helpers
  (:require [clojure.spec.gen.alpha :as gen]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as st]
            [failjure.core :as f]))

;;;============================================================================
;;;                           P U B L I C
;;;============================================================================

(defn any-or
  "Given a spec, a symbol for a spec, or a predicate, returns a spec that will
  generate either the normal values expected or any type of value. Useful for
  testing fdef args."
  [spec-or-pred]
  (s/with-gen any? #(gen/one-of [(s/gen (if (symbol? spec-or-pred)
                                          (s/get-spec spec-or-pred)
                                          spec-or-pred))
                                 (s/gen any?)])))

(defn checks?
  "Checks a function against a spec and returns true if all checks pass.
  If f is not supplied but spec is, will assume that f has same qualified name
  as spec. If passed only n, returns a function that takes a spec and calls
  (checks? spec n)."
  ([n] (fn [spec] (checks? spec n)))
  ;; KLUDGE: Would like to do next line without eval.
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
  Returns a Failure object if unsuccessful. If passed only n, returns a function
  that takes a spec and calls (gen-samples spec n)."
  ([n] (fn [spec] (gen-samples spec n)))
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

(defn ok-map
  "Monadic map for failjure-aware functions. Attempts to map f, a function taking
  one argument that returns a Failure object upon failure, over xs. Short
  circuits if anything fails. Returns a new collection, or a Failure object."
  [f xs]
  (try
    (letfn [(f' [x y] (f/if-let-ok? [n (f y)]
                                    (conj x n)
                                    (throw (Exception. (str (f/message n))))))]
      (reduce f' [] xs))
    (catch Exception e
      (f/fail (f/message e)))))

(defn verified?
  "Returns true if every one of n generated samples of spec is a valid should-be.
  If passed only n, returns a function that takes spec and should-be and
  calls (verified? spec should-be n). If passed only should-be and n, returns a
  function that takes a spec and calls (verified? spec should-be n)"
  ([n] (fn [spec should-be] (verified? spec should-be n)))
  ([should-be n] (fn [spec] (verified? spec should-be n)))
  ([spec should-be n]
   (f/if-let-ok? [attempt (f/try*
                           (every? #(s/valid? (s/get-spec should-be) %)
                                   (gen-samples spec n)))]
                 attempt
                 false)))

;;;============================================================================
;;;                              S P E C S
;;;============================================================================

(s/def ::sample1 int?)
(s/def ::sample2 string?)
(s/def ::sample-spec (s/with-gen any? #(gen/one-of
                                        [(gen/return (s/get-spec ::sample1))
                                         (gen/return (s/get-spec ::sample2))])))

(s/def ::sample-fn (s/with-gen any? #(gen/one-of [(gen/return (constantly 42))
                                                  (gen/return inc)])))

;; Some test fns that fail half the time
(defn- fail-str [_] (when (odd? (rand-int 2)) (f/fail "f failed")))
(defn- fail-num [_] (if (odd? (rand-int 2)) 8 (f/fail 9)))
(s/def ::failjure-fn (s/with-gen any? #(gen/one-of [(gen/return (constantly 42))
                                                    (gen/return fail-str)
                                                    (gen/return fail-num)])))

(def ^:private samples-num 500)

(s/fdef any-or
  :args (s/cat :spec (any-or ::sample-spec))
  :ret s/spec?)

(s/fdef checks?
  :args (s/or :unary   (s/cat :n (any-or pos-int?))
              :binary  (s/cat :spec ::sample-spec
                              :n (any-or pos-int?))
              :ternary (s/cat :f (any-or ::sample-fn)
                              :spec (any-or ::sample-spec)
                              :n (any-or pos-int?)))
  :ret (s/or :partial fn?
             :full    (s/nilable boolean?)))

(s/fdef contains-all?
  :args (s/cat :m (any-or map?) :ks (s/* any?))
  :ret boolean?)

(s/fdef gen-samples
  :args (s/or :unary  (s/cat :n (any-or pos-int?))
              :binary (s/cat :spec (any-or ::sample-spec) :n (any-or pos-int?)))
  :ret (s/or :partial fn?
             :success coll?
             :failure f/failed?))

(s/fdef ok-map
  :args (s/cat :f ::failjure-fn :xs sequential?)
  :ret (s/or :success vector?
             :failure f/failed?))

(s/fdef verified?
  :args (s/or :unary   (s/cat :n (any-or pos-int?))
              :binary  (s/cat :should-be (any-or ::sample-spec)
                              :n (any-or pos-int?))
              :ternary (s/cat :spec (any-or ::sample-spec)
                              :should-be (any-or ::sample-spec)
                              :n (any-or pos-int?)))
  :ret (s/or :partial fn?
             :full boolean?)
  :fn #(let [arity (-> % :args first)
             ret   (-> % :ret second)]
         (if (= arity :ternary)
           (let [spec-kw (-> % :args second :spec)
                 spec (s/get-spec spec-kw)
                 should-be (s/get-spec (-> % :args second :should-be))]
             (if (and spec should-be)        ; We have valid specs so...
               (= ret (every? (partial s/valid? should-be)
                              (gen-samples spec-kw samples-num)))
               (not ret)))                   ; Invalid args should return false.
           (fn? ret))))                      ; Else should have returned a fn
