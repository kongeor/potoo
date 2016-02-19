(defproject potoo "0.1.0-SNAPSHOT"
  :description "A mini twitter clone backend using clojure and datomic"
  :url "https://github.com/kongeor/potoo"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.170"]
                 [reagent "0.6.0-alpha"]
                 [cljs-ajax "0.5.3"]
                 [com.datomic/datomic-free "0.9.5344"]
                 [com.stuartsierra/component "0.3.1"]
                 [ring/ring "1.4.0"]
                 [ring/ring-json "0.4.0"]
                 [bidi "1.25.0"]
                 [com.taoensso/timbre "4.2.1"]]
  :source-paths ["src/clj"]
  :main potoo.system
  :min-lein-version "2.0.0"
  :plugins [[lein-figwheel "0.5.0-1"]
            [lein-cljsbuild "1.1.2"]]
  :clean-targets [:target-path "out"]
  :uberjar-name "potoo-standalone.jar"
  :profiles {:uberjar {:aot :all}}
  :prep-tasks ["compile" ["cljsbuild" "once" "min"]]
  :cljsbuild {
              :builds [{:id "dev"
                        :source-paths ["src/cljs" "src/cljs_dev"]
                        :figwheel true
                        :compiler {:main "dev.dev"
                                   :asset-path "js/out"
                                   :output-to "resources/public/js/app.js"
                                   :output-dir "resources/public/js/out"
                                   :optimizations :none
                                   :source-map true
                                   :source-map-timestamp true
                                   :cache-analysis true
                                   }}
                       {:id "min"
                        :source-paths ["src/cljs"]
                        :compiler {:output-to "resources/public/js/app.js"
                                   :optimizations :advanced
                                   :pretty-print false}}]
              }
  )
