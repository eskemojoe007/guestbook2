(ns guestbook2.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [guestbook2.core-test]))

(doo-tests 'guestbook2.core-test)
