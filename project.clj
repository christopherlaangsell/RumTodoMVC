(defproject rum-todo "0.1.0"
  :dependencies [
    [org.clojure/clojure "1.7.0"]
    [org.clojure/clojurescript "1.7.122"]
    [rum "0.4.0"]
  ]

  :plugins [
    [lein-cljsbuild "1.1.0"]
  ]

  :cljsbuild { 
    :builds [
      { :id "advanced"
        :source-paths  ["src"]
        :compiler {
          :main          rum-todo.core
          :output-to     "target/todo.js"
          :optimizations :advanced
          :pretty-print  false
        }}
  ]}
  
  :profiles {
    :dev {
      :cljsbuild {
        :builds [
          { :id "none"
            :source-paths  ["src"]
            :compiler {
              :main          rum-todo.core
              :output-to     "target/todo.js"
              :output-dir    "target/none"
              :optimizations :none
              :source-map    true
            }}
      ]}
    }
  }
)
