(ns speclj.platform
  (:require [clojure.java.io :as io]
            [clojure.string :as string :refer [split]]))

(def endl (System/getProperty "line.separator"))
(def file-separator (System/getProperty "file.separator"))

(defn re? [obj] (isa? (type obj) java.util.regex.Pattern))

(def throwable Throwable)
(def exception java.lang.Exception)
(def failure speclj.SpecFailure)
(def pending speclj.SpecPending)

(defn pending? [e] (isa? (type e) pending))
(defn failure? [e] (isa? (type e) failure))

(defn- classname->filename [classname]
  (let [root-name (first (split classname #"\$"))]
    (str
      (string/replace root-name "." file-separator)
      ".clj")))


(declare ^:dynamic *bound-by-should-invoke*)

(defn bound-by-should-invoke? []
  (and (bound? #'*bound-by-should-invoke*)
       *bound-by-should-invoke*))

(defn difference-greater-than-delta? [expected actual delta]
    (> (.abs (- (bigdec expected) (bigdec actual))) (.abs (bigdec delta))))

(defn error-message [e] (.getMessage e))
(defn stack-trace [e] (seq (.getStackTrace e)))
(defn cause [e] (.getCause e))
(defn print-stack-trace [e]
  (.printStackTrace e (java.io.PrintWriter. *out* true)))

(defn failure-source [exception]
  (let [source (nth (.getStackTrace exception) 0)
        classname (.getClassName source)
        filename (classname->filename classname)]
    (if-let [url (io/resource filename)]
      (str (.getFile url) ":" (.getLineNumber source))
      (str filename ":" (.getLineNumber source)))))

(defn elide-level? [stack-element]
  (let [classname (.getClassName stack-element)]
    (or
      (.startsWith classname "clojure.")
      (.startsWith classname "speclj.")
      (.startsWith classname "java."))))

(defn type-name [t] (.getName t))

(def seconds-format (java.text.DecimalFormat. "0.00000"))
(defn format-seconds [secs] (.format seconds-format secs))
(defn current-time [] (System/nanoTime))
(defn secs-since [start] (/ (double (- (System/nanoTime) start)) 1000000000.0))

(defn dynamically-invoke [ns-name fn-name]
  (let [ns-sym (symbol ns-name)
        fn-sym (symbol (str ns-name "/" fn-name))
        expr `(do (require '~ns-sym) (~fn-sym))]
    (eval expr)))

(def new-line 10)

(defn- read-in []
  (when (.ready *in*)
    (.read *in*)))

(defn enter-pressed? []
  (= (read-in) new-line))
