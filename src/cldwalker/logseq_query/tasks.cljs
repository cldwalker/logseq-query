(ns cldwalker.logseq-query.tasks
  (:require [cldwalker.logseq-query.util :as util]
            [cldwalker.logseq-query.cli :as cli]))

;; Common q
(defn- add-default-options
  [args]
  (update-in args [:options] #(merge (:default-options (util/get-config))
                                     (when js/process.env.LQ_GRAPH
                                       {:graph js/process.env.LQ_GRAPH})
                                     %)))

(defn q*
  [{:keys [arguments summary] :as args}]
  (if (empty? arguments)
    (cli/print-summary " QUERY [& QUERY-ARGS]" summary)
    ;; (d/q ...)
    (prn (add-default-options args))))

(def common-options
  [["-h" "--help" "Print help"]
   ["-g" "--graph GRAPH" "Choose a graph"]
   ["-e" "--export" "Print/export query for use with logseq"]
   ["-t" "--table" "Render results in a table"]
   [nil "--table-command COMMAND" "Command to run with --table"]
   ["-p" "--puget" "Colorize results with puget"]
   ["-P" "--no-puget" :id :puget :parse-fn not]
   ["-c" "--count" "Print count of results"]
   ["-C" "--block-content" "Print only :block/content of result"]
   ["-T" "--tag-counts" "Print tag counts for query results"]
   ["-s" "--silence" "Silence noisy errors like d/q error"]])

(def q-cli-options common-options)

(defn q
  "Run a named query"
  [args]
  (cli/run-command q* args q-cli-options))

(def sq-cli-options common-options)

(defn sq*
  [{:keys [arguments summary] :as args}]
  (if (empty? arguments)
    (cli/print-summary " QUERY [& QUERY-ARGS]" summary)
    ;; (d/sq ...)
    (prn (add-default-options args))))

(defn sq
  "Run a short query"
  [args]
  (cli/run-command sq* args sq-cli-options))

;; Non query commands

(defn rules
  "List all rules"
  []
  (util/print-table
   (->> (util/get-all-rules)
        (map (fn [[rule-name m]]
               (hash-map :name (name rule-name)
                         :desc (:desc m)
                         :args (pr-str (first (:rule m)))
                         :namespace (namespace rule-name)))))
   :fields [:name :namespace :args :desc]))

(defn graphs
  "List all graphs"
  []
  (util/print-table
   (map #(hash-map :name (util/full-path->graph %)
                   :path %)
        (util/get-graph-paths))
   :fields [:name :path]))

(defn queries
  "List all queries"
  []
  (util/print-table
   (sort-by :name
            (map (fn [[k v]]
                   (merge {:name (name k) :namespace (namespace k)}
                          v))
                 (util/get-all-queries)))
   :fields [:name :namespace :parent :desc]))
