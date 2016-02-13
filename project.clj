(defproject potoo "0.1.0-SNAPSHOT"
  :description "A mini twitter clone backend using clojure and datomic"
  :url "https://github.com/kongeor/potoo"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [com.datomic/datomic-free "0.9.5344"]
                 [com.stuartsierra/component "0.3.1"]
                 [ring/ring "1.4.0"]
                 [ring/ring-json "0.4.0"]
                 [bidi "1.25.0"]
                 [com.taoensso/timbre "4.2.1"]]

  :source-paths ["src/clj"]
  :main potoo.system
  :aot [potoo.system]
  :min-lein-version "2.0.0")
