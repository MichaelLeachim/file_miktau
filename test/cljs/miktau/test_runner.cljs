(ns miktau.test-runner
  (:require
   [doo.runner :refer-macros [doo-tests]]
   [miktau.cloud.events-test]
   [miktau.cloud.subs-test]

   [miktau.nodes.events-test]
   [miktau.nodes.subs-test]
   
   [miktau.edit-nodes.events-test]
   [miktau.edit-nodes.subs-test]
   [miktau.edit-nodes.query-building-test]
   
   [miktau.generic.query-building-test]
   
   [miktau.various-test]))

(enable-console-print!)

(doo-tests
 'miktau.cloud.events-test
 'miktau.cloud.subs-test

 'miktau.nodes.events-test
 'miktau.nodes.subs-test
 
 'miktau.edit-nodes.events-test
 'miktau.edit-nodes.subs-test
 'miktau.edit-nodes.query-building-test

 'miktau.generic.query-building-test
 'miktau.various-test)


