(ns cldwalker.logseq-query.tasks
  (:require [cldwalker.logseq-query.util :as util]
            [cldwalker.logseq-query.cli :as cli]
            [babashka.tasks :refer [clojure]]))

(defn rules
  []
  (util/print-table
   (->> (util/get-all-rules)
        (map #(hash-map :name (:name %)
                        :desc (:desc %)
                        :author (:author %))))
   :fields [:name :author :desc]))

(defn- add-default-options
  [args]
  (update-in args [:options] #(merge (:default-options (util/get-config)) %)))

(defn q*
  [{:keys [options arguments summary] :as args}]
  (cond (or (:help options) (empty? arguments))
        (cli/print-summary " QUERY [& QUERY-ARGS]" summary)
        (System/getenv "BABASHKA_DATASCRIPT")
        ((requiring-resolve 'cldwalker.logseq-query.datascript/q)
         (add-default-options args))
        :else
        (clojure (format "-X cldwalker.logseq-query.datascript/q '%s'"
                         (pr-str (add-default-options args))))))

(def q-cli-options
  [["-h" "--help" "Print help"]
   ["-g" "--graph GRAPH" "Choose a graph"]
   ["-t" "--table" "Render results in a table"]
   [nil "--table-command COMMAND" "Command to run with --table"]
   ["-p" "--puget" "Colorize results with puget"]
   ["-P" "--no-puget" :id :puget :parse-fn not]
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
   [nil "--table-command COMMAND" "Command to run with --table"]
   ["-p" "--puget" "Colorize results with puget"]
   ["-c" "--count" "Print count of results"]
   ["-C" "--block-content" "Only prints :block/content of result"]
   ["-n" "--pretend" "Prints the full query that would execute"]
   ["-s" "--silence" "Silence noisy d/q error"]])

(defn qs*
  [{:keys [options arguments summary] :as args}]
  (cond (or (:help options) (empty? arguments))
        (cli/print-summary " QUERY [& QUERY-ARGS]" summary)
        (System/getenv "BABASHKA_DATASCRIPT")
        ((requiring-resolve 'cldwalker.logseq-query.datascript/qs)
         (add-default-options args))
        :else
        (clojure (format "-X cldwalker.logseq-query.datascript/qs '%s'"
                         (pr-str (add-default-options args))))))

(defn qs
  [& args]
  (cli/run-command qs* args qs-cli-options))

(defn graphs
  []
  (util/print-table
   (map #(hash-map :name (util/full-path->graph %)
                   :path %)
        (util/get-graph-paths))
   :fields [:name :path]))

(defn queries
  []
  (util/print-table
   (sort-by :name
            (map (fn [[k v]]
                   (merge {:name k} v))
                 (util/get-all-queries)))
   :fields [:name :parent :author :desc]))
