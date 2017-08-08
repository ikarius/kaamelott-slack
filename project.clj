(defproject kaamelott-slack "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 ; External libs
                 [environ "1.1.0"]
                 [funcool/cuerdas "2.0.3"]
                 [clj-http "3.6.1"]
                 [cheshire "5.7.1"]
                 ; Webserver
                 [org.immutant/web   "2.1.9"]
                 [compojure          "1.6.0"]
                 [ring/ring-defaults "0.3.1"]
                 [ring/ring-codec    "1.0.1"]
                 [ring/ring-devel    "1.6.2"]]

:profiles {:uberjar {:uberjar-name "kaamelott.jar"
                     :main kaamelott-slack.core
                     :aot :all}})
