(ns reagent-phonecat.core)

(defmacro <? "Version of <! which throws Error that come out of the channel."
  [c]
  `(reagent-phonecat.core/throw-err (cljs.core.async/<! ~c)))

(defmacro err-or "If body throws an exception, catch it and return it" 
  [& body]
  `(try 
     ~@body
     (catch js/Error e# e#)))

(defmacro go-safe "'Safe' version of cljs.core.async/go which catches and returns exception which are thrown in its body"
  [& body]
  `(cljs.core.async.macros/go (reagent-phonecat.core/err-or ~@body)))

(defmacro spy [form]
  (let [text (str form)]
    `(let [r# ~form]
       (prn ~text r#)
       r#)))