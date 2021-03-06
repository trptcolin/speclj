(ns speclj.main
  (:gen-class)
  (:use
    [speclj.running :only (run-directories report)]
    [speclj.util :only (endl)])
  (:require
    [speclj.version])
  (:import
    [mmargs Arguments]))

(def default-config {
  :specs ["spec"]
  :runner "standard"
  :reporter "progress"
  })

(def invoke-method "java -cp [...] speclj.main")

(def arg-spec (Arguments.))
(doto arg-spec
  (.addMultiParameter "specs" "directories specifying which specs to run.")
  (.addValueOption "r" "runner" "RUNNER" (str "Use a custom Runner." endl
    endl
    "Builtin runners:" endl
    "standard               : (default) Runs all the specs once" endl
    "vigilant               : Watches for file changes and re-runs affected specs (used by autotest)" endl))
  (.addValueOption "f" "reporter" "REPORTER" (str "Specifies how to report spec results. Ouput will be written to *out*." endl
    endl
    "Builtin reporters:" endl
    "silent                 : No output" endl
    "progress               : (default) Text-based progress bar" endl
    "specdoc                : Code example doc strings" endl))
  (.addValueOption "f" "format" "FORMAT" "An alias for reporter.")
  (.addSwitchOption "a" "autotest" "Alias to use the 'vigilant' runner and 'specdoc' reporter.")
  (.addSwitchOption "v" "version" "Shows the current speclj version.")
  (.addSwitchOption "h" "help" "You're looking at it.")
  )

(defn- resolve-aliases [options]
  (cond
    (:format options) (recur (dissoc (assoc options :reporter (:format options)) :format))
    (:autotest options) (recur (dissoc (assoc options :runner "vigilant" :reporter "specdoc") :autotest))
    :else options))

(defn exit [code]
  (System/exit code))

(defn usage [errors]
  (if (seq errors)
    (do
      (println "ERROR!!!")
      (doseq [error (seq errors)]
        (println error))))
  (println)
  (println "Usage: " invoke-method (.argString arg-spec))
  (println)
  (println (.parametersString arg-spec))
  (println (.optionsString arg-spec))
  (if (seq errors)
    (exit -1)
    (exit 0)))

(defn print-version []
  (println speclj.version/summary)
  (exit 0))

(defn parse-args [& args]
  (let [parse-result (.parse arg-spec (into-array String args))
        options (reduce (fn [result entry] (assoc result (keyword (.getKey entry)) (.getValue entry))) {} parse-result)
        options (resolve-aliases options)]
    (if-let [errors (options :*errors)]
      (usage errors)
      (merge default-config options))))

(defn load-runner [name]
  (let [ns-name (symbol (str "speclj.run." name))
        ctor-name (symbol (str ns-name "/new-" name "-runner"))
        expr `(do (require '~ns-name) (~ctor-name))]
    (try
      (eval expr)
      (catch Exception e (throw (Exception. (str "Failed to load runner: " name) e))))))

(defn load-reporter [name]
  (let [ns-name (symbol (str "speclj.report." name))
        ctor-name (symbol (str ns-name "/new-" name "-reporter"))
        expr `(do (require '~ns-name) (~ctor-name))]
    (try
      (eval expr)
      (catch Exception e (throw (Exception. (str "Failed to load reporter: " name) e))))))

(defn do-specs [config]
  (let [runner (load-runner (:runner config))
        reporter (load-reporter (:reporter config))
        spec-dirs (:specs config)
        fail-count (run-directories runner spec-dirs reporter)]
    (exit fail-count)))

(defn run [& args]
  (let [config (apply parse-args args)]
    (cond
      (:version config) (print-version)
      (:help config) (usage nil)
      :else (do-specs config))))

(defn -main [& args]
  (apply run args))
