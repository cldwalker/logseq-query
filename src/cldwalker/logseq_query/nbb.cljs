(ns cldwalker.logseq-query.nbb
  "Main entry point for nbb-based lq"
  (:require [nbb.core]
            [cldwalker.logseq-query.tasks :as tasks]))

(def commands
  {:q {:fn #'tasks/q
       :doc "Run a named query"}
   :sq {:fn #'tasks/sq
        :doc "Run a short query"}
   :graphs {:fn #'tasks/graphs
            :doc "List all graphs"}
   :queries {:fn #'tasks/queries
             :doc "List all queries"}
   :rules {:fn #'tasks/rules
           :doc "List all rules"}})

(defn- print-help
  []
  (println "The following commands are available:\n")
  (doseq [[k m] commands]
    (println (.padEnd (name k) 7) (:doc m))))

(defn -main
  [[cmd & args]]
  (if (or (nil? cmd) (#{"-h" "--help"} cmd))
    (print-help)
    (if-let [cmd-m (get commands (keyword cmd))]
      (do
        (set! (.. js/process -env -LQ_COMMAND) cmd)
        ((:fn cmd-m) args))
      (println "No such command:" cmd))))

(when (= nbb.core/*file* (:file (meta #'-main)))
  (-main *command-line-args*))
