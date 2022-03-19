(ns cldwalker.logseq-query.tasks
  "Run lq's main tasks. Call out to bb-datascript or clojure depending on what
  user has installed"
  (:require [cldwalker.logseq-query.util :as util]
            [cldwalker.logseq-query.cli :as cli]
            [babashka.tasks :refer [clojure]]))

(defn rules
  "List all rules"
  []
  (util/print-table
   (->> (util/get-all-rules)
        (map (fn [[rule-name m]]
               (hash-map :name (name rule-name)
                         :desc (:desc m)
                         :namespace (namespace rule-name)))))
   :fields [:name :namespace :desc]))

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
        (clojure (format "-X:bb cldwalker.logseq-query.datascript/q '%s'"
                         (pr-str (add-default-options args))))))

(def common-options
  [["-h" "--help" "Print help"]
   ["-g" "--graph GRAPH" "Choose a graph"]
   ["-t" "--table" "Render results in a table"]
   [nil "--table-command COMMAND" "Command to run with --table"]
   ["-p" "--puget" "Colorize results with puget"]
   ["-P" "--no-puget" :id :puget :parse-fn not]
   ["-c" "--count" "Print count of results"]
   ["-C" "--block-content" "Print only :block/content of result"]
   ["-s" "--silence" "Silence noisy errors like d/q error"]
   ["-e" "--export" "Print/export query for use with logseq"]])

(def q-cli-options common-options)

(defn q
  "Run a named query"
  [& args]
  (cli/run-command q* args q-cli-options))

(def sq-cli-options
  (into common-options
        [["-n" "--pretend" "Print the full query that would execute"]]))

(defn sq*
  [{:keys [options arguments summary] :as args}]
  (cond (or (:help options) (empty? arguments))
        (cli/print-summary " QUERY [& QUERY-ARGS]" summary)
        (System/getenv "BABASHKA_DATASCRIPT")
        ((requiring-resolve 'cldwalker.logseq-query.datascript/sq)
         (add-default-options args))
        :else
        (clojure (format "-X:bb cldwalker.logseq-query.datascript/sq '%s'"
                         (pr-str (add-default-options args))))))

(defn sq
  "Run a short query"
  [& args]
  (cli/run-command sq* args sq-cli-options))

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
