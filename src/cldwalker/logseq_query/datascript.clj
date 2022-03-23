(ns cldwalker.logseq-query.datascript
  (:require [datascript.core :as d]
            [datascript.transit :as dt]
            [datalog.parser :as parser]
            [babashka.fs :as fs]
            [clojure.set :as set]
            [clojure.string :as str]
            [clojure.edn :as edn]
            [clojure.pprint :as pprint]
            [cldwalker.logseq-query.cli :as cli]
            [cldwalker.logseq-query.util :as util]))

;; Misc util fns

(defn- get-graph-db*
  [graph]
  (when-let [file (or (util/get-graph-path graph)
                      ;; graph is a path
                      graph)]
    (when (fs/exists? file)
      (-> file slurp dt/read-transit-str))))

(defn- get-graph-db
  [graph]
  (when (nil? graph) (cli/error (str "--graph option required")))
  (or (get-graph-db* graph)
      (cli/error (str "No graph found for " (pr-str graph)))))

;; datalog util fns

;; From earlier version of datascript
(defn- query-vec->map
  "Convert query vec to query map and preserve insertion order with array-map"
  [query]
  (loop [parsed (array-map) key nil qs query]
    (if-let [q (first qs)]
      (if (keyword? q)
        (recur parsed q (next qs))
        (recur (update-in parsed [key] (fnil conj []) q) key (next qs)))
      parsed)))

(defn query-map->vec
  "Converts query map to vec"
  [query-map]
  (vec
   (reduce (fn [acc [k v]]
             (concat acc [k] v))
           '()
           query-map)))

