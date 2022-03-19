(ns cldwalker.logseq-query.datascript-test
  (:require [clojure.test :refer [is deftest testing]]
            [cldwalker.logseq-query.cli :as cli]
            [cldwalker.logseq-query.datascript :as ld]
            [clojure.edn :as edn]))

(defn- q-error [args]
  (let [error (atom nil)]
    (with-redefs [cli/error (fn [& args]
                              (reset! error args)
                              (throw (ex-info "EXIT" {})))]
      (try (ld/q args)
           (catch clojure.lang.ExceptionInfo _))
      (first @error))))

(deftest q-errors
  (testing "error when nil graph given"
    (is (= "--graph option required"
           (q-error {:arguments ["property-all"]
                     :options {:graph nil}}))))

  (testing "error when invalid graph given"
    (is (re-find #"No graph found"
                 (q-error {:arguments ["property-all"]
                           :options {:graph "blarg"}}))))

  (testing "error when no query found"
    (is (re-find #"No query found"
                 (q-error {:arguments ["blarg"]
                           :options {:graph "test/cldwalker/logseq_query/test-notes.json"}}))))

  (testing "error when wrong number of arguments given"
    (is (re-find #"Wrong number of arguments"
                 (q-error {:arguments ["content-search"]
                           :options {:graph "test/cldwalker/logseq_query/test-notes.json"}})))))

(deftest export-option
  (is (= {:query '[:find (pull ?b [*]) :in $ ?query % :where (block-content ?b ?query)]
          :inputs '[[[(block-content ?b ?query)
                      [?b :block/content ?content]
                      [(clojure.string/includes? ?content ?query)]]]]}
         (edn/read-string
          (with-out-str
            (ld/q {:arguments ["content-search"] :options {:export true}}))))
      "Works with q")

  (is (= {:query '[:find (pull ?b [*]) :in $ % :where (block-content ?b "wow")]
          :inputs '[[[(block-content ?b ?query)
                      [?b :block/content ?content]
                      [(clojure.string/includes? ?content ?query)]]]]}
         (edn/read-string
          (with-out-str
            (ld/sq {:arguments ["(block-content ?b \"wow\")"] :options {:export true}}))))
      "Works with sq"))
