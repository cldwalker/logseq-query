#!/usr/bin/env bb
(ns datalog-lint
  "Lint default queries.edn and rules.edn"
  (:require [datalog.parser :as parser]
            [datalog.parser.impl :as parser-impl]
            [cldwalker.logseq-query.util :as util]))

(defn- lint-query [[query-name {:keys [query]}]]
  (try (parser/parse query)
    {:success true :name query-name :query query}
    (catch Exception e
      {:success false :name query-name :query query :error e})))

(defn- lint-rule [{:keys [rule]}]
  (try (parser-impl/parse-rule rule)
    {:success true :rule rule}
    (catch Exception e
      {:success false :rule rule :error e})))

(defn -main [_args]
  (let [invalid-queries (->> (util/get-queries)
                             (remove (fn [[_k v]] (keyword? (:query v))))
                             (map lint-query)
                             (remove :success))
        ;; TODO: Enable when https://github.com/lambdaforge/datalog-parser/issues/20 is addressed
        ;; Running into false positives do to fn usages
        ; invalid-rules (->> (util/get-rules)
        ;                    (map lint-rule)
        ;                    (remove :success))
        lint-results (concat invalid-queries #_invalid-rules)]
    (if (seq lint-results)
      (do
        (println (count lint-results) "failures for datalog linting:")
        (println lint-results)
        (System/exit 1))
      (println "Datalog queries linted fine!"))))

(when (= *file* (System/getProperty "babashka.file"))
  (-main *command-line-args*))
