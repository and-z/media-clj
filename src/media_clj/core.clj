(ns media-clj.core
  (:require [clojure.core.async :as a]
            [media-clj.files :as files]
            [media-clj.handler :as handler])
  (:import [java.text SimpleDateFormat]))

(defn format-with [timestamp formatter]
  (.format formatter timestamp))
(defn timestamp-format-fn [ts]
  (format-with ts (SimpleDateFormat. "YYYY/MM")))

(def =stop= (a/chan 1))
(def =events= (a/chan 4))
(def events-publication (a/pub =events= (fn [t] (:topic t))))

(def =statistics= (a/chan))
(def =errors= (a/chan))
(def =copy= (a/chan 2))
(def =copy-finished= (a/chan))

(a/sub events-publication :stats =statistics=)
(a/sub events-publication :errors =errors=)
(a/sub events-publication :copy =copy=)
(a/sub events-publication :copy-finished =copy-finished=)

(def stats (atom {:all 0,
                  :copied 0,
                  :duplicates 0,
                  :errors 0}))

(defn start-processing! [{:keys [source-path target-path target-format-fn],
                          :or {target-format-fn timestamp-format-fn}}]
  (comment
    (handler/start-watch-errors =errors= stats)
    (handler/start-count-events =statistics= stats))

  (handler/start-watchdog =copy-finished= =stop= stats)
  (handler/start-watch-copy-files =copy= stats)
  (handler/stream-files =events= {:stats stats
                                  :path source-path,
                                  :target-parent-path target-path,
                                  :lm-format-fn target-format-fn})

  {:stats stats,
   :stop-ch =stop=})
