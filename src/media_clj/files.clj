(ns media-clj.files
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.core.async :as a]))

;; dev helper
(defn remove-dir
  "Does not remove recursively."
  [path]
  (let [dir (io/file path)
        files (->> (file-seq dir)
                   (filter (fn [file] (.isFile file))))]
    (doseq [file files]
      (io/delete-file file))
    (io/delete-file dir)))

(defn path->file [path]
  (io/file path))

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

(defn copy-file! [source target stats]
  (io/make-parents target)
  (io/copy source target)
  (swap! stats update-in [:copied] inc))

(defn copy-with-new-name!
  [{:file/keys [target-path-fn name-short], :as file} stats]
  (let [new-filename (target-path-fn (format "%s-%d" name-short (System/currentTimeMillis)))
        target-file (io/file new-filename)]
    (copy-file! (:file file) target-file stats)
    (swap! stats update-in [:duplicates] inc)))

(defn safe-copy-file!
  [{:file/keys [target-path], :as file} stats]
  (let [target-file (io/file target-path)]
    (if (.exists target-file)
      (copy-with-new-name! file stats)
      (copy-file! (:file file) target-file stats))))

