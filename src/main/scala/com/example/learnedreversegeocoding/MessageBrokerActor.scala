package com.example.learnedreversegeocoding

import akka.actor.{Actor, Props}

/**
 * class: ActorRequestBroker
 * Centralized brokering of actor requests
 *
 * Created by jmurdoch on 2014-06-16.
 */

object MessageBrokerActor {

  import akka.actor.ActorSystem
  import akka.util.Timeout
  import java.util.concurrent.TimeUnit

  var started = false

  // Create an ActorSystem to host the application
  implicit lazy val system = ActorSystem()
  implicit val timeout = Timeout(10, TimeUnit.SECONDS)

  def broker = if (started) startBroker else null

  lazy val startBroker = {
    started = true
    system.actorOf(Props(new MessageBrokerActor))
  }
}

private[learnedreversegeocoding] class MessageBrokerActor extends Actor {

  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.Future
  import scala.util.Success
  import akka.io.IO
  import akka.pattern._
  import akka.event.Logging
  import spray.can.Http
  import domain.{ReverseGeocoding4Params, ReverseGeocoding3Params, ReverseGeocoding2Params}
  import BrokeredActorMessages._
  import googleapi.GoogleApiClientActor
  import cassandra.{CassandraWriterActor, CassandraReaderActor}
  import restservice.ReverseGeocodingRESTServiceActor
  import wekalearning.{PostalCodeDomain, UpdatableLearnerActor}

  implicit lazy val system = MessageBrokerActor.system
  implicit val timeout = MessageBrokerActor.timeout

  val log = Logging(system, getClass)

  // Create and start our service actors
  val writeCassandraActor = system.actorOf(Props(new CassandraWriterActor()))
  val readCassandraActor = system.actorOf(Props(new CassandraReaderActor()))
  val googleApiClientActor = system.actorOf(Props(new GoogleApiClientActor()))
  val updatableLearnerActor = system.actorOf(Props(new UpdatableLearnerActor()))
  val restServiceActor = system.actorOf(Props[ReverseGeocodingRESTServiceActor])

  // Start a new HTTP server on port 8080 with our service actor as the handler
  IO(Http) ! Http.Bind(restServiceActor, interface = "localhost", port = 8080)

  def receive = {

    case RESTTrainRqst(rgp) =>
      log.info("REST Request: TRAIN ZIP for {}, {}", rgp.lat, rgp.lon)
      val rest_sender = sender()

      // 1. Query the cache to determine if we have already submitted this data point as Model training data
      // 2. If not, query Google for the zip code (and submit result as Model training data)
      // 3. Return result to the REST service for rendering
      val future = (queryCache(rgp) recoverWith { case _ => queryGoogle(rgp) })
        .asInstanceOf[Future[ReverseGeocoding3Params]]

      future.onComplete {
        case Success(rgp3: ReverseGeocoding3Params) => rest_sender ! BROKERRestTrainResp(rgp3)
        case _ => rest_sender ! BROKERRestTrainResp(ReverseGeocoding3Params(rgp, null))
      }

    case RESTPredictRqst(rgp) =>
      log.info("REST Request: PREDICT ZIP for {}, {}", rgp.lat, rgp.lon)
      val rest_sender = sender()

      // 1. Predict the zip code using the current Model
      // 2. Query the cache to determine if we have already submitted this data point as Model training data
      // 3. If not, query Google for the zip code (and submit result as Model training data)
      // 4. Return result to the REST service for rendering
      val futurePredicted = queryLearner(rgp).map( rgp3 => rgp3.postal_code )
      val futureReal = (queryCache(rgp) recoverWith { case _ => queryGoogle(rgp) })
        .asInstanceOf[Future[ReverseGeocoding3Params]]
        .map( rgp3 => rgp3.postal_code )

      val future = for {
        predictedRgp <- futurePredicted recover { case _ => null }
        realRgp <- futureReal recover { case _ => null }
      } yield ReverseGeocoding4Params(rgp.lat, rgp.lon, predictedRgp, realRgp)

      future.onComplete {
        case Success(rgp4: ReverseGeocoding4Params) => rest_sender ! BROKERRestPredictResp(rgp4)
        case _ => rest_sender ! new Exception("REST PREDICT request failed")
      }

    case RESTUnlearnRqst() =>
      // Reset the model.
      // This request does not modify the existing DB cache.
      log.info("REST Request: Re-init model")
      updatableLearnerActor ! BROKERLearnerClearModel()

    case RESTDeleteRequestCacheRqst() =>
      val rest_sender = sender()

      // Clear the request cache (Cassandra DB).
      // This request does not modify the existing model.
      log.info("REST Request: Delete training data")
      val future = writeCassandraActor ? BROKERCassDeleteAllData()
      future.onComplete {
        case Success(CASSOk()) => rest_sender ! BROKEROk()
        case _ =>
          rest_sender ! new Exception("Local cache could not be deleted")
          log.warning("Local cache could not be deleted")
      }

    case RESTRetrainRqst() =>
      val rest_sender = sender()

      // Update/train the current model with any data points stored in the local
      // DB cache which have not already been used to train the model.
      log.info("REST Request: Train the model using the local request cache")

      val cacheFuture = readCassandraActor ? BROKERCassReadData()
      cacheFuture.onComplete {
        case Success(CASSResults(rgpsList)) =>
          updatableLearnerActor ! BROKERLearnerTrain(rgpsList)
          rest_sender ! BROKEROk()

        case _ =>
          rest_sender ! new Exception("REST RETRAIN from cached requests failed")
          log.warning("REST RETRAIN from cached requests failed")
      }
  }

  // Query the cache for an exact match and associated postal code
  def queryCache(rgp: ReverseGeocoding2Params) : Future[ReverseGeocoding3Params] = {
    (readCassandraActor ? BROKERCassFindData(rgp)) map { case CASSResult(rgp3) => rgp3 }
  }

  // Query Google (only performed if the result is not cached)
  def queryGoogle(rgp: ReverseGeocoding2Params) : Future[ReverseGeocoding3Params] = {
    (googleApiClientActor ? BROKERGoogleQuery(rgp)) map {
      case GOOGLEResponse(rgp3) =>
        if (rgp3.postal_code != null && PostalCodeDomain.USA.contains(rgp3.postal_code)) {
          writeCassandraActor ! BROKERCassWriteData(List(rgp3))
          updatableLearnerActor ! BROKERLearnerTrain(List(rgp3))
        }
        else
        {
          rgp3.postal_code = null
        }
        rgp3
    }
  }

  // Query the learning model for a predicted postal code based on previous training
  def queryLearner(rgp: ReverseGeocoding2Params) : Future[ReverseGeocoding3Params] = {
    (updatableLearnerActor ? BROKERLearnerPredict(rgp)) map {
      case LEARNERPredictResult(rgp3) => rgp3
      case e => throw new Exception("Model returned unexpected result type! " concat e.toString)
    }
  }
}

