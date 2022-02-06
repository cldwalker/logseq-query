(ns cldwalker.logseq-query.datascript
  (:require [datascript.core :as d]
            [datascript.transit :as dt]
            [clojure.set :as set]
            [clojure.string :as str]
            [clojure.edn :as edn]
            [clojure.pprint :as pprint]
            [babashka.tasks :refer [shell]]
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

(defn- print-table [rows]
  (-> (shell {:out :string} "echo" (pr-str rows))
      (shell "bb-table")))

(defn print-results
  [rows options]
  (if (:table options)
      (if (:block/uuid (first rows))
        (print-table (map #(merge {:id (:db/id %)} (:block/properties %))
                                 rows))
        (print-table rows))
      (if (:puget options)
        (-> (shell {:out :string} "echo" (pr-str rows))
            (shell "puget"))
        (prn rows))))

(defn- q-and-print-results
  [query query-args options {:keys [find]}]
  (let [post-transduce (if (pull-or-single-binding? find) (map first) (map identity))
        res (apply d/q query query-args)
        res' (cond-> (into [] post-transduce res)
                     (:count options)
                     count)]
    (print-results res' options)))

(defn q
  [{:keys [arguments options]}]
  (let [[query & args] arguments
        {:keys [graph]} options
        graph' (or graph (:default-graph (util/get-config)))
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
        q-args (conj actual-args rules)]
    (q-and-print-results (:query query-m) (into [db] q-args) options {:find find})))

(defn qs
  [{:keys [arguments options]}]
  (let [query (str/join " " arguments)
        graph (or (:graph options) (:default-graph (util/get-config)))
        db (get-graph-db graph)
        rules (map :rule (util/get-rules))
        query' (edn/read-string query)
        query'' (if (keyword? (first query')) query' (conj [:where] query'))
        {:keys [find] :as query-map} (parse-query query'')
        query-map' (merge {:in '[$ %]
                           :find '[(pull ?b [*])]}
                          query-map)]
    (if (:pretend options)
      (do (print "Query: ")
        (pprint/pprint query-map'))
      (q-and-print-results query-map' [db rules] options {:find find}))))
