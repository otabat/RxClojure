(ns rx.lang.clojure.future-test
  (:require [rx.lang.clojure.core :as rx]
            [rx.lang.clojure.blocking :as b]
            [rx.lang.clojure.future :as f])
  (:require [clojure.test :refer [deftest testing is]]))

(deftest test-future-generator
  (is (not= [(.getId (Thread/currentThread))]
            (b/into []
                      (f/future-generator f/default-runner
                        [observer]
                        (rx/on-next observer (.getId (Thread/currentThread))))))))

(deftest test-future
  (is (= [15] (b/into [] (f/future* f/default-runner + 1 2 3 4 5))))
  (is (= [15] (b/into [] (f/future f/default-runner (println "HI") (+ 1 2 3 4 5))))) )

(deftest test-future-exception
  (is (= "Caught: boo"
         (->> (f/future f/default-runner (throw (java.io.FileNotFoundException. "boo")))
              (rx/catch java.io.FileNotFoundException e
                (rx/return (str "Caught: " (.getMessage e))))
              (b/single)))))

(deftest test-future-cancel
  (let [exited? (atom nil)
        o (f/future f/default-runner
            (Thread/sleep 1000)
            (reset! exited? true)
            "WAT")
        result (->> o
                    (rx/take 0)
                    (b/into []))]
    (Thread/sleep 2000)
    (is (= [nil []]
           [@exited? result]))))

(deftest test-future-generator-cancel
  (let [exited? (atom nil)
        o (f/future-generator f/default-runner
            [o]
            (rx/on-next o "FIRST")
            (Thread/sleep 1000)
            (reset! exited? true))
        result (->> o
                    (rx/take 1)
                    (b/into []))]
    (Thread/sleep 2000)
    (is (= [nil ["FIRST"]]
           [@exited? result]))))

(deftest test-future-generator-exception
  (let [e (java.io.FileNotFoundException. "snake")]
    (is (= [1 2 e]
           (->> (f/future-generator
                  f/default-runner
                  [o]
                  (rx/on-next o 1)
                  (rx/on-next o 2)
                  (throw e))
                (rx/catch java.io.FileNotFoundException e
                  (rx/return e))
                (b/into []))))))
