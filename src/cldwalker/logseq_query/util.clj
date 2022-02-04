(ns cldwalker.logseq-query.util
  (:require [clojure.edn :as edn]
            [clojure.pprint :as pprint]
            [babashka.fs :as fs]))

(defn logseq-query-path
  [filename]
  (str (fs/path (fs/parent (fs/parent (System/getenv "_")))
            filename)))

(defn get-rules
  []
  (-> "rules.edn" logseq-query-path slurp edn/read-string))

(defn get-queries
  []
  (-> "queries.edn" logseq-query-path slurp edn/read-string))

(defn get-config
  []
  (let [config-file (logseq-query-path "config.edn")]
    (if (fs/exists? config-file)
     (-> config-file slurp edn/read-string)
     {})))

(defn get-graph-paths
  []
  (map str (fs/glob (fs/expand-home "~/.logseq/graphs") "*.transit")))

(defn full-path->graph
  [path]
  (second (re-find #"\+\+([^\+]+).transit$" path)))

(defn print-table
  [rows]
  (pprint/print-table rows)
  (println "Total:" (count rows)))
