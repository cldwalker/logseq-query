(ns cldwalker.logseq-query.fs
  "Approximate nbb equivalent of babashka.fs. Instead of returning file objects,
most fns return strings"
  (:require ["fs" :as fs]
            ["os" :as os]
            ["path" :as path]
            [clojure.string :as str])
  (:refer-clojure :exclude [exists?]))

(defn expand-home
  [path]
  (str/replace-first path "~" (os/homedir)))

(defn list-dir
  "Unlike original list-dir, returns relative file names"
  [dir]
  (fs/readdirSync dir))

(defn exists?
  [file]
  (fs/existsSync file))

(defn directory?
  [file]
  (and (exists? file) (.isDirectory (fs/lstatSync file))))

(defn parent
  [file]
  (path/dirname file))

(defn glob
  "Traverses one level deep in dir and only supports '*' pattern. For deeper
  glob, look at fdir"
  [dir pattern]
  (->> (list-dir dir)
       (filter #(re-find (re-pattern (str/replace pattern "*" ".*")) %))
       (map #(str dir "/" %))))
