(ns cldwalker.logseq-query.util
  (:require [clojure.pprint :as pprint]
            [clojure.edn :as edn]
            [clojure.string :as str]
            ["fs" :as node-fs]
            ["child_process" :as child-process]
            [nbb.classpath :as classpath]
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
  (some #(when (node-fs/existsSync (str % "/" file-name))
           (str % "/" file-name))
        (str/split (classpath/get-classpath) #":")))

(defn update-keys
  "Not in cljs yet. One day this can ripped out. Maps function `f` over the keys
  of map `m` to produce a new map."
  [m f]
  (reduce-kv
   (fn [m_ k v]
     (assoc m_ (f k) v)) {} m))

(defn sh
  "Run shell cmd synchronously and print to inherited streams by default. Aims
  to be similar to babashka.tasks/shell"
  [cmd opts]
  (child-process/spawnSync (first cmd)
                           (clj->js (rest cmd))
                           (clj->js (merge {:stdio "inherit"} opts))))

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
         (when-not js/process.env.LQ_DISABLE_GLOBAL
           (read-config-file (str (fs/expand-home "~/.lq/rules.edn"))))))

(defn get-all-queries
  []
  (merge (-> "queries.edn" resource slurp edn/read-string)
         (when-not js/process.env.LQ_DISABLE_GLOBAL
           (read-config-file (str (fs/expand-home "~/.lq/queries.edn"))))))

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
