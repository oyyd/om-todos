(ns om-todos.todo
  (:require [cljs.core.async :refer [put!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(defn change-finished [todo finished?]
  (om/transact! todo :finished (fn [_] finished?)))

; TODO: refactor
(defn change-editing [owner editing? text]
  (let [tmp-text (if editing? text "")]
    (om/set-state! owner {:editing? editing? :tmp-text tmp-text})))

(defn change-tmp-text [owner e]
  (om/set-state! owner :tmp-text (.. e -target -value)))

(defn todo [{:keys [todo-data handle show-todo]} owner]
  (reify
    om/IInitState
    (init-state [_]
      {:editing? false
       :tmp-text ""})
    om/IDidUpdate
    (did-update [_ prev-props prev-state]
      (let [editing? (om/get-state owner :editing?)]
        (when editing?
          (.focus (om/get-node owner "text-input")))))
    om/IRenderState
    (render-state [_ state]
      (let [editing? (:editing? state)
            change-finish-status (partial change-finished
              todo-data (not (:finished todo-data)))
            change-editing-status (partial change-editing
              owner true (:text todo-data))
            label-class-name (str
              (if (:finished todo-data) "completed" "")
              (if editing? "" " hidden"))]
        (dom/li #js {:className
          (str (if show-todo "" "hidden") (if editing? " editing" ""))}
          (dom/div #js {:className "view"}
            (dom/input
              #js {:className "toggle" :type "checkbox"
                   :checked (:finished todo-data)
                   :onClick change-finish-status}
              nil)
            (dom/label
              #js {:onDoubleClick change-editing-status
                   :className label-class-name}
              (:text todo-data))
            (dom/button
              ; TODO: do not directly use string literals
              #js {:className "destroy"
                   :onClick #(put! handle {:type "remove" :todo @todo-data})}
              nil))
          (dom/input
            #js {:className "edit"
                 :ref "text-input" :type "text" :value (:tmp-text state)
                 :onBlur (partial change-editing owner false "")
                 :onChange (partial change-tmp-text owner)
                 :onKeyUp #(when (= (.-keyCode %) 13)
                   (put! handle {:type "update" :todo @todo-data
                                 :next-text (:tmp-text state)})
                   (om/set-state! owner {:editing? false}))
                 :style #js {:display (if editing? "block" "none")}}
            nil))))))
