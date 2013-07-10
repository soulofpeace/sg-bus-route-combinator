(ns sg-bus-router.core
  (:gen-class)
  (:require [clojure.xml :as xml]
            [clj-http.client :as client]
            [clojure.string :as string]
            [clojure.data.json :as json]
            )
  (:import (java.io ByteArrayInputStream))

  )

(def get-route (fn
                 ([bus-number] (:body (client/get (str "http://www.publictransport.sg/kml/busroutes/" bus-number ".kml" ) {:as :stream})))
                 ([bus-number direction] (:body (client/get (str "http://www.publictransport.sg/kml/busroutes/" bus-number "-" direction ".kml" ) {:as :stream})))
                 ))

(defn get-kml [kml-stream] 
    (xml-seq (xml/parse kml-stream)))

(defn join [memo text]  (str memo " " (first text)))

(defn to-double-tuple [raw] (vec (map 
                              #(Double/parseDouble (.trim %)) 
                              (string/split raw #",")
                              )))

(defn seq-to-double [str-seq] (map to-double-tuple str-seq))
(defn split-string [raw] (string/split (.trim raw) #"\s"))
(defn parse-xml [xml] (map #(:content %) (filter #(= :coordinates (:tag %)) xml)))

(defn coordinate [xml] (seq-to-double 
                         (split-string 
                           (reduce 
                             join 
                               ""
                               (parse-xml xml)))))


(defn dist [p1 p2] (let [[p1x p1y] p1
                         [p2x p2y] p2]
                     (Math/sqrt
                       (+
                        (- (Math/pow p2y 2) (Math/pow p1y 2))
                        (- (Math/pow p2x 2) (Math/pow p1x 2))
                        ))))
                          
(defn compare-with [start] 
  (fn compare-fn [p1 p2] (let [dist-1 (dist p1 start)
                               dist-2 (dist p2 start)]
                           (if (< dist-1 dist-2)
                             true
                             false))))

(defn to-json [direction sorted-seq] 
  (json/write-str
    {direction 
     {'route 
      (map #(string/join "," %) sorted-seq)
      }}))


(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  ;; work around dangerous default behaviour in Clojure
  (alter-var-root #'*read-eval* (constantly false))
  (println (get-route 7)))
