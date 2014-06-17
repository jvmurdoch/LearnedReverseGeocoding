/**
 * class: LearnedReverseGeocoding
 * Application entry point
 *
 * Created by jmurdoch on 2014-06-16.
 */

package com.example

import learnedreversegeocoding.MessageBrokerActor

object LearnedReverseGeocoding extends App {

  // Create and start the broker service actor
  val actorRequestBroker = MessageBrokerActor.startBroker
}

