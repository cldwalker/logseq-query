(ns cldwalker.logseq-query.tasks
  (:require [cldwalker.logseq-query.util :as util]
            [clojure.string :as str]
            [babashka.tasks :refer [clojure]]))

(defn rules
  []
  (->> (util/get-rules)
       (map #(hash-map :name (ffirst (:rule %))
                       :desc (:desc %)))
       util/print-table))

(defn q
  [& args]
  (let [args (cond-> {:query (first args)}
                     (seq (rest args))
                     (assoc :args (rest args))
                     (System/getenv "GRAPH")
                     (assoc :graph (System/getenv "GRAPH")))]
    (if (System/getenv "BABASHKA_DATASCRIPT")
      ((requiring-resolve 'cldwalker.logseq-query.datascript/q) args)
      (clojure (format "-X cldwalker.logseq-query.datascript/q '%s'"
                       (pr-str args))))))

(defn qs
  [& args]
  (let [args (cond-> {:query (str/join " " args)}
                     (System/getenv "GRAPH")
                     (assoc :graph (System/getenv "GRAPH")))]
    (if (System/getenv "BABASHKA_DATASCRIPT")
      ((requiring-resolve 'cldwalker.logseq-query.datascript/qs) args)
      (clojure (format "-X cldwalker.logseq-query.datascript/qs '%s'"
                       (pr-str args))))))

(defn graphs
  []
  (prn (map util/full-path->graph (util/get-graph-paths))))

(defn queries
  []
  (prn (keys (util/get-queries))))
