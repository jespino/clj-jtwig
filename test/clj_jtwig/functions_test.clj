(ns clj-jtwig.functions-test
  (:require [clojure.test :refer :all]
            [clj-jtwig.core :refer :all]
            [clj-jtwig.functions :refer :all]))

; TODO: is there a better way to test that something is an instance of some object generated by reify?
(defn valid-function-handler? [x]
  (and (not (nil? x))
       (-> x
           (class)
           (.getName)
           (.startsWith "clj_jtwig.functions$make_function_handler"))))

(deftest template-functions
  (testing "Adding custom template functions"
    (do
      (reset-functions!)

      (is (valid-function-handler?
            (deftwigfn "add" [a b]
                       (+ a b))))

      (is (true? (function-exists? "add")))
      (is (false? (function-exists? "foobar")))

      (is (valid-function-handler?
            (deftwigfn "add" [a b]
                       (+ a b))))

      (is (= (render "{{add(1, 2)}}" nil)
             "3")
          "calling a custom function")
      (is (= (render "{{add(a, b)}}" {:a 1 :b 2})
             "3")
          "calling a custom function, passing in variables from the model-map as arguments")
      (is (= (render "{{x|add(1)}}" {:x 1})
             "2")
          "calling a custom function using the 'filter' syntax")

      (reset-functions!)))

  (testing "Fixed and variable number of template function arguments"
    (do
      (reset-functions!)

      (is (valid-function-handler?
            (deftwigfn "add2" [a b]
                       (+ a b))))
      (is (true? (function-exists? "add2")))
      (is (valid-function-handler?
            (deftwigfn "addAll" [& numbers]
                       (apply + numbers))))
      (is (true? (function-exists? "addAll")))

      (is (= (render "{{add2(1, 2)}}" nil)
             "3")
          "fixed number of arguments (correct amount)")
      (is (thrown-with-msg?
            Exception
            #"clojure\.lang\.ArityException: Wrong number of args"
            (render "{{add2(1)}}" nil)))
      (is (= (render "{{addAll(1, 2, 3, 4, 5)}}" nil)
             "15")
          "variable number of arguments (non-zero)")
      (is (= (render "{{addAll}}" nil)
             "null")
          "variable number of arguments (zero)")

      (reset-functions!)))

  (testing "Passing different data structures to template functions"
    (do
      (reset-functions!)

      (is (valid-function-handler?
            (deftwigfn "identity" [x]
                       x)))
      (is (true? (function-exists? "identity")))
      (is (valid-function-handler?
            (deftwigfn "typename" [x]
                       (.getName (type x)))))
      (is (true? (function-exists? "typename")))

      ; verify that the clojure function recognizes the correct types when the variable is passed via the model-map
      (is (= (render "{{typename(x)}}" {:x 42})
             "java.lang.Long")
          "integer typename via model-map")
      (is (= (render "{{typename(x)}}" {:x 3.14})
             "java.lang.Double")
          "float typename via model-map")
      (is (= (render "{{typename(x)}}" {:x "foobar"})
             "java.lang.String")
          "string typename via model-map")
      (is (= (render "{{typename(x)}}" {:x \a})
             "java.lang.Character")
          "char typename via model-map")
      (is (= (render "{{typename(x)}}" {:x true})
             "java.lang.Boolean")
          "boolean typename via model-map")
      (is (= (render "{{typename(x)}}" {:x '(1 2 3 4 5)})
             "clojure.lang.LazySeq")
          "list typename via model-map")
      (is (= (render "{{typename(x)}}" {:x [1 2 3 4 5]})
             "clojure.lang.LazySeq")
          "vector typename via model-map")
      (is (= (render "{{typename(x)}}" {:x {:a 1 :b "foo" :c nil}})
             "clojure.lang.PersistentArrayMap")
          "map typename via model-map")
      (is (= (render "{{typename(x)}}" {:x #{1 2 3 4 5}})
             "clojure.lang.LazySeq")
          "set typename via model-map")

      ; verify that the clojure function recognizes the correct types when the variable is passed via a constant
      ; value embedded in the template
      (is (= (render "{{typename(42)}}" nil)
             "java.lang.Integer")
          "integer typename via constant value embedded in the template")
      (is (= (render "{{typename(3.14)}}" nil)
             "java.lang.Double")
          "float typename via constant value embedded in the template")
      (is (= (render "{{typename('foobar')}}" nil)
             "java.lang.String")
          "string typename via constant value embedded in the template")
      (is (= (render "{{typename('a')}}" nil)
             "java.lang.Character")
          "char typename via constant value embedded in the template")
      (is (= (render "{{typename(true)}}" nil)
             "java.lang.Boolean")
          "boolean typename via constant value embedded in the template")
      (is (= (render "{{typename([1, 2, 3, 4, 5])}}" nil)
             "clojure.lang.LazySeq")
          "list typename via constant value embedded in the template")
      (is (= (render "{{typename(1..5)}}" nil)
             "clojure.lang.LazySeq")
          "vector typename via constant value embedded in the template")
      (is (= (render "{{typename({a: 1, b: 'foo', c: null})}}" nil)
             "clojure.lang.PersistentArrayMap")
          "map typename via constant value embedded in the template")

      ; simple passing / returning... not doing anything exciting with the arguments
      ; using a constant value embedded inside the template
      (is (= (render "{{identity(x)}}" {:x 42})
             "42")
          "integer via model-map")
      (is (= (render "{{identity(x)}}" {:x 3.14})
             "3.14")
          "float via model-map")
      (is (= (render "{{identity(x)}}" {:x "foobar"})
             "foobar")
          "string via model-map")
      (is (= (render "{{identity(x)}}" {:x \a})
             "a")
          "char via model-map")
      (is (= (render "{{identity(x)}}" {:x true})
             "true")
          "boolean via model-map")
      (is (= (render "{{identity(x)}}" {:x '(1 2 3 4 5)})
             "[1, 2, 3, 4, 5]")
          "list via model-map")
      (is (= (render "{{identity(x)}}" {:x [1 2 3 4 5]})
             "[1, 2, 3, 4, 5]")
          "vector via model-map")
      ; TODO: order of iteration through a map is undefined, the string being tested may not always be the same (wrt. order)
      (is (= (render "{{identity(x)}}" {:x {:a 1 :b "foo" :c nil}})
             "{b=foo, c=null, a=1}")
          "map via model-map")
      (is (= (render "{{identity(x)}}" {:x #{1 2 3 4 5}})
             "[1, 2, 3, 4, 5]")
          "set via model-map")

      ; simple passing / returning... not doing anything exciting with the arguments
      ; using a constant value embedded inside the template
      (is (= (render "{{identity(42)}}" nil)
             "42")
          "integer via constant value embedded in the template")
      (is (= (render "{{identity(3.14)}}" nil)
             "3.14")
          "float via constant value embedded in the template")
      (is (= (render "{{identity('foobar')}}" nil)
             "foobar")
          "string via constant value embedded in the template")
      (is (= (render "{{identity('a')}}" nil)
             "a")
          "char via constant value embedded in the template")
      (is (= (render "{{identity(true)}}" nil)
             "true")
          "boolean via constant value embedded in the template")
      (is (= (render "{{identity([1, 2, 3, 4, 5])}}" nil)
             "[1, 2, 3, 4, 5]")
          "enumerated list via constant value embedded in the template")
      (is (= (render "{{identity(1..5)}}" nil)
             "[1, 2, 3, 4, 5]")
          "list by comprehension via constant value embedded in the template")
      ; TODO: order of iteration through a map is undefined, the string being tested may not always be the same (wrt. order)
      (is (= (render "{{identity({a: 1, b: 'foo', c: null})}}" nil)
             "{b=foo, c=null, a=1}")
          "map via constant value embedded in the template")

      ; iterating over passed sequence/collection type arguments passed to a custom function from a variable
      ; inside the model-map and being returned
      (is (= (render "{% for i in identity(x) %}{{i}} {% endfor %}" {:x '(1 2 3 4 5)})
             "1 2 3 4 5 ")
          "list (iterating over a model-map var passed to a function and returned from it)")
      (is (= (render "{% for i in identity(x) %}{{i}} {% endfor %}" {:x [1 2 3 4 5]})
             "1 2 3 4 5 ")
          "vector (iterating over a model-map var passed to a function and returned from it)")
      ; TODO: order of iteration through a map is undefined, the string being tested may not always be the same (wrt. order)
      (is (= (render "{% for k, v in identity(x) %}{{k}}: {{v}} {% endfor %}" {:x {:a 1 :b "foo" :c nil}})
             "b: foo c: null a: 1 ")
          "map (iterating over a model-map var passed to a function and returned from it)")
      (is (= (render "{% for i in identity(x) %}{{i}} {% endfor %}" {:x #{1 2 3 4 5}})
             "1 2 3 4 5 ")
          "set (iterating over a model-map var passed to a function and returned from it)")

      ; iterating over passed sequence/collection type arguments passed to a custom function from a constant
      ; value embedded in the template and being returned
      (is (= (render "{% for i in identity([1, 2, 3, 4, 5]) %}{{i}} {% endfor %}" nil)
             "1 2 3 4 5 ")
          "enumerated list (iterating over a model-map var passed to a function and returned from it)")
      (is (= (render "{% for i in identity(1..5) %}{{i}} {% endfor %}" nil)
             "1 2 3 4 5 ")
          "list by comprehension (iterating over a model-map var passed to a function and returned from it)")
      ; TODO: order of iteration through a map is undefined, the string being tested may not always be the same (wrt. order)
      (is (= (render "{% for k, v in identity({a: 1, b: 'foo', c: null}) %}{{k}}: {{v}} {% endfor %}" nil)
             "b: foo c: null a: 1 ")
          "map (iterating over a model-map var passed to a function and returned from it)")

      (reset-functions!))))

(deftest standard-functions
  (testing "Standard functions were added properly"
    (is (true? (function-exists? "blankIfNull")))
    (is (true? (function-exists? "butlast")))
    (is (true? (function-exists? "dump")))
    (is (true? (function-exists? "nth")))
    (is (true? (function-exists? "max")))
    (is (true? (function-exists? "min")))
    (is (true? (function-exists? "random")))
    (is (true? (function-exists? "range")))
    (is (true? (function-exists? "rest")))
    (is (true? (function-exists? "second")))
    (is (true? (function-exists? "sort")))
    (is (true? (function-exists? "sortDescending")))
    (is (true? (function-exists? "sortBy")))
    (is (true? (function-exists? "sortDescendingBy")))))