(ns rum-todo.core
  (:require
    [clojure.set :as set]
    [clojure.string :as str]
    [rum.core :as rum]))


(defonce todos (atom (sorted-map)))
(defonce counter (atom 0))


(def stopwatch (atom {:start nil}))






(defn toggle [id] (swap! todos update-in [id :done] not))
(defn save [id title] (swap! todos assoc-in [id :title] title))
(defn delete [id] (swap! todos dissoc id))

(defn mmap [m f a] (->> m (f a) (into (empty m))))
(defn complete-all [v] (swap! todos mmap map #(assoc-in % [1 :done] v)))
(defn clear-done [] (swap! todos mmap remove #(get-in % [1 :done])))

(defn add-todo [text]
  (let [id (swap! counter inc)]
    (swap! todos assoc id {:id id :title text :done false})))

(def filt (atom :all))

(defn benchmark1 []
  (swap! stopwatch assoc :start (new js/Date))
  (dotimes [_ 200]
    (add-todo "foo")))

(defn benchmark2 [active]
  (swap! stopwatch assoc :start (new js/Date))
  (complete-all (pos? active))
  (clear-done))


(defn benchmark3 [active]
  (swap! stopwatch assoc :start (new js/Date))
  (dotimes [_ 200]
    (add-todo "foo"))
  (dotimes [_ 5]
    (complete-all (pos? active)))
  (clear-done)
  (dotimes [_ 200]
    (add-todo "foo")))


(defn benchmark4 [active]
  (swap! stopwatch assoc :start (new js/Date))
  (complete-all (pos? active)))


(defn benchmark5 [active]
  (swap! stopwatch assoc :start (new js/Date))
  (dotimes [_ 400]
    (add-todo "bar")))

(defn benchmark6 [active]
  (swap! stopwatch assoc :start (new js/Date))
  (complete-all (pos? active))
  (clear-done))


(defn benchmark7 [active]
  (swap! stopwatch assoc :start (new js/Date))
  (dotimes [_ 800]
    (add-todo "bar")))

(defn benchmark8 [active]
  (swap! stopwatch assoc :start (new js/Date))
  (doall (map toggle (take 400 (keys @todos)))))

(defn benchmark9 [active]
  (swap! stopwatch assoc :start (new js/Date))
  (reset! filt :active)
  (set! (.. js/document (getElementById "active") -checked) true))

(defn benchmark10 [active]
  (swap! stopwatch assoc :start (new js/Date))
  (reset! filt :all)
  (complete-all (pos? active))
  (clear-done)
  (set! (.. js/document (getElementById "all") -checked) true))


(rum/defcs todo-input < (rum/local "" ::val)
  [state {:keys [title on-save on-stop id class placeholder]}]
  (let [val (::val state)
        stop #(do (reset! val "")
                   (if on-stop (on-stop)))
        save #(let [v (-> @val str str/trim)]
                (if-not (empty? v) (on-save v))
                (stop))
        ]
    [:input {:type "text" :value @val
             :id id  :class class
             :placeholder placeholder
             :on-blur save
             :on-change #(reset! val (-> % .-target .-value))
             :on-key-down #(case (.-which %)
                             13 (save)
                             27 (stop)
                             nil)}]))

(rum/defcs todo-edit
  < (rum/local "" ::val)
  {:did-mount (fn [state]
                (.focus (.getDOMNode (:rum/react-component state)))
                state)}
  [state {:keys [title on-save on-stop id class placeholder]}]
  (let [val (::val state)
        stop #(do (reset! val "")
                  (if on-stop (on-stop)))
        save #(let [v (-> @val str str/trim)]
                (if-not (empty? v) (on-save v))
                (stop))
        ]
    [:input {:type "text" :value @val
             :id id  :class class
             :placeholder placeholder
             :on-blur save
             :on-change #(reset! val (-> % .-target .-value))
             :on-key-down #(case (.-which %)
                             13 (save)
                             27 (stop)
                             nil)}]))


(rum/defc todo-stats < rum/reactive
  [{:keys [filt active done]}]
  (let [props-for (fn [name]
                    {:class (if (= name (rum/react filt) "selected")
                              :on-click #(reset! filt name))})
        radio-buttons (fn [[id txt]]
                        [:div {:key id}
                         [:input {:name "filters" :type "radio"
                                  :id id :value txt}]
                         [:label {:for id} txt]])]
    [:div
     [:span#todo-count
      [:strong active] " " (case active 1 "item" "items") " left"]
     [:ul#filters
      [:div {:on-change (fn [e]
                          (reset! filt (keyword (-> e .-target .-id))))}
       (map radio-buttons [[:all "All"]
                           [:active "Active"]
                           [:done "Completed"]])]
      (when (pos? done)
        [:button#clear-completed {:on-click clear-done}
         "Clear completed " done])]]))


(rum/defcs todo-item < (rum/local false ::editing)
  [state {:keys [id done title]}]
  (let [editing (::editing state)]
    [:li {:class (str (if done "completed ")
                      (if @editing "editing"))}
     [:div.view
      [:input.toggle {:type "checkbox" :checked done
                      :on-change #(toggle id)}]
      [:label {:on-double-click #(reset! editing true)} title]
      [:button.destroy {:on-click #(delete id)} "X"]]
     (when @editing
       (todo-edit {:class "edit" :title title
                   :on-save #(save id %)
                   :on-stop #(reset! editing false)}))]))



(def did-update-mixin
  {:did-update
   (fn [state]
     (let [ms (- (.valueOf (new js/Date)) (:start @stopwatch))]
       (set! (.-innerHTML (js/document.getElementById "message")) (str ms "ms")))
     state)})

(rum/defc todo-app <
  rum/reactive
  did-update-mixin
  []
  (let [items (vals (rum/react todos))
        done (->> items (filter :done) count)
        active (- (count items) done)]
    [:div
     [:input {:type "button" :value "Benchmark 1" :on-click #(benchmark1)}]
     [:input {:type "button" :value "Benchmark 2" :on-click #(benchmark2 active)}]
     [:input {:type "button" :value "Benchmark 3" :on-click #(benchmark3 active)}]
     [:input {:type "button" :value "Benchmark 4" :on-click #(benchmark4 active)}]
     [:input {:type "button" :value "Benchmark 5" :on-click #(benchmark5 active)}]
     [:input {:type "button" :value "Benchmark 6" :on-click #(benchmark6 active)}]
     ;; [:input {:type "button" :value "Benchmark 7" :on-click #(benchmark7 active)}]
     ;; [:input {:type "button" :value "Benchmark 8" :on-click #(benchmark8 active)}]
     ;; [:input {:type "button" :value "Benchmark 9" :on-click #(benchmark9 active)}]
     ;; [:input {:type "button" :value "Benchmark 10" :on-click #(benchmark10 active)}]

     [:div#message]
     [:section#todoapp
      [:header#header
       [:h1 "todos"]
       (todo-input {:id "new-todo"
                    :placeholder "What needs to be done?"
                    :on-save add-todo
                    })]
      (when (-> items count pos?)
        [:div
         [:section#main
          [:input#toggle-all {:type "checkbox" :checked (zero? active)
                              :on-change #(complete-all (pos? active))}]
          [:label {:for "toggle-all"} "Mark all as complete"]
          [:ul#todo-list
           (for [todo (filter (case (rum/react filt)
                                :active (complement :done)
                                :done :done
                                :all identity) items)]
             (todo-item todo))]]
         [:footer#footer
          (todo-stats {:active active :done done :filt filt})]])]]))


(rum/mount (todo-app) js/document.body)
