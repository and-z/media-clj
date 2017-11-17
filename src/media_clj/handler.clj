(ns media-clj.handler
  (:require [clojure.core.async :as a]
            [media-clj.files :as files]))

(defn start-watch-errors [ch a]
  (a/go-loop []
    (a/<! ch)
    (swap! a update-in [:errors] inc)
    (recur)))

(defn start-count-events [ch a]
  (a/go-loop []
    (a/<! ch)
    (swap! a update-in [:all] inc)
    (recur)))

(defn start-watch-copy-files [ch a]
  (a/go-loop []
    (let [{:keys [file]} (a/<! ch)]
      #_(future (files/safe-copy-file! file a))
      (files/safe-copy-file! file a)
      (recur))))

(defn stream-files [ch {:keys [path], :as options}]
  (doseq [{:file/keys [extension name], :as file} (files/get-files path options)]
    (if (= :ext-not-supported extension)
      (a/put! ch {:topic :errors :error (str name " has unsupported extension")})
      (do
        (a/put! ch {:topic :stats})
        (a/put! ch {:topic :copy :file file})))))
