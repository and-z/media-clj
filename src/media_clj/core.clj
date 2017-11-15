(ns media-clj.core
  (:gen-class)
  (:require [clojure.core.async :as a]
            [clojure.string :as str]
            [clojure.pprint]
            [clojure.java.io :as io])
  (:import [java.util Date]
           [java.io BufferedInputStream FileInputStream]
           [java.text SimpleDateFormat]))

(defn path->file [path]
  (io/file path))

(defn format-with [timestamp formatter]
  (.format formatter timestamp))

(defn ext [filename]
  (let [extension (str/lower-case (subs filename (str/last-index-of filename \.)))]
    (if (contains? #{".jpg", ".jpeg", ".mts", ".mp4"} extension)
      extension)))

(defn drop-ext [filename]
  (subs filename 0 (str/last-index-of filename \.)))

(defn file->filemap [file {:keys [target-parent-path lm-format-fn]}]
  (let [name (.getName file)
        extension (ext name)
        lm (.lastModified file)
        lm-formatted (lm-format-fn lm)
        target-path-fn (fn [name-short]
                  (str (str/join "/" [target-parent-path lm-formatted name-short]) extension))]
    {:file file,
     :file/name name,
     :file/name-short (drop-ext name),
     :file/origin-path (.getAbsolutePath file),
     :file/last-modified-ts lm,
     :file/last-modified-date lm-formatted,
     :file/target-path (str/join "/" [target-parent-path lm-formatted name]),
     :file/target-path-fn target-path-fn,
     :file/target-parent-path (str target-parent-path "/" lm-formatted),
     :file/extension (or extension :ext-not-supported)}))

(defn get-files [path options]
  (->> (path->file path)
       (file-seq)
       (filter (fn [file] (.isFile file)))
       (map #(file->filemap % options))))

(defn remove-dir
  "Does not remove recursively."
  [path]
  (let [dir (io/file path)
        files (->> (file-seq dir)
                   (filter (fn [file] (.isFile file))))]
    (doseq [file files]
      (io/delete-file file))
    (io/delete-file dir)))

(comment
  (remove-dir "/home/andz/dev/projects/media-clj/out")
)

(defn watch-errors [ch a]
  (a/go-loop []
    (a/<! ch)
    (swap! a update-in [:errors] inc)
    (recur)))


(defn count-events [ch a]
  (a/go-loop []
    (a/<! ch)
    (swap! a update-in [:all] inc)
    (recur)))


(defn copy-file! [source target a]
  (io/make-parents target)
  (io/copy source target)
  (swap! a update-in [:copied] inc))

(defn safe-copy-file! [file a]
  (let [{:file/keys [target-path target-path-fn name-short]} file
        target-file (io/file target-path)]
    (if (.exists target-file)
      (let [new-filename (target-path-fn (format "%s-%d" name-short (System/currentTimeMillis)))]
        (copy-file! (:file file) (io/file new-filename) a)
        (swap! a update-in [:duplicates] inc))
      (copy-file! (:file file) target-file a))))

(defn watch-copy-files [ch a]
  (a/go-loop []
    (let [{:keys [file]} (a/<! ch)]
      (safe-copy-file! file a)
      (recur))))

(defn stream-files [ch {:keys [path], :as options}]
  (doseq [{:file/keys [extension name], :as file} (get-files path options)]
    (if (= :ext-not-supported extension)
      (a/go (a/>! ch {:topic :errors :error (str name " has unsupported extension")}))
      (a/go
        (a/>! ch {:topic :stats})
        (a/>! ch {:topic :copy :file file})))))

(defn format-with [timestamp formatter]
  (.format formatter timestamp))

;;; App configuration
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

(watch-errors =errors= stats)
(count-events =statistics= stats)
(watch-copy-files =copy= stats)

(stream-files =events= {:path "/media/sf_share/test-media/source",
                        :target-parent-path "/home/andz/dev/projects/media-clj/out",
                        :lm-format-fn #(format-with % (SimpleDateFormat. "YYYY/MM"))})
