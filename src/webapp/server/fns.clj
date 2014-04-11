(ns webapp.server.fns
  [:require [clojure.string :as str]]
  [:use [korma.db]]
  [:use [webapp.framework.server.systemfns]]
  [:use [webapp.framework.server.email-service]]
  [:use [webapp.framework.server.encrypt]]
  [:use [korma.core]]
  [:use [clojure.repl]]
  [:use [webapp.framework.server.db-helper]]
  [:use [webapp.framework.server.neo4j-helper]]

  (:use [webapp-config.settings])
  (:use [overtone.at-at])
  (:import [java.util.UUID])
  (:import [java.util TimerTask Timer]))



(def my-pool (mk-pool))

(def a (atom 0))







(defn process-ask-for-endorsement [send-endorsement-neo4j-node]
  (if send-endorsement-neo4j-node
    (do
      (send-email
       :message      "hi zubair"
       :subject      "subject test"
       :from-email   (:from send-endorsement-neo4j-node)
       :from-name    "zubair"
       :to-email     (:to send-endorsement-neo4j-node)
       :to-name      "zubairq")
      (neo4j "match n where id(n)={id}
             remove n:AskForEndorsement
             set n:SendEndorsementConfirmFrom
             return n"
             {:id (:neo-id send-endorsement-neo4j-node)} "n")
      )

    ))





(neo4j "match (n:WebSession) return count(n)" {} "count(n)")



(process-ask-for-endorsement
 (first (neo4j "match (n:AskForEndorsement) return n" {} "n")))

(first (neo4j "match (n:WebSession) return n" {} "n"))




(defn check-messages []
  (let [messages-waiting (neo4j "match (n:AskForEndorsement) return n" {} "n")]
    (dorun (map process-ask-for-endorsement  messages-waiting))
    ))


(stop-and-reset-pool! my-pool)

(do
  (stop-and-reset-pool! my-pool)
  (every 5000
         #(do
            (swap! a inc)
            (check-messages)
            ;(println @a)
            )

         my-pool))





(defn create-session[{}]
  {:value (uuid-str)}
)



(defn add-history [{:keys [session-id  history-order  timestamp  history]}]
  (println (str "** " history-order ", "  history))
  (let [
        path        (:path       history)
        new-state   (:new-state  history)
        ]
  (println (str "** history-order "    history-order))
  (println (str "** path "             path))
  (println (str "** new-state "        new-state))
  (println (str "** timestamp "        timestamp))
  (println (str "** session "          session-id))

  (let [existing-session  (first (neo4j "match (n:WebSession)
                                 where n.session_id={si}
                                 return n " {:si session-id} "n"))
        session           (if existing-session
                            existing-session
                            (first (neo4j "create  (n:WebSession
                            {
                              session_id:           {session_id}
                                   }) return n"
                                   {:session_id session-id}
                                   "n")))

        web-record        (first (neo4j "create  (n:WebRecord
                            {
                              session_id:           {session_id},
                              seq_ord:              {seq_ord},
                              path:                 {path},
                              new_state:            {new_state},
                              timestamp:            {timestamp}
                            }) return n"
         {
          :session_id  session-id
          :seq_ord     (str history-order)
          :path        (str path)
          :new_state   (str new-state)
          :timestamp   timestamp
          }
         "n"))
        ]
    (do
      (println session)
      (println web-record)
      (neo4j "START n=node(*), m=node(*)
             where id(n)={ws} and id(m)={wr}
             create (n)-[:FRIENDSHIP {status:2}]->(m)
  " {:ws (:neo-id session) :wr (:neo-id web-record)})

      []
      )

    )
  ))




(comment neo4j "START n=node(*), m=node(*)
           where id(n)={ws} and id(m)={wr}
           create (n)-[:FRIENDSHIP {status:2}]->(m)
" {:ws 17197 :wr 17200})


(neo4j "match (n:WebSession) return n.session_id" {} "n.session_id")
