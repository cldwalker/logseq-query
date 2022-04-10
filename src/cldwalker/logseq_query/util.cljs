(ns cldwalker.logseq-query.util
  (:require [clojure.pprint :as pprint]
            [clojure.edn :as edn]
            ["fs" :as node-fs]
            [nbb.core :as nbb]
            [cldwalker.logseq-query.logseq-rules :as rules]
            [cldwalker.logseq-query.fs :as fs]))

;; Misc utils

(defn print-table
  [rows & {:keys [fields]}]
  (if fields (pprint/print-table fields rows) (pprint/print-table rows))
  (println "Total:" (count rows)))

(defn slurp
  "Like clojure.core/slurp"
  [file]
  (str (node-fs/readFileSync file)))

(defn- resource
  [file-name]
  (str (fs/parent nbb/*file*) "/../../../resources/" file-name))

;; Config fns

(defn read-config-file
  [file]
  (if (fs/exists? file)
    (-> file slurp edn/read-string)
    {}))

(defn- get-logseq-rules
  []
  (let [descs {:block-content "Blocks that have given string in :block/content"
               :has-property "Blocks that have given property"
               :has-page-property "Pages that have given property"
               :page-property "Pages that have property equal to value or that contain the value"
               :page-ref "Blocks associated to given page/tag"
               :task "Tasks that contain one of markers"}]
    ;; TODO: Debug issues with upstream property
    (->> (dissoc rules/query-dsl-rules :property)
         (map (fn [[k v]]
                [(keyword "logseq" (name k))
                 {:rule v :desc (descs k)}]))
         (into {}))))

(defn get-all-rules
  []
  (merge (get-logseq-rules)
         (-> "rules.edn" resource slurp edn/read-string)
         (read-config-file (str (fs/expand-home "~/.lq/rules.edn")))))

(defn get-all-queries
  []
  (merge (-> "queries.edn" resource slurp edn/read-string)
         (read-config-file (str (fs/expand-home "~/.lq/queries.edn")))))

(defn get-config
  []
  (read-config-file (str (fs/expand-home "~/.lq/config.edn"))))

;; Graph fns

(defn get-graph-paths
  []
  (let [dir (fs/expand-home "~/.logseq/graphs")]
    (when (fs/directory? dir)
      (fs/glob dir "*.transit"))))

(defn full-path->graph
  [path]
  (second (re-find #"\+\+([^\+]+).transit$" path)))

(defn get-graph-path
  [graph]
  (some #(when (= graph (full-path->graph %)) %)
        (get-graph-paths)))
