(ns cldwalker.logseq-query.util
  (:require [clojure.edn :as edn]
            [clojure.pprint :as pprint]
            [clojure.java.io :as io]
            [babashka.process :as p]
            [babashka.fs :as fs]))

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

(defn logseq-query-path
  [filename]
  ;; TODO: Fix
  (str "./" filename)
  #_(str (fs/path (fs/parent (fs/parent (System/getenv "_")))
                  filename)))

(defn get-rules
  ([] (get-rules "rules.edn"))
  ([file]
   (-> file logseq-query-path slurp edn/read-string)))

(defn get-queries
  ([] (get-queries "queries.edn"))
  ([file]
   (-> file logseq-query-path slurp edn/read-string)))

(defn get-config
  []
  (let [config-file (logseq-query-path "config.edn")]
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
  [rows]
  (pprint/print-table rows)
  (println "Total:" (count rows)))
