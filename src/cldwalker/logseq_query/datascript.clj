(ns cldwalker.logseq-query.datascript
  (:require [datascript.core :as d]
            [datascript.transit :as dt]
            [datalog.parser :as parser]
            [clojure.set :as set]
            [clojure.string :as str]
            [clojure.edn :as edn]
            [clojure.pprint :as pprint]
            [babashka.tasks :refer [shell]]
            [cldwalker.logseq-query.cli :as cli]
            [cldwalker.logseq-query.util :as util]))

(defn- get-graph-db
  [graph]
  (let [file (some #(when (= graph (util/full-path->graph %)) %)
                   (util/get-graph-paths))]
    (or (some-> file slurp dt/read-transit-str)
        (cli/error (str "No graph found for " (pr-str graph))))))

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
  (cond
    (:table options)
    (if (:block/uuid (first rows))
      (print-table (map #(merge {:id (:db/id %)} (:block/properties %))
                        rows))
      (print-table rows))

    (:puget options)
    (-> (shell {:out :string} "echo" (pr-str rows))
        (shell "puget"))
    (:block-content options)
    (run! println (map #(->> % :block/content (str "- ")) rows))
    :else
    (prn rows)))

(defn- q-and-print-results
  [query query-args options {:keys [find result-transform]}]
  (let [post-transduce (if (pull-or-single-binding? find)
                         (map #(let [e (first %)]
                                 (if (map? e)
                                   ;; Dissoc order until I see why it's causing an error later
                                   (dissoc e :block/properties-order)
                                   e)))
                         (map identity))
        res (apply d/q query query-args)
        res' (cond-> (into [] post-transduce res)
                     (:count options)
                     count
                     result-transform
                     ((fn [x] (eval (list result-transform x)))))]
    (print-results res' options)))

(defn- wrap-query
  "To optionally silence unhelpful db barf for certain d/q"
  [options f]
  (if (:silence options)
    (try (f)
      (catch Exception e (prn (.getMessage e))))
    (f)))

(defn q
  [{:keys [arguments options]}]
  (let [[query-id & args] arguments
        ; args [#{"TODO" "DOING"}]
        {:keys [graph]} options
        graph' (or graph (:default-graph (util/get-config)))
        db (get-graph-db graph')
        queries (util/get-queries)
        {:keys [args-transform] :as query-m} (get queries (keyword query-id))
        _ (when-not query-m
            (println "Error: No query found for" query-id)
            (System/exit 1))
        query (if (keyword? (:query query-m))
                (or (:query (get queries (:query query-m)))
                    (cli/error (str "No query found for " (:query query-m))))
                (:query query-m))
        args (if (and (seq args) args-transform)
               [(eval (list args-transform (vec args)))]
               args)
        {:keys [find in]} (parse-query query)
        expected-args (set/difference (set in) #{'% '$})
        actual-args (into [] (or (seq args) (:default-args query-m)))
        _ (when-not (= (count actual-args) (count expected-args))
            (println "Error: Wrong number of arguments")
            (println (format "Usage: lq q %s" (str/join " " expected-args)))
            (System/exit 1))
        rules (map :rule (util/get-rules))
        q-args (conj actual-args rules)]
    (parser/parse query)
    (wrap-query options
                (fn []
                  (q-and-print-results query
                                       (into [db] q-args)
                                       options
                                       {:find find
                                        :result-transform (:result-transform query-m)})))))

(defn- add-find-and-in-defaults
  "Adds defaults to :find and :in if they are not present in vec query"
  [query]
  (case (first query)
    :where
    (into [:find '(pull ?b [*]) :in '$ '%]
          query)
    :in
    (into [:find '(pull ?b [*])] query)
    :find
    (if (= :in (nth query 2))
      query
      (apply concat
        [(take 2 query)
         [:in '$ '%]
         (drop 2 query)]))))

(defn qs
  [{:keys [arguments options]}]
  (let [query-string (str/join " " arguments)
        graph (or (:graph options) (:default-graph (util/get-config)))
        db (get-graph-db graph)
        rules (map :rule (util/get-rules))
        query (edn/read-string query-string)
        query' (add-find-and-in-defaults
                (if (keyword? (first query)) query (conj [:where] query)))
        {:keys [find]} (parse-query query')]
    (parser/parse query')
    (if (:pretend options)
      (do (print "Query: ")
        (pprint/pprint query'))
      (wrap-query
       options
       (fn []
         (q-and-print-results query' [db rules] options {:find find}))))))
