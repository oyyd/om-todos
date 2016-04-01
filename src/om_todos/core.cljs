(ns ^:figwheel-always om-todos.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [om-todos.todos :refer [todos]]
            [om-todos.utils :refer [create-todo-data type-filters]]))

(enable-console-print!)

(defonce app-state
  (atom
    {:todos
      [(create-todo-data 0 "first" true)
       (create-todo-data 1 "second" false)]
     :type-filter [(:all type-filters)]}))

(om/root todos app-state
  {:target (. js/document (getElementById "main"))})

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
