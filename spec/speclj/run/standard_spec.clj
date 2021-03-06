(ns speclj.run.standard-spec
  (:use
    [speclj.core]
    [speclj.run.standard]
    [speclj.running :only (run-directories)]
    [speclj.reporting :only (active-reporter)]
    [speclj.report.silent :only (new-silent-reporter)]
    [speclj.spec-helper :only (find-dir)])
  (:import
    [java.io File]))

(def examples-dir (find-dir "examples"))
(def prime-factors-dir (.getCanonicalPath (File. examples-dir "prime_factors")))
(def failures-dir (.getCanonicalPath (File. examples-dir "failures")))

(describe "StandardRunner"
  (with runner (new-standard-runner))
  (with reporter (new-silent-reporter))

  (it "returns 0 failures when all tests pass"
    (should= 0 (run-directories @runner [prime-factors-dir] @reporter)))

  (it "returns lots-o failures when running failure example"
    (should= 8 (run-directories @runner [failures-dir] @reporter))))

(run-specs)