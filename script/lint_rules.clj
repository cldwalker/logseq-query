#!/usr/bin/env bb
(ns lint-rules
  "Lint datalog rules for parse-ability and unbound variables"
  (:require [datalog.parser.impl :as parser-impl]
            [dlint.core :as dlint]
            [clojure.string :as str]
            [clojure.edn :as edn]
            [babashka.fs :as fs]))

(defn- read-config-file
  [file]
  (if (fs/exists? file)
    (-> file slurp edn/read-string)
    {}))

(defn- allowed-unbound-symbol?
  [sym]
  (str/starts-with? (str sym) "?_"))

(defn- lint-unbound-rule [rule]
  (->> (dlint/lint [rule])
       (keep
        (fn [[k v]]
          (when-let [new-v (seq (remove allowed-unbound-symbol? v))]
            {:success false :name k :rule rule :unbound-vars (set new-v)})))))

(defn- lint-rule [rule]
  (try (parser-impl/parse-rule rule)
    {:success true :rule rule}
    (catch Exception e
      {:success false :rule rule :error (.getMessage e)})))

(defn -main [args]
  (let [rules (->> args
                   (map read-config-file)
                   (apply merge)
                   vals
                   (map :rule))
        invalid-unbound-rules (->> rules
                                   (mapcat lint-unbound-rule)
                                   (remove :success))
        invalid-rules (->> rules
                           (map lint-rule)
                           (remove :success))
        lint-results (concat invalid-unbound-rules invalid-rules)]
    (if (seq lint-results)
      (do
        (println (count lint-results) "rules failed to lint:")
        (println lint-results)
        (System/exit 1))
      (println (count rules) "datalog rules linted fine!"))))

(when (= *file* (System/getProperty "babashka.file"))
  (-main *command-line-args*))
