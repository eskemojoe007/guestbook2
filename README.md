# guestbook2

generated using Luminus version "3.39"

This was setup to go through the tutorials found in the pragmatic bookstore.
In that tutorial a basic template is setup, then is followed by the addition of
many of different tools.  Here I ran the correct template to make all of that
easy to add: `lein new luminus guestbook2 +h2 +cljs +reagent +re-frame +swagger`
Though `+cljs +reagent` is likely redundant.

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

## License

Copyright © 2019 FIXME
