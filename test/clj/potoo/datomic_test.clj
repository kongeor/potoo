(ns clj.potoo.datomic-test
  (:require [clojure.test :refer :all]
            [potoo.datomic :refer :all]
            [clj-time.core :as t]
            [clj-time.coerce :as c]))

(defn create-test-db []
  (let [datomic (new-database "datomic:mem://localhost:4334/test")]
    (-> (.start datomic) :db-conn)))

(deftest db-ops-test
  (testing "creating potoos"
    (let [conn (create-test-db)
          when (c/to-date (t/date-time 1986 10 14 4 3 27 456))
          _ (create-potoo conn "Foo Bar" "Rick" when)]
      (is (= #{["Foo Bar" "Rick" #inst "1986-10-14T04:03:27.456-00:00"]}
             (find-potoos conn))))))
