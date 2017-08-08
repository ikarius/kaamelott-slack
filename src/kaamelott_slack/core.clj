(ns kaamelott-slack.core
  (:require [environ.core :refer [env]]
            [cuerdas.core :as cuerdas]
            [clojure.java.io :as io]
            [cheshire.core :as json]
            [clj-http.client :as http]
            [immutant.web :as web]
            [compojure.core :as c]
            [compojure.route :as r]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.util.response :refer [content-type response charset]])
  (:gen-class))

(def config
  (-> {:sounds-path "sounds"
       :callback-id "kaamelott"
       :port 8081
       :command "arthour"}
    (merge (select-keys env [:slack-validation-token :slack-oauth-token]))))

(def sounds (-> (io/resource "sounds.edn")
              slurp
              read-string))

(defn- search-sounds
  "Search sounds in DB (KISS - no diacritics)"
  [sounds query]
  (filter #(cuerdas/includes? (-> % :title cuerdas/lower)
             query)
    sounds))

(defn- sound-by-file
  "Find sound metadata via its filename"
  [filename]
  (->> sounds
    (filter #(= filename (:file %)))
    (first)))

(defn- sound->attn
  "Convert sound metadata into Slack attachment"
  [{:keys [title file]}]
  {:text title
   :color "#3AA3E3"
   :fallback "No buttons..."
   :attachment_type "default"
   :callback_id (:callback-id config)
   :actions [{:type "button"
              :name "send"
              :text "Envoyer"
              :value file}]})

(defn- send-sound [channel-id filename]
  (let [{:keys [slack-oauth-token sounds-path]} config
        file (io/resource (str sounds-path "/" filename))]
    (with-open [is (io/input-stream file)]
      (println (str sounds-path "/" filename) file is)
      (println channel-id slack-oauth-token)
      (http/post "https://slack.com/api/files.upload"
        {:multipart [{:name "file" :content is}
                     {:name "filename" :content filename}
                     {:name "channels" :content channel-id}
                     {:name "token" :content slack-oauth-token}]}))))

(defn- post-artour-check
  "Slack sends a POST validity request from time to time"
  [{:keys [params]}]
  (response (:title (rand-nth sounds))))

(defn- post-artour
  "Response to Slack command notification"
  [{:keys [params] :as req}]
  (let [{:keys [command text token channel_id]} params]
    (if true #_(= token (:slack-validation-token config))
      (let [results (search-sounds sounds text)]
        (-> (json/encode {:text "Les citations de Kaamelott dans ton Slack"
                          :attachments (map sound->attn results)})
          (response)
          (content-type "application/json")
          (charset "utf-8")))
      {:status 400 :body (:title (rand-nth sounds))})))

(defn- post-messages
  "Send sound file to channel when user made a choice"
  [{:keys [params] :as req}]
  (let [payload (:payload params)
        {:keys [actions callback_id channel] :as payload} (json/decode payload keyword)]
    (when (= (:callback-id config) callback_id)
      (let [{:keys [value]} (first actions)
            {:keys [character episode]} (sound-by-file value)]
        (future (send-sound (:id channel) value))
        (-> (response (json/encode {:text (format "*%s*\n_%s_" character episode)
                                    :replace_original true}))
          (content-type "application/json")
          (charset "utf-8"))))))

(c/defroutes app
  (c/POST "/check" [:as req] (post-artour-check req))
  (c/POST "/messages" [:as req] (post-messages req))
  (c/POST "/" [:as req] (post-artour req))
  (r/not-found (:title (rand-nth sounds))))

(defn -main [& _]
  (println (str "Kaamelott in - " (:title (rand-nth sounds))))
  (let [handler (wrap-defaults
                  app
                  (assoc-in api-defaults [:params :multipart] true))
        server (web/run handler :path "/arthour" :port (:port config))]
    (-> (Runtime/getRuntime)
      (.addShutdownHook (Thread. ^Runnable
      #(do
         (web/stop server)
         (println (str "Kaamelott out - "
                    (:title (rand-nth sounds))))))))))
