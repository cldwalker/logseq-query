(ns cldwalker.logseq-query.nbb
  "Main entry point for nbb-based lq"
  (:require [nbb.core]
            [cldwalker.logseq-query.util :as util]
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

(defn- print-completions
  []
  (run! println (concat (map name (keys commands))
                        ;; complete queries for convenience
                        (map name (keys (util/get-all-queries))))))

(defn- run-command
  [cmd cmd-map args]
  (set! (.. js/process -env -LQ_COMMAND) cmd)
  ((:fn cmd-map) args))

(defn -main
  [[cmd & args]]
  (cond
    (or (nil? cmd) (#{"-h" "--help"} cmd))
    (print-help)
    (= "--completion" cmd)
    (print-completions)
    :else
    (if-let [cmd-map (get commands (keyword cmd))]
      (run-command cmd cmd-map args)
      ;; Allow queries as subcommands for convenience
      (if (contains? (set (map name (keys (util/get-all-queries)))) cmd)
        (run-command "q" (commands :q) (into [cmd] args))
        (println "No such command:" cmd)))))

(when (= nbb.core/*file* (:file (meta #'-main)))
  (-main (js->clj (.slice js/process.argv 2))))
