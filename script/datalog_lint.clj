#!/usr/bin/env bb
(ns datalog-lint
  "Lint default queries.edn and rules.edn"
  (:require [datalog.parser :as parser]
            [datalog.parser.impl :as parser-impl]
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

(defn- lint-unbound-rule [rule]
  (->> (dlint/lint [rule])
       (keep
        (fn [[k v]]
          (when-let [new-v (seq (remove allowed-unbound-symbol? v))]
            {:success false :name k :rule rule :unbound-vars (set new-v)})))))

(defn- lint-rule [{:keys [rule]}]
  (try (parser-impl/parse-rule rule)
    {:success true :rule rule}
    (catch Exception e
      {:success false :rule rule :error (.getMessage e)})))

(defn -main [_args]
  (let [queries (->> (util/get-queries)
                     (remove (fn [[_k v]] (keyword? (:query v)))))
        invalid-queries (->> queries
                             (map lint-query)
                             (remove :success))
        invalid-unbound-queries (->> queries
                                     (map lint-unbound-query)
                                     (remove :success))
        invalid-unbound-rules (->> (util/get-rules)
                                   (map :rule)
                                   (mapcat lint-unbound-rule)
                                   (remove :success))
        ;; TODO: Enable when https://github.com/lambdaforge/datalog-parser/issues/20 is addressed
        ;; Running into false positives do to fn usages
        ; invalid-rules (->> (util/get-rules)
        ;                    (map lint-rule)
        ;                    (remove :success))
        lint-results (concat invalid-queries invalid-unbound-queries invalid-unbound-rules #_invalid-rules)]
    (if (seq lint-results)
      (do
        (println (count lint-results) "failures for datalog linting:")
        (println lint-results)
        (System/exit 1))
      (println "Datalog queries linted fine!"))))

(when (= *file* (System/getProperty "babashka.file"))
  (-main *command-line-args*))
