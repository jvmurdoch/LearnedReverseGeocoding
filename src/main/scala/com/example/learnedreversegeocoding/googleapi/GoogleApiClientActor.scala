package com.example.learnedreversegeocoding.googleapi

import akka.actor.Actor
import com.example.learnedreversegeocoding._

/**
 * Class: GoogleApiClientActor
 * An actor for managing HTTP requests and response processing
 * for Google API reverse geocoding queries.
 *
 * Created by jmurdoch on 2014-06-16.
 */

private[learnedreversegeocoding] class GoogleApiClientActor extends Actor {

  import scala.util.{Failure, Success}
  import akka.event.Logging
  import spray.client.pipelining._
  import spray.httpx.SprayJsonSupport._
  import spray.json._
  import system.dispatcher
  import domain.{ReverseGeocoding2Params,ReverseGeocoding3Params}
  import BrokeredActorMessages.{GOOGLEResponse, BROKERGoogleQuery}
  import GoogleApiJsonResultProtocol._

  implicit val system = context.system
  val log = Logging(system, getClass)

  val GoogleGeocodingAPIKey = "AIzaSyBc1m3W2fh9FL2Ymm5FBxrhIkqtP-2mdOk"
  def googleGeocodingAPIProxy(lat: Double, lon: Double) =
    s"https://maps.googleapis.com/maps/api/geocode/json?latlng=$lat,$lon&result_type=postal_code&key=$GoogleGeocodingAPIKey"

  def receive = {
    case BROKERGoogleQuery(rgp: ReverseGeocoding2Params) =>

      log.info("Requesting Postal Code for: {}, {}", rgp.lat, rgp.lon)

      val pipeline = sendReceive ~> unmarshal[GoogleApiResults]
      val broker_sender = sender()

      val responseFuture = pipeline {
        val apiProxy = googleGeocodingAPIProxy(rgp.lat, rgp.lon)
        log.info("GET Request URL: {}", apiProxy)
        Get(apiProxy)
      }

      responseFuture onComplete {
        case Success(g) =>
          broker_sender ! GOOGLEResponse(ReverseGeocoding3Params(rgp, filterResultsForPostalCode(g)))
          log.info("Request Response:\n{}", g.toJson.prettyPrint)

        case Failure(error) =>
          broker_sender ! new Exception("Google query failed")
          log.warning(error.getMessage)
      }
  }

  // Extract a single Postal Code result from the unmarshalled GoogleApiResults
  def filterResultsForPostalCode(results: GoogleApiResults) : String = {

    val s = {
      for (i <- results.results;
           j <- i.address_components if j.types.size == 1 && j.types.contains("postal_code"))
      yield j.long_name
    }

    // If there are multiple postal code values represented in the results, use the first
    if (s.size >= 1) s(0)
    else null
  }
}
