(ns clojure-irc-client.core
  (:gen-class)
  (:require [clojure.string :as string]
            [clojure.tools.cli :as cli]
            [clojure.core.async :as async]
            [com.gearswithingears.async-sockets :refer :all]))

(defn write
  ([socket message]
   (write socket message false))
  ([socket message print?]
   (when print?
     (println message))
   (async/>!! (:out socket) (str message "\r"))))

(defn login-as-guest [socket nick]
  (println (str "Logging in as guest " nick))
  (write socket (str "NICK " nick))
  ;(Thread/sleep 5000)
  (write socket (str "USER " nick " 0 * :" nick))
  (println "done logging in")
  )

(defn input-listener [socket]
  (loop []
    (let [input (read-line)]
      (println input)
      
      (recur))))

(defn handle-line [socket line]
  (println line)
  (cond
    (re-find #"^ERROR :Closing Link:" line)
    (close-socket-client socket)
    (re-find #"^PING" line)
    (write socket (str "PONG " (re-find #":.*" line)) :print)))

(defn message-listener [socket]
  (async/go-loop []
    (when-let [line (async/<! (:in socket))]
      (handle-line socket line)
      (recur))))

(defn connect [nick host port]
  (println "Connecting...")
  (try
    (let [socket (socket-client port host)]
      (println (str "Connected to " host ":" port))
      (login-as-guest socket nick)
      
       (message-listener socket)
      
       (Thread/sleep 7000)
       (write socket "JOIN #whateverhey" true)
       (Thread/sleep 1000)
       (write socket "PRIVMSG #whateverhey :Hello!" true)
       
       (Thread/sleep 10000)
       (write socket "QUIT")
       (Thread/sleep 1000)
       
       
       
       ; it's a loop, so put it last.
       ;(input-listener socket)
       
       
       
       
       )
    (catch Exception e
      (println (str "Failed to connect to " host ":" port)))))

(def cli-usage
  [["-n" "--nick NICK" "Nickname" :default "examplenickdonotuse"]
   ["-h" "--host HOST" "Hostname" :default "127.0.0.1"]
   ["-p" "--port PORT" "Port number" :parse-fn #(Integer/parseInt %) :default 6667]])

(defn -main [& args]
  (let [{:keys [options summary]} (cli/parse-opts args cli-usage)
         socket (socket-client (:port options) (:host options))
           ]
    (if (or (not (:nick options)) (not (:port options)) (not (:host options)))
      (println summary)
      (do 
        (connect
          (-> options :nick string/trim)
          (-> options :host string/trim)
          (:port options))
        ;(write socket "JOIN #whateverhey" true)
        ;(write socket "PRIVMSG #whateverhey :Hello!")
        ;(Thread/sleep 10000)
        ;(write socket "QUIT")
      )
)))
