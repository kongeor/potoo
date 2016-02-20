(ns potoo.datomic-test
  (:require [clojure.test :refer :all]
            [potoo.datomic :refer :all]
            [datomic.api :as d]
            [clj-time.core :as t]
            [clj-time.coerce :as c]))

(defn create-test-db []
  (let [uri "datomic:mem://localhost:4334/test"
        _ (d/delete-database uri)
        datomic (new-empty-database uri)]
    (-> (.start datomic) :db-conn)))

(deftest user-tests
  (testing "creating a user"
    (let [conn (create-test-db)
          _    (create-user conn "Morty" "Morty")]
      (is (find-user-id conn "Morty"))
      (is (not (find-user-id conn "Rick"))))))

(deftest db-ops-test
  (testing "creating potoos"
    (let [conn (create-test-db)
          date (c/to-date (t/date-time 1986 10 14 4 3 27 456))
          _    (create-user conn "Morty" "Morty")
          _ (create-potoo conn "Foo Bar" "Morty" date)]
      (is (= #{["Foo Bar" "Morty" #inst "1986-10-14T04:03:27.456-00:00"]}
             (find-potoos conn)))))
  (testing "finding potoos for user"
    (let [conn (create-test-db)
          date (c/to-date (t/date-time 1986 10 14 4 3 27 456))
          _ (create-user conn "Morty" "")
          _ (create-user conn "Rick" "")
          _ (create-potoo conn "Foo" "Rick" date)
          _ (create-potoo conn "Bar" "Rick" date)
          _ (create-potoo conn "Baz" "Morty" date)]
      (is (= #{["Foo" #inst "1986-10-14T04:03:27.456-00:00"]
               ["Bar" #inst "1986-10-14T04:03:27.456-00:00"]}
             (find-potoos-for-user conn "Rick"))))))

