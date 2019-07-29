# guestbook2

generated using Luminus version "3.39"

This was setup to go through the tutorials found in the pragmatic bookstore.
In that tutorial a basic template is setup, then is followed by the addition of
many of different tools.  Here I ran the correct template to make all of that
easy to add: `lein new luminus guestbook2 +h2 +cljs +reagent +re-frame +swagger +jetty`
Though `+cljs +reagent` is likely redundant.

Note: `+jetty` wasn't entered, but `jetty` replaced `immutant` as the default just before this project was start, before the documentation was introduced.  

## Prerequisites

You will need [Leiningen][1] 2.0 or above installed.

[1]: https://github.com/technomancy/leiningen

## Running

To start a web server for the application, run:

    lein run


Or you can run it using the various repl's that we want to use.  Within Atom, we
use proto-repl.  I always make a `scratch.clj` file in the root for my commands
to run and test the server.  In general to run from the Repl, you need to start the
server and figwheel.

- `(start)`
- `(start-fw)`
- `(cljs)` - Transitions the repl to the figwheel repl.  You may have to add
`:nrepl-middleware [cider.piggieback/wrap-cljs-repl]` to your `project.clj` in the
dev section.

### Usefule Links:
- https://github.com/eskemojoe007/guestbook - My first version
- https://github.com/metosin/ring-http-response - ring response
- http://dm3.github.io/clojure.java-time/java-time.html#var-local-date-time.3F - Clojure.java-time
- https://clojure.org/guides/destructuring - Destructuring
- https://github.com/Day8/re-frame/blob/master/docs/CodeWalkthrough.md - Re-frame tutorial
- https://github.com/Day8/re-frame/blob/master/docs/Interceptors.md - Reframe interceptors (like `rf/path`)
- Clojure spec - https://clojure.org/about/spec
- Luminus docs - http://www.luminusweb.net/docs
- https://www.braveclojure.com/do-things/ - Clojure brave basics
-  https://github.com/metosin/reitit/blob/master/doc/ring/swagger.md - swagger UI
- http://localhost:3000/ - My site

## License

Copyright © 2019 FIXME