(defn- find-rules-in-where
  "Given where clauses and a set of valid rules, returns rules found in where
  clause as keywords"
  [where valid-rules]
  (->> where
       flatten
       distinct
       (filter #(and (symbol? %) (contains? valid-rules (keyword %))))
       (map keyword)))

(defn- pull-or-single-binding?
  [find]
  (and (= 1 (count find))
       (or (not (coll? (first find)))
           (and (coll? (first find)) (= 'pull (ffirst find))))))

(defn- ensure-in-with-rules
  [{:keys [in] :as query-map}]
  (query-map->vec
   (if in
     (if (not (contains? (set in) '%))
       (update query-map :in conj '%)
       query-map)
     (merge (array-map :find (:find query-map)
                       :in ['$ '%])
            (dissoc query-map :find)))))

;; Common query fns

(def option-transformations
  {:tag-counts
   {:query {:find ['(pull ?b [* {:block/refs [:db/id :block/name]}])]}
    :result-transform (fn [result]
                        (->> result
                             (mapcat (fn [m] (map :block/name (:block/refs m))))
                             frequencies
                             (sort-by val >)))}})

(defn- process-query
  "Process query with options"
  [query options]
  (cond-> (query-vec->map query)
          ;; Would be helpful to fail if find is not ?b
          (:tag-counts options)
          (merge (get-in option-transformations [:tag-counts :query]))
          true
          ensure-in-with-rules))

(defn- process-query-m
  "Process top-level query map"
  [query-m options]
  (cond-> query-m
          (:tag-counts options)
          (assoc :result-transform
                 (get-in option-transformations [:tag-counts :result-transform]))
          true
          (update :query process-query options)))


(defn- print-table [rows table-command]
  (if table-command
    (-> (util/shell {:out :string} "echo" (pr-str rows))
        (util/shell table-command))
    (util/print-table rows)))

(defn print-results
  [rows options]
  (cond
    (:table options)
    (if (:block/uuid (first rows))
      (print-table (map #(merge {:id (:db/id %)} (:block/properties %))
                        rows)
                   (:table-command options))
      (print-table rows (:table-command options)))

    (:block-content options)
    (run! println (map #(->> % :block/content (str "- ")) rows))

    :else
    (cond
      (:puget options)
      (-> (util/shell {:out :string} "echo" (pr-str rows))
          (util/shell "puget"))
      (:raw options)
      rows
      :else
      (prn rows))))

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

(defn- get-rules-in-query
  [{:keys [where]}]
  (let [rules (util/get-all-rules)
        rules' (merge rules
                      ;; shortened names
                      (update-keys rules #(keyword (name %))))
        rules-found (find-rules-in-where where (-> rules' keys set))]
    (mapv (comp :rule rules') rules-found)))

(defn- print-logseq-query
  [{:keys [query result-transform]}]
  (let [{:keys [in] :as query-map} (query-vec->map query)
        rules (get-rules-in-query query-map)
        args-count (count (set/difference (set in) #{'% '$}))
        inputs (cond-> []
                 (pos? args-count)
                 (into (repeat args-count "TODO"))
                 true
                 (conj rules))
        export (cond-> {:query query
                        :inputs inputs}
                 result-transform
                 (assoc :result-transform result-transform))]
    (pprint/pprint export)))

;; q command

(defn- get-query [query-name]
  (let [queries (util/get-all-queries)
        all-queries (merge queries
                           ;; shortened names
                           (update-keys queries #(keyword (name %))))
        query-m (get all-queries (keyword query-name))
        _ (when-not query-m
            (cli/error "Error: No query found for" query-name))
        query (if (:parent query-m)
                (or (:query (get queries (:parent query-m)))
                    (cli/error (str "No query found for " (:parent query-m))))
                (:query query-m))]
    (assoc query-m :query query)))

(defn- validate-args [actual in]
  (let [expected-args (set/difference (set in) #{'% '$})]
    (when-not (= (count actual) (count expected-args))
      (cli/error "Wrong number of arguments"
                 (format "\nUsage: lq q %s" (str/join " " expected-args))))))

(defn- q*
  [{:keys [args-transform query] :as query-m} args options]
  (let [db (get-graph-db (:graph options))
        args (if (and (seq args) args-transform)
               [(eval (list args-transform (vec args)))]
               args)
        {:keys [find in] :as query-map} (query-vec->map query)
        actual-args (into [] (or (seq args) (:default-args query-m)))
        _ (validate-args actual-args in)
        rules (get-rules-in-query query-map)
        q-args (conj actual-args rules)]
    (parser/parse query)
    (wrap-query options
                (fn []
                  (q-and-print-results query
                                       (into [db] q-args)
                                       options
                                       {:find find
                                        :result-transform (:result-transform query-m)})))))

(defn q
  "Run a query given it's name and args. Takes options that are documented in
  tasks/q-cli-options. One additional option is :raw which when set to true
  returns the result and is useful for the repl "
  [{:keys [arguments options]}]
  (let [[query-name & args] arguments
        query-m (process-query-m (get-query query-name) options)]
    (if (:export options)
      (print-logseq-query query-m)
      (q* query-m args options))))

;; sq command

(defn- sq*
  [{:keys [query] :as query-m} options]
  (let [{:keys [find] :as query-map} (query-vec->map query)
        db (get-graph-db (:graph options))
        rules (get-rules-in-query query-map)]
    (parser/parse query)
    (wrap-query
     options
     (fn []
       (q-and-print-results query
                            [db rules]
                            options
                            {:find find
                             :result-transform (:result-transform query-m)})))))

(defn- expand-query
  [query]
  (let [query' (if (keyword? (first query))
                 query
                 (into [:where]
                       (if (and (vector? query) (not (symbol? (first query))))
                         query [query])))
        query-map (merge (array-map :find ['(pull ?b [*])])
                         (query-vec->map query'))]
    (query-map->vec query-map)))

(defn sq
  "Run a shorthand query. Takes options that are documented in
  tasks/sq-cli-options. One additional option is :raw which when set to true
  returns the result and is useful for the repl "
  [{:keys [arguments options]}]
  (let [query-string (str/join " " arguments)
        query (expand-query (edn/read-string query-string))
        query-m (process-query-m {:query query} options)]
    (if (:export options)
      (print-logseq-query query-m)
      (sq* query-m options))))
