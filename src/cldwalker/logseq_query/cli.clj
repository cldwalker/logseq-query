(ns cldwalker.logseq-query.cli
  "Some of these are from bb-clis but not worth coupling yet"
  (:require [clojure.string :as str]))

(defn error
  "Print error message(s) and exit"
  [& msgs]
  (apply println "Error:" msgs)
  (System/exit 1))

(defn print-summary
  "Print help summary given args and opts strings"
  [args-string options-summary]
  (println (format "Usage: lq %s [OPTIONS]%s\nOptions:\n%s"
                   (System/getProperty "babashka.task")
                   args-string
                   options-summary)))

(defn run-command
  "Processes a command's functionality given a cli options definition, arguments
  and primary command fn. This handles option parsing, handles any errors with
  parsing and then passes parsed input to command fn"
  [command-fn args cli-opts & parse-opts-options]
  (let [{:keys [errors] :as parsed-input}
        (apply (requiring-resolve 'clojure.tools.cli/parse-opts) args cli-opts parse-opts-options)]
    (if (seq errors)
      (error (str/join "\n" (into ["Options failed to parse:"] errors)))
      (command-fn parsed-input))))
