(ns test-runner
  (:require [cljs.test :as t]
            [nbb.core :as nbb]
            [cldwalker.logseq-query.queries-test]
            [cldwalker.logseq-query.datascript-test]))

(defn init []
  (t/run-tests 'cldwalker.logseq-query.datascript-test
               'cldwalker.logseq-query.queries-test))

(when (= nbb/*file* (:file (meta #'init)))
  (init))
