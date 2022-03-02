(ns cldwalker.logseq-query.datascript-test
  (:require [clojure.test :refer [is deftest testing]]
            [cldwalker.logseq-query.cli :as cli]
            [cldwalker.logseq-query.datascript :as ld]))

(defn- q-error [args]
  (let [error (atom nil)]
    (with-redefs [cli/error (fn [& args]
                              (reset! error args)
                              (throw (ex-info "EXIT" {})))]
      (try (ld/q args)
           (catch clojure.lang.ExceptionInfo _))
      (first @error))))

(deftest q-test
  (testing "error when nil graph given"
    (is (= "--graph option required"
           (q-error {:options {:graph nil}}))))

  (testing "error when invalid graph given"
    (is (re-find #"No graph found"
                 (q-error {:options {:graph "blarg"}}))))

  (testing "error when no query found"
    (is (re-find #"No query found"
                 (q-error {:arguments ["blarg"]
                           :options {:graph "test/cldwalker/logseq_query/test-notes.json"}}))))

  (testing "error when wrong number of arguments given"
    (is (re-find #"Wrong number of arguments"
                 (q-error {:arguments ["content-search"]
                           :options {:graph "test/cldwalker/logseq_query/test-notes.json"}})))))
