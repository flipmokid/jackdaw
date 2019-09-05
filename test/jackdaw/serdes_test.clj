(ns jackdaw.serdes-test
  (:require [clojure.spec.alpha :as s]
            [clojure.test :refer :all]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [jackdaw.serdes :as js]))

(defspec edn-roundtrip-test 20
  (testing "EDN data is the same after serialization and deserialization."
    (prop/for-all [x (gen/fmap #(apply str %) (gen/vector gen/char-alpha 100))]
      (is (= x (->> (.serialize (js/edn-serializer) nil x)
                    (.deserialize (js/edn-deserializer) nil)))))))

(defspec edn-reverse-roundtrip-test 20
  (testing "EDN data is the same after deserialization and serialization."
    (prop/for-all [x (gen/fmap #(apply str %) (gen/vector gen/char-alpha 100))]
      (let [bytes (.serialize (js/edn-serializer) nil x)]
        (is (= (seq bytes)
               (seq (->> (.deserialize (js/edn-deserializer) nil bytes)
                         (.serialize (js/edn-serializer) nil)))))))))

(defspec edn-print-length-test 20
  (testing "EDN data is the same after serialization and deserialization with *print-length*."
    (binding [*print-length* 100]
      (prop/for-all [x (gen/vector gen/int (inc *print-length*))]
        (is (= x (->> (.serialize (js/edn-serializer) nil x)
                      (.deserialize (js/edn-deserializer) nil))))))))

(defmethod print-method java.net.URI
  [obj writer]
  (.write writer "#jackdaw/uri ")
  (.write writer (pr-str (str obj))))

(defspec edn-roundtrip-custom-reader 20
  (testing "custom EDN reader"
    (let [opts {:readers {'jackdaw/uri #(java.net.URI. %)}}]
      (prop/for-all [x (s/gen uri?)]
        (is (= x (->> (.serialize (js/edn-serializer) nil x)
                      (.deserialize (js/edn-deserializer opts) nil))))))))
