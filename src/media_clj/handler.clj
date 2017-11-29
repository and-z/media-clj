(ns media-clj.handler
  (:require [clojure.core.async :as a]
            [media-clj.files :as files]))

(defn start-watch-errors [ch a]
  (a/go-loop []
    (let [err (a/<! ch)]
      (println "ERROR: " err)
      (swap! a update-in [:errors] inc)
      (recur))))

(defn start-count-events [ch a]
  (a/go-loop []
    (a/<! ch)
    (swap! a update-in [:all] inc)
    (recur)))

(defn start-watchdog [_ stop-ch stats]
  (future
    (loop [s @stats]
      (let [{:keys [all copied]} s]
        (println (format "%s / %s" copied all))
        (if (and (< 0 all)
                 (= all copied))
          (a/put! stop-ch :quit)
          (recur @stats))))))

(defn start-watch-copy-files [in-ch stats]
  (a/go-loop []
    (let [{:keys [file]} (a/<! in-ch)]
      (future (files/safe-copy-file! file stats))
      #_(files/safe-copy-file! file stats)
      (recur))))

(defn stream-files [events-ch {:keys [path stats], :as options}]
  (a/go-loop [files (files/get-files path options)]
    (when-let [{:file/keys [extension name], :as file} (first files)]
      (if (= :ext-not-supported extension)
        (a/put! events-ch {:topic :errors :error (str name " has unsupported extension")})
        (do
          (swap! stats update-in [:all] inc)
          (a/>!! events-ch {:topic :copy :file file})))
      (recur (rest files)))))
