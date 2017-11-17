(ns media-clj.core
  (:require [clojure.core.async :as a]
            [media-clj.files :as files]
            [media-clj.handler :as handler])
  (:import [java.text SimpleDateFormat]))

(comment
  (files/remove-dir "/home/andz/dev/projects/media-clj/out")
)

(defn format-with [timestamp formatter]
  (.format formatter timestamp))
(defn timestamp-format-fn [ts]
  (format-with ts (SimpleDateFormat. "YYYY/MM")))

(def =stop= (a/chan))
(def =events= (a/chan))
(def events-publication (a/pub =events= (fn [t] (:topic t))))

(def =statistics= (a/chan))
(def =errors= (a/chan))
(def =copy= (a/chan))

(a/sub events-publication :stats =statistics=)
(a/sub events-publication :errors =errors=)
(a/sub events-publication :copy =copy=)

(def stats (atom {:all 0,
                  :copied 0,
                  :duplicates 0,
                  :errors 0}))

(defn start-processing! [{:keys [source-path target-path target-format-fn],
                          :or {target-format-fn timestamp-format-fn}}]
  (handler/start-watch-errors =errors= stats)
  (handler/start-count-events =statistics= stats)
  (handler/start-watch-copy-files =copy= stats)
  (handler/stream-files =events= {:path source-path,
                                  :target-parent-path target-path,
                                  :lm-format-fn target-format-fn})
  {:stats stats})
