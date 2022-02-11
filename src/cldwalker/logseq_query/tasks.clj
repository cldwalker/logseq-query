(ns cldwalker.logseq-query.tasks
  (:require [cldwalker.logseq-query.util :as util]
            [cldwalker.logseq-query.cli :as cli]
            [babashka.tasks :refer [clojure]]))

(defn rules
  []
  (->> (util/get-rules)
       (map #(hash-map :name (ffirst (:rule %))
                       :desc (:desc %)))
       util/print-table))

(defn q*
  [{:keys [options arguments summary] :as args}]
  (cond (or (:help options) (empty? arguments))
    (cli/print-summary " QUERY [& QUERY-ARGS]" summary)
    (System/getenv "BABASHKA_DATASCRIPT")
    ((requiring-resolve 'cldwalker.logseq-query.datascript/q) args)
    :else
    (clojure (format "-X cldwalker.logseq-query.datascript/q '%s'"
                     (pr-str args)))))

(def q-cli-options
  [["-h" "--help" "Print help"]
   ["-g" "--graph GRAPH" "Choose a graph"]
   ["-t" "--table" "Render results in a table"]
   ["-p" "--puget" "Colorize results with puget"]
   ["-c" "--count" "Print count of results"]
   ["-C" "--block-content" "Only prints :block/content of result"]
   ["-s" "--silence" "Silence noisy d/q error"]])

(defn q
  [& args]
  (cli/run-command q* args q-cli-options))

(def qs-cli-options
  [["-h" "--help" "Print help"]
   ["-g" "--graph GRAPH" "Choose a graph"]
   ["-t" "--table" "Render results in a table"]
   ["-p" "--puget" "Colorize results with puget"]
   ["-c" "--count" "Print count of results"]
   ["-n" "--pretend" "Prints the full query that would execute"]
   ["-s" "--silence" "Silence noisy d/q error"]])

(defn qs*
  [{:keys [options arguments summary] :as args}]
  (cond (or (:help options) (empty? arguments))
    (cli/print-summary " QUERY [& QUERY-ARGS]" summary)
    (System/getenv "BABASHKA_DATASCRIPT")
    ((requiring-resolve 'cldwalker.logseq-query.datascript/qs) args)
    :else
    (clojure (format "-X cldwalker.logseq-query.datascript/qs '%s'"
                     (pr-str args)))))

(defn qs
  [& args]
  (cli/run-command qs* args qs-cli-options))

(defn graphs
  []
  (prn (map util/full-path->graph (util/get-graph-paths))))

(defn queries
  []
  (util/print-table
   (map (fn [[k v]]
          {:name k :desc (:desc v)})
        (util/get-queries))))
