(defproject media-clj "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-RC1"]
                 [org.clojure/core.async "0.3.443"]]
  :main ^:skip-aot media-clj.main
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
