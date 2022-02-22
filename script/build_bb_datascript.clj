#!/usr/bin/env bb

(ns build-bb-datascript
  "Builds version of bb with datascript enabled"
  (:require [babashka.tasks :refer [shell]]
            [babashka.fs :as fs]))

(defn- build-bb [bb-dir]
  (let [options {:dir bb-dir
                 :extra-env {"BABASHKA_XMX" (or (System/getenv "BABASHKA_XMX") "-J-Xmx8g")
                             "BABASHKA_FEATURE_DATASCRIPT" "true"}}]
    (shell options "script/uberjar")
    (shell options "script/compile")))

(defn -main [args]
  (let [[bb-dir] args]
    (build-bb bb-dir)
    (fs/copy (fs/path bb-dir "bb") "bin/bb-datascript")
    (println "Sucessfully built bb-datascript!")))

(when (= *file* (System/getProperty "babashka.file"))
  (-main *command-line-args*))
