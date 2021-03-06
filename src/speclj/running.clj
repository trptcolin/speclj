(ns speclj.running
  (:use
    [speclj.exec :only (pass-result fail-result)]
    [speclj.reporting :only (active-reporter report-runs report-pass report-fail report-description)]
    [speclj.components :only (reset-with)]
    [speclj.util :only (secs-since)])
  (:import
    [java.io File]))

(def default-runner (atom nil))
(declare *runner*)
(defn active-runner []
  (if (bound? #'*runner*)
    *runner*
    (if-let [runner @default-runner]
      runner
      (throw (Exception. "*runner* is unbound and no default value has been provided")))))

(defn- eval-components [components]
  (doseq [component components] ((.body component))))

(defn nested-fns [base fns]
  (if (seq fns)
    (partial (first fns) (nested-fns base (rest fns)))
    base))

(defn- eval-characteristic [befores body afters]
  (eval-components befores)
  (body)
  (eval-components afters))

(defn- reset-withs [withs]
  (doseq [with withs] (reset-with with)))

(defn- do-characteristic [characteristic reporter]
  (let [description @(.description characteristic)
        befores @(.befores description)
        body (nested-fns (.body characteristic) (map #(.body %) @(.arounds description)))
        afters @(.afters description)
        withs @(.withs description)
        start-time (System/nanoTime)]
    (try
      (eval-characteristic befores body afters)
      (let [result (pass-result characteristic (secs-since start-time))]
        (report-pass reporter result)
        result)
      (catch Exception e
        (let [result (fail-result characteristic (secs-since start-time) e)]
          (report-fail reporter result)
          result))
      (finally
        (reset-withs withs))))) ;MDM - Possible clojure bug.  Inlining reset-withs results in compile error 

(defn- do-characteristics [characteristics description reporter]
  (doall
    (for [characteristic characteristics]
      (do-characteristic characteristic reporter))))

(defn do-description [description reporter]
  (report-description reporter description)
  (eval-components @(.before-alls description))
  (let [results (do-characteristics @(.charcteristics description) description reporter)]
    (eval-components @(.after-alls description))
    results))

(defprotocol Runner
  (run-directories [this directories reporter])
  (run-description [this description reporter])
  (report [this reporter]))

(defn submit-description [description]
  (run-description (active-runner) description (active-reporter)))

(def clj-file-regex #".*\.clj")
(defn clj-files-in [& dirs]
  (let [files (reduce #(into %1 (file-seq (File. %2))) [] dirs)]
    (filter #(re-matches clj-file-regex (.getName %)) files)))


