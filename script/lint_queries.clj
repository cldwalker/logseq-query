#!/usr/bin/env bb
(ns lint-queries
  "Lint datalog queries for parse-ability and unbound variables"
  (:require [datalog.parser :as parser]
            [dlint.core :as dlint]
            [clojure.string :as str]
            [cldwalker.logseq-query.util :as util]))

(defn- lint-query [[query-name {:keys [query]}]]
  (try (parser/parse query)
    {:success true :name query-name}
    (catch Exception e
      {:success false :name query-name :query query :error (.getMessage e)})))

(defn- allowed-unbound-symbol?
  [sym]
  (str/starts-with? (str sym) "?_"))

(defn- lint-unbound-query [[query-name {:keys [query]}]]
  (let [unbound-vars (->> (dlint/lint query)
                          (keep
                           (fn [[k v]]
                             (when-let [new-v (seq (remove allowed-unbound-symbol? v))]
                               [k (set new-v)])))
                          (into {}))]
    (if (seq unbound-vars)
      {:success false :name query-name :query query :unbound-vars unbound-vars}
      {:success true :name query-name})))

(defn -main [args]
  (let [queries (->> args
                     (map util/get-queries)
                     (apply merge)
                     (remove (fn [[_k v]] (nil? (:query v)))))
        invalid-queries (->> queries
                             (map lint-query)
                             (remove :success))
        invalid-unbound-queries (->> queries
                                     (map lint-unbound-query)
                                     (remove :success))
        lint-results (concat invalid-queries invalid-unbound-queries)]
    (if (seq lint-results)
      (do
        (println (count lint-results) "queries failed to lint:")
        (println lint-results)
        (System/exit 1))
      (println (count queries) "datalog queries linted fine!"))))

(when (= *file* (System/getProperty "babashka.file"))
  (-main *command-line-args*))
