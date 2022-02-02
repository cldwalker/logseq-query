(ns cldwalker.logseq-query.datascript
  (:require [datascript.core :as d]
            [datascript.transit :as dt]
            [cldwalker.logseq-query.util :as util]))

(defn- get-graph-db
  [graph]
  (let [file (some #(when (= graph (util/full-path->graph %)) %)
                   (util/get-graph-paths))]
    (-> file slurp dt/read-transit-str)))

;; TODO: Args to queries
(defn q
  [{:keys [query graph]}]
  (let [graph' (or graph (:default-graph (util/get-config)))
        db (get-graph-db graph')
        rules (map :rule (util/get-rules))
        queries (util/get-queries)
        query-m (get queries (keyword query))
        _ (when-not query-m
            (println "Error: No query found for" query)
            (System/exit 1))
        args (cond-> [db]
                     (:args query-m)
                     (conj (:args query-m))
                     true
                     (conj rules))
        res (apply d/q (:query query-m) args)]
    (prn res)))
