(ns cldwalker.logseq-query.util
  (:require [clojure.edn :as edn]
            [clojure.pprint :as pprint]
            [babashka.fs :as fs]))

(defn get-rules
  []
  (-> "rules.edn" slurp edn/read-string))

(defn get-queries
  []
  (-> "queries.edn" slurp edn/read-string))

(defn get-config
  []
  (if (fs/exists? "config.edn")
    (-> "config.edn" slurp edn/read-string)
    {}))

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
