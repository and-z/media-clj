(ns media-clj.main
  (:require [clojure.core.async :as a]
            [clojure.pprint :as pp]
            [media-clj.core :as core])
  (:gen-class))

(defn print-stats [a]
  (future
    (while true
     (pp/pprint @a)
     (Thread/sleep 1000))))

(defn -main []
  (println "Start processing...")
  (let [{:keys [stats]} (core/start-processing! {:source-path "/media/sf_share/test-media/source",
                                        :target-path "/home/andz/dev/projects/media-clj/out"})]
    (print-stats stats)
    (println "Main App finished.")))
