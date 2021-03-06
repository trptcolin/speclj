(ns speclj.reporting
  (:use
    [speclj.exec :only (pass? fail?)]))

(def default-reporter(atom nil))
(declare *reporter*)
(defn active-reporter []
  (if (bound? #'*reporter*)
    *reporter*
    (if-let [reporter @default-reporter]
      reporter
      (throw (Exception. "*reporter* is unbound and no default value has been provided")))))

(defn failure-source [exception]
  (let [source (nth (.getStackTrace exception) 0)]
    (if-let [filename (.getFileName source)]
      (str (.getCanonicalPath (java.io.File. filename)) ":" (.getLineNumber source))
      "Unknown source")))

(defn tally-time [results]
  (loop [tally 0.0 results results]
    (if (seq results)
      (recur (+ tally (.seconds (first results))) (rest results))
      tally)))

(defprotocol Reporter
  (report-message [reporter message])
  (report-description [this description])
  (report-pass [this result])
  (report-fail [this result])
  (report-runs [this results]))