// Case classes for all messages passed between Actors
private[learnedreversegeocoding] object BrokeredActorMessages {

  import domain._

  case class BROKEROk()

  // ASK messages sourced from the restServiceActor
  case class RESTTrainRqst(rgp: ReverseGeocoding2Params)
  case class RESTPredictRqst(rgp: ReverseGeocoding2Params)
  case class RESTUnlearnRqst()
  case class RESTRetrainRqst()
  case class RESTDeleteRequestCacheRqst()

  // REPLY messages sent to the restServiceActor
  case class BROKERRestTrainResp(rgp: ReverseGeocoding3Params)
  case class BROKERRestPredictResp(rgp: ReverseGeocoding4Params)

  // ASK messages sent to the googleApiClientActor
  case class BROKERGoogleQuery(rpg: ReverseGeocoding2Params)
  // REPLY messages sourced from the googleApiClientActor
  case class GOOGLEResponse(rpg: ReverseGeocoding3Params)

  // ASK messages sent to the svmActor
  case class BROKERLearnerPredict(rgp: ReverseGeocoding2Params)
  case class BROKERLearnerClearModel()
  case class BROKERLearnerTrain(data: List[ReverseGeocoding3Params])
  // REPLY messages sourced from the svmActor
  case class LEARNERPredictResult(rpg: ReverseGeocoding3Params)

  // ASK messages sent to the readCassandraActor
  case class BROKERCassReadData()
  case class BROKERCassFindData(rgp: ReverseGeocoding2Params)
  // REPLY messages sourced from the readCassandraActor
  case class CASSResult(result: ReverseGeocoding3Params)
  case class CASSResults(results: List[ReverseGeocoding3Params])
  case class CASSOk()

  // TELL messages sent to the writeCassandraActor
  case class BROKERCassWriteData(data: List[ReverseGeocoding3Params])
  case class BROKERCassDeleteAllData()
}