(defproject media-clj "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [com.drewnoakes/metadata-extractor "2.10.1"]]
  :main ^:skip-aot media-clj.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
