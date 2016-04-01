(ns om-todos.utils)

(def type-filters {:all "ALL" :active "ACTIVE" :completed "COMPLETE"})

(defn create-todo-data
  [id text finished] {:id id :text text :finished finished})
