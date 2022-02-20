(ns cldwalker.logseq-query.queries-test
  "These are high level tests for queries in queries.edn. These tests use the
  logseq-query-notes graph."
  (:require [clojure.test :refer [is deftest]]
            [cldwalker.logseq-query.datascript :as ld]))

(defn- q [& args]
  (with-redefs [ld/print-results (fn [result _] result)]
    (ld/q {:arguments args
           :options {:graph "test/cldwalker/logseq_query/test-notes.transit"}})))

(deftest block-property
  (is (= #{{:type "comment" :desc "hi"} {:type "comment" :desc "hola"}}
         (set (map :block/properties (q "block-property" "type" "comment"))))))
