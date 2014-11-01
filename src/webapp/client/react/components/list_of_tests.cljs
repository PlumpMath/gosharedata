(ns webapp.client.react.components.list-of-tests
  (:require
   [webapp.framework.client.coreclient   :as c :include-macros true]
   [cljs.core.async  :refer [put! chan <! pub timeout]]
   [om-sync.core     :as async]
   [clojure.data     :as data]
   [clojure.string   :as string]
   [ankha.core       :as ankha]
   )
  (:use-macros
   [webapp.framework.client.coreclient  :only [ns-coils sql log neo4j neo4j-1 sql-1 log
                                               watch-data
                                               -->ui
                                               <--data
                                               remote
                                               defn-ui-component
                                               container
                                               map-many
                                               inline
                                               text
                                               div
                                               ]])
  (:use
   [webapp.framework.client.system-globals  :only  [app-state
                                                    reset-app-state
                                                    ui-watchers
                                                    data-watchers
                                                    data-state
                                                    add-init-state-fn
                                                    update-data
                                                    touch
                                                    ]]
   )
  (:require-macros
    [cljs.core.async.macros :refer [go]]))

(c/ns-coils 'webapp.client.react.components.list-of-tests)








(def data-sources (atom  {}))
@data-sources








(defn add-data-source [data-source-name
                       {
                        fields               :fields
                        db-table             :db-table
                        where                :where
                        path                 :path
                        }
                       ]
  (if (not (get @data-sources data-source-name))
    (do
      (reset! data-sources
              (assoc @data-sources data-source-name {}))

      (go
       (update-data [:tables db-table]
                    (remote !make-sql
                            {
                             :fields        fields
                             :db-table      db-table
                             :where         where
                             }) ))

      (watch-data [:tables db-table]
                  (do
                    (-->ui (into [] (flatten (conj  [:ui] path [:values])))
                           (<--data [:tables db-table]))
                    )))))






(defn data [name-of-reader    {
                               path                 :path
                               ui-state             :ui-state
                               interval-in-millis   :interval-in-millis
                               db-table             :db-table
                               fields               :fields
                               where                :where
                               }]
  (add-data-source  name-of-reader
                    {
                       :fields        fields
                       :db-table      db-table
                       :where         where
                       :path          path
                     })
  (get (get-in ui-state path) :values)
  )







(defn-ui-component     component-list-of-tests   [ui]

  (container




   (map-many
    #(container
      (inline "100%" (c/text "- " (:question  %1  ) )))
    (data  "read all questions for list" {
                                          :path       [:questions]
                                          :db-table   "learno_questions"
                                          :fields     "question"
                                          :ui-state   ui}))

   (div {:style {:padding "20px"}})

   (map-many
    #(container
      (inline "250px" (text "*" (:name %1) ))
      (inline ""      (text (:questions_answered_count %1))))
    (data  "read all tests for list" {
                                      :path       [:tests]
                                      :db-table   "learno_tests"
                                      :fields     "name"
                                      :ui-state   ui}))






   ))
