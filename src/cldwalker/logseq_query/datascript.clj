(ns cldwalker.logseq-query.datascript
  (:require [datascript.core :as d]
            [datascript.transit :as dt]
            [clojure.set :as set]
            [clojure.string :as str]
            [clojure.edn :as edn]
            [cldwalker.logseq-query.util :as util]))

(defn- get-graph-db
  [graph]
  (let [file (some #(when (= graph (util/full-path->graph %)) %)
                   (util/get-graph-paths))]
    (-> file slurp dt/read-transit-str)))

;; From earlier version of datascript
(defn- parse-query [query]
  (loop [parsed {} key nil qs query]
    (if-let [q (first qs)]
      (if (keyword? q)
        (recur parsed q (next qs))
        (recur (update-in parsed [key] (fnil conj []) q) key (next qs)))
      parsed)))

(defn- pull-or-single-binding?
  [find]
  (and (= 1 (count find))
       (or (not (coll? (first find)))
           (and (coll? (first find)) (= 'pull (ffirst find))))))

(defn q
  [{:keys [query graph args]}]
  (let [graph' (or graph (:default-graph (util/get-config)))
        db (get-graph-db graph')
        rules (map :rule (util/get-rules))
        queries (util/get-queries)
        query-m (get queries (keyword query))
        _ (when-not query-m
            (println "Error: No query found for" query)
            (System/exit 1))
        {:keys [find in]} (parse-query (:query query-m))
        expected-args (set/difference (set in) #{'% '$})
        actual-args (into [] (or (seq args) (:default-args query-m)))
        _ (when-not (= (count actual-args) (count expected-args))
            (println "Error: Wrong number of arguments")
            (println (format "Usage: lq q %s" (str/join " " expected-args)))
            (System/exit 1))
        q-args (conj actual-args rules)
        post-transduce (if (pull-or-single-binding? find) (map first) (map identity))
        res (apply d/q (:query query-m) db q-args)]
      (prn (into [] post-transduce res))))

(defn qs
  [{:keys [query graph]}]
  (let [graph' (or graph (:default-graph (util/get-config)))
        db (get-graph-db graph')
        rules (map :rule (util/get-rules))
        query' (edn/read-string query)
        query'' (if (keyword? (first query')) query' (conj [:where] query'))
        {:keys [find] :as query-map} (parse-query query'')
        query-map' (merge {:in '[$ %]
                           :find '[(pull ?b [*])]}
                          query-map)
        post-transduce (if (pull-or-single-binding? find) (map first) (map identity))
        res (d/q query-map' db rules)]
      (prn (into [] post-transduce res))))
