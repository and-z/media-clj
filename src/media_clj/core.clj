(ns media-clj.core
  (:gen-class)
  (:require [clojure.java.io :as io])
  (:import [java.io BufferedInputStream FileInputStream]
           [com.drew.imaging ImageMetadataReader]))

(defn get-meta [filename]
  (let [logo (io/resource filename)
        metadata (ImageMetadataReader/readMetadata (io/file logo))]
    metadata))

(defn get-directories [metadata]
  (let [m metadata]
    (for [d (.getDirectories m)]
      d)))

(defn get-tags [directory]
  (for [t (.getTags directory)]
    {;:tag.type (.getTagType t),
     :tag.description (.getDescription t),
     :tag.name (.getTagName t)}))

(defn get-errors [directory]
  (for [e (.getErrors directory)]
    e))

(->> (get-meta "clojure-logo.png")
    (get-directories)
    (map (fn [d]
           {:directory.name (.getName d),
            :tags (get-tags d)
            :errors (get-errors d)}))
    (clojure.pprint/pprint))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
