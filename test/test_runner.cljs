(ns test-runner
  (:require [cljs.test :as t]
            [nbb.core :as nbb]
            [cldwalker.logseq-query.queries-test]
            [cldwalker.logseq-query.datascript-test]))

(defmethod t/report [::t/default :end-run-tests] [{:keys [error fail]}]
  (if (pos? (+ error fail))
    (js/process.exit 1)
    (js/process.exit 0)))

(defn init []
  (t/run-tests 'cldwalker.logseq-query.datascript-test
               'cldwalker.logseq-query.queries-test))

(when (= nbb/*file* (:file (meta #'init)))
  (init))
