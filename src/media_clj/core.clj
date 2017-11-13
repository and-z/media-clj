(ns media-clj.core
  (:gen-class)
  (:require [clojure.core.async :as a]
            [clojure.pprint]
            [clojure.java.io :as io])
  (:import [java.io BufferedInputStream FileInputStream]
           [java.text SimpleDateFormat]))


(defn path->file [path]
  (io/file path))

(def target-parent-path "/home/andz/dev/projects/media-clj/out")

(def sdm (SimpleDateFormat. "YYYY/MM"))

(defn format-with [timestamp formatter]
  (.format formatter timestamp))

(defn file->filemap [file]
  (let [name (.getName file)
        lm (.lastModified file)
        lm-formatted (-> lm (format-with sdm))]
    {:file file,
     :file/name name
     :file/last-modified-ts lm,
     :file/last-modified-date lm-formatted,
     :target-parent-path (str target-parent-path "/" lm-formatted),
     :extension-normalized (identity name)}))

(defn get-files [path]
  (->> (path->file path)
       (file-seq)
       (filter (fn [file] (.isFile file)))
       (map file->filemap)))

(comment
  (->> (get-files "/media/sf_share/test-media/source")
      (map :file/name)
      (clojure.pprint/pprint))

 (def files-ch (a/chan))

 (a/go
   (while true
     (let [file (a/<! files-ch)]
       (println "received" (:file/name file)))))

 (doseq [file (get-files "/media/sf_share/test-media/source")]
   (a/>!! files-ch file)))

;; media files processing pipeline
;; 1. list of all files from given root directory
;; 2. create filtered sequence of files to be moved // relevant for stats
;; 2.1. represent via
;;      {:file #file-object
;;       :file/last-edited #timestamp,
;;       :source-path "path/to/file",
;;       :target-parent-dir "path/to/parent/dir/" // will be concatenated with YYYY/MM/ pattern
;;       :target-path nil, // to be calculated based on :file/last-edited
;;       :normalized-extension #{jpg, mts, mp4}}
