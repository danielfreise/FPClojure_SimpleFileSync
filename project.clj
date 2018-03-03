(defproject filesync "0.1.0-SNAPSHOT"
  :description "Clojure project for the course Functional Programming."
  :url "https://bitbucket.org/Suddoha/file-synchronization-clojure-project"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [
                  [org.clojure/clojure "1.8.0"]
                  [org.clojure/core.async "0.2.374"]
                  [proto-repl "0.3.1"]
                  [byte-streams "0.2.3"]
                  [nio "1.0.4"]]
  :main filesync.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
