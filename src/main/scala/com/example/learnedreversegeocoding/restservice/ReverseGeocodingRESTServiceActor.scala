
package com.example.learnedreversegeocoding.restservice

/**
 * Class: ReverseGeocodingRESTServiceActor
 * Actor for handling routing of an HTTP REST Service
 *
 * Created by jmurdoch on 2014-06-16.
 */

import akka.actor.Actor

private[learnedreversegeocoding] class ReverseGeocodingRESTServiceActor extends Actor with ReverseGeocodingRESTService {

  // The HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  // Run the route
  def receive = runRoute(myRoute)
}

