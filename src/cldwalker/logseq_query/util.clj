(ns cldwalker.logseq-query.util
  (:require [clojure.edn :as edn]
            [clojure.pprint :as pprint]
            [clojure.java.io :as io]
            [babashka.process :as p]
            [babashka.fs :as fs]
            [frontend.db.rules :as rules]))

(def default-opts
  {:in :inherit
   :out :inherit
   :err :inherit
   :shutdown p/destroy-tree})

(defn shell
  "Copied from babashka.impl.tasks/shell"
  [cmd & args]
  (let [[prev cmd args]
        (if (and (map? cmd)
                 (:proc cmd))
          [cmd (first args) (rest args)]
          [nil cmd args])
        [opts cmd args]
        (if (map? cmd)
          [cmd (first args) (rest args)]
          [nil cmd args])
        opts (if-let [o (:out opts)]
               (if (string? o)
                 (update opts :out io/file)
                 opts)
               opts)
        opts (if-let [o (:err opts)]
               (if (string? o)
                 (update opts :err io/file)
                 opts)
               opts)
        opts (if prev
               (assoc opts :in nil)
               opts)
        cmd (if (.exists (io/file cmd))
              [cmd]
              (p/tokenize cmd))
        cmd (into cmd args)]
    @(p/process prev cmd (merge default-opts opts))))

(defn get-rules
  [file]
  (if (fs/exists? file)
    (-> file slurp edn/read-string)
    []))

(defn- get-logseq-rules
  []
  (let [descs {:block-content "Blocks that have given string in :block/content"
               :has-property "Blocks that have given property"
               :has-page-property "Pages that have given property"
               :page-property "Pages that have property equal to value or that contain the value"
               :page-ref "Blocks associated to given page/tag"
               :task "Tasks that contain one of markers"}]
    ;; TODO: Debug issues with upstream property
    ;; TODO: May need to page page-ref upstream
    (->> (dissoc rules/query-dsl-rules :property :page-ref)
         (map (fn [[k v]]
                [(keyword "logseq" (name k))
                 {:rule v :desc (descs k)}]))
         (into {}))))

(defn get-all-rules
  []
  (merge (get-logseq-rules)
         (-> "rules.edn" io/resource slurp edn/read-string)
         (get-rules (str (fs/expand-home "~/.lq/rules.edn")))))

(defn get-queries
  [file]
  (if (fs/exists? file)
    (-> file slurp edn/read-string)
    {}))

(defn get-all-queries
  []
  (merge (-> "queries.edn" io/resource slurp edn/read-string)
         (get-queries (str (fs/expand-home "~/.lq/queries.edn")))))

(defn get-config
  []
  (let [config-file (str (fs/expand-home "~/.lq/config.edn"))]
    (if (fs/exists? config-file)
      (-> config-file slurp edn/read-string)
      {})))

(defn get-graph-paths
  []
  (let [dir (fs/expand-home "~/.logseq/graphs")]
    (when (fs/directory? dir)
      (map str (fs/glob dir "*.transit")))))

(defn full-path->graph
  [path]
  (second (re-find #"\+\+([^\+]+).transit$" path)))

(defn get-graph-path
  [graph]
  (some #(when (= graph (full-path->graph %)) %)
        (get-graph-paths)))

(defn print-table
  [rows & {:keys [fields]}]
  (if fields (pprint/print-table fields rows) (pprint/print-table rows))
  (println "Total:" (count rows)))
