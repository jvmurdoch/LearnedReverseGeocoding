/**
 * Trait: ReverseGeocodingRESTService
 * HTTP REST Service routing and request handling
 *
 * Created by jmurdoch on 2014-06-16.
 */

package com.example.learnedreversegeocoding.restservice

import spray.routing.HttpService
import com.example.learnedreversegeocoding._

private[learnedreversegeocoding] trait ReverseGeocodingRESTService extends HttpService {

  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.util.Success
  import akka.pattern._
  import spray.http.MediaTypes._
  import domain._
  import BrokeredActorMessages._

  implicit val timeout = MessageBrokerActor.timeout

  val myRoute =
    pathPrefix("reverse_geocode") {
      pathEnd {
        post {
          parameters('lat.as[Double], 'lon.as[Double]).as(ReverseGeocoding2Params) { rgp: ReverseGeocoding2Params =>
            respondWithMediaType(`application/json`) {
              requestContext => {
                val future = MessageBrokerActor.broker ? RESTTrainRqst(rgp)
                future.onComplete {
                  case Success(BROKERRestTrainResp(rgp3)) => requestContext.complete(rgp3.prettyPrint())
                  case error => requestContext.complete(error.toString)
                }
              }
            }
          }
        } ~
          get {
            parameters('lat.as[Double], 'lon.as[Double]).as(ReverseGeocoding2Params) { rgp: ReverseGeocoding2Params =>
              respondWithMediaType(`application/json`) {
                requestContext => {
                  val future = MessageBrokerActor.broker ? RESTPredictRqst(rgp)
                  future.onComplete {
                    case Success(BROKERRestPredictResp(rgp4)) => requestContext.complete(rgp4.prettyPrint())
                    case error => requestContext.complete(error.toString)
                  }
                }
              }
            }
          }
      } ~
        path("clear_model") {
          post {
            respondWithMediaType(`application/json`) {
              complete {
                MessageBrokerActor.broker ! RESTUnlearnRqst()
                """{ "response" : "Model has been reset" }"""
              }
            }
          }
        } ~
        path("retrain_model") {
          post {
            respondWithMediaType(`application/json`) {
              requestContext => {
                val future = MessageBrokerActor.broker ? RESTRetrainRqst()
                future.onComplete {
                  case Success(BROKEROk()) => requestContext.complete("""{ "response" : "Model has been trained with the local request cache" }""")
                  case _ => requestContext.complete("""{ "response" : "Training data cache is currently unavailable" }""")
                }
              }
            }
          }
        } ~
        path("clear_cache") {
          post {
            respondWithMediaType(`application/json`) {
              requestContext => {
                val future = MessageBrokerActor.broker ? RESTDeleteRequestCacheRqst()
                future.onComplete {
                  case Success(BROKEROk()) => requestContext.complete("""{ "response" : "Local request cache has been deleted" }""")
                  case _ => requestContext.complete("""{ "response" : "Local request cache is currently unavailable" }""")
                }
              }
            }
          }
        }
    }
}