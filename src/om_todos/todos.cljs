(ns om-todos.todos
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [om-todos.utils :refer [create-todo-data type-filters]]
            [om-todos.todo :refer [todo]]
            [om.core :as om :include-macros true]
            [cljs.core.async :refer [put! chan <!]]
            [clojure.string :refer [lower-case]]))

(defn show-todo? [type-filter finished]
  (or
    (= type-filter (:all type-filters))
    (and (= type-filter (:active type-filters)) (not finished))
    (and (= type-filter (:completed type-filters)) finished)))

(defn append-new-todo [todos text]
  ; TODO: maybe a better way to get the id for new todo
  (let [new-todo (create-todo-data (inc (:id (last todos))) text false)]
    (om/transact! todos (fn [tds] (conj tds new-todo)))))

(defn remove-from-todos [todos todo]
  (om/transact! todos (fn [tds] (vec (remove #(= todo %) tds)))))

(defn update-from-todos [todos todo next-text]
  (if (= next-text "")
    (remove-from-todos todos todo)
    (om/transact! todos (fn [tds]
      (vec (map #(if (= (:id todo) (:id %))
        (assoc % :text next-text)
        %)
      tds))))))

(defn change-type-filter [type-filter to-type-filter]
  (om/transact! type-filter (fn [_] [to-type-filter])))

(defn remove-completed [todos]
  (om/transact! todos (fn [tds] (vec (remove #(:finished %) tds)))))

(defn mark-all-todos [todos]
  (let [mark-as-finished (not-every? #(:finished %) todos)]
    (om/transact! todos (fn [tds]
      (vec (map #(assoc % :finished mark-as-finished) todos))))))

(defn check-new-item [data e]
  (let [keyCode (.-keyCode e)]
    (when (= keyCode 13)
      (append-new-todo data (.. e -target -value))
      (set! (.. e -target -value) ""))))

(defn filter-item [type-filter type]
  (let [active? (= (first type-filter) type)
        change-type (partial change-type-filter type-filter type)]
    (dom/li #js {:onClick change-type}
      (dom/a #js {:className (if active? "selected" nil)} (lower-case type)))))

(defn todos [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:handle (chan)})
    om/IWillMount
    (will-mount [_]
      (let [handle (om/get-state owner :handle)
            todos (:todos data)]
        (go (loop []
          (let [{type :type :as msg} (<! handle)]
            (case type
              "remove" (remove-from-todos todos (:todo msg))
              "update" (update-from-todos todos (:todo msg) (:next-text msg))))
          (recur)))))
    om/IRender
    (render [_]
      (let [handle (om/get-state owner :handle)
            keyUpHandler (partial check-new-item (:todos data))
            mark-all (partial mark-all-todos (:todos data))
            show-clear-button? (some #(:finished %) (:todos data))]
        (dom/div #js {:className "todoapp"}
          (dom/h1 nil "todos")
          (dom/div nil
            (dom/input
              #js {:type "checkbox" :className "toggle-all" :onClick mark-all} nil)
            (dom/input
              #js {:type "text" :onKeyUp keyUpHandler :className "new-todo"
                   :placeholder "What needs to be done?"} nil))
          (apply dom/ul #js {:className "todo-list"}
            (om/build-all todo (map #(hash-map
              :todo-data % :handle handle
              :show-todo (show-todo? (first (:type-filter data)) (:finished %))
            ) (:todos data))))
          (dom/div #js {:className "footer"}
            (dom/span #js {:className "todo-count"}
              (str (count (:todos data)) " items left"))
            (apply dom/ul #js {:className "filters"}
              (map #(filter-item (:type-filter data) %) (vals type-filters)))
            (dom/button
              #js {:className "clear-completed"
                   :onClick (partial remove-completed (:todos data))
                   :style #js {:display
                     (if show-clear-button? "block" "none")}}
              "Clear completed")))))))
