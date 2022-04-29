(ns test-runner
  (:require [cljs.test :as t]
            [nbb.core :as nbb]
            [cldwalker.logseq-query.queries-test]
            [cldwalker.logseq-query.datascript-test]))

(defn init []
  (let [{:keys [error fail]}
        (t/run-tests 'cldwalker.logseq-query.datascript-test
                     'cldwalker.logseq-query.queries-test)]
    (when (pos? (+ error fail))
      (throw (ex-info "Tests failed" {:babashka/exit 1})))))

(when (= nbb/*file* (:file (meta #'init)))
  (init))
