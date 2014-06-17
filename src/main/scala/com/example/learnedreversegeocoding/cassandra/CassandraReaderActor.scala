package com.example.learnedreversegeocoding.cassandra

/**
 * Class: CassandraReaderActor
 * Actor for reading data from a Cassandra cluster
 *
 * Created by jmurdoch on 2014-06-16.
 */

import akka.actor.Actor
import com.example.learnedreversegeocoding._

private[learnedreversegeocoding] class CassandraReaderActor extends Actor with ConfigCassandraCluster {
  import com.datastax.driver.core.querybuilder.QueryBuilder
  import BrokeredActorMessages.{CASSResult, CASSResults, BROKERCassFindData, BROKERCassReadData}

  import scala.collection.JavaConversions._
  import scala.util.{Failure, Success}
  import akka.event.Logging
  import akka.pattern.pipe
  import context.dispatcher
  import com.datastax.driver.core.{Session, Row}
  import cassandra.resultset._
  import domain.{ReverseGeocoding2Params, ReverseGeocoding3Params}

  implicit val system = context.system
  val log = Logging(system, getClass)
  var session: Session = null
  val maxResults = 2500

  // Session management for the actor
  override def preStart() = createSession()
  override def postStop() = closeSession()

  def receive: Receive = {
    // Retrieve all data
    case BROKERCassReadData()  =>
      ifAlive ({ selectAll() pipeTo sender })

    // Retrieve a single row
    case BROKERCassFindData(rgp: ReverseGeocoding2Params) =>
      ifAlive ({
        val broker_sender = sender()
        val future = findUnique(rgp)

        future.onComplete {
          case Success(rgp3) => broker_sender ! CASSResult(rgp3)
          case Failure(e) =>
            log.info(e.getMessage)
            broker_sender ! new Exception("Query failed")
        }
      })
  }

  // Query for ALL data rows
  def selectAll() = {
    val query = QueryBuilder.select().all().from(Keyspaces.akkaCassandra, ColumnFamilies.tuples).limit(maxResults)
    log.info(query.toString)
    session.executeAsync(query) map(_.all().map(buildRGP).toList) map(x => CASSResults(x))
  }

  // Query for a single data row
  def findUnique(rgp: ReverseGeocoding2Params) = {
    val query = QueryBuilder.select().all()
      .from(Keyspaces.akkaCassandra, ColumnFamilies.tuples)
      .where(QueryBuilder.eq(ColumnNames.lat, rgp.lat)).and(QueryBuilder.eq(ColumnNames.lon, rgp.lon))
    log.info(query.toString)
    session.executeAsync(query) map(_.all().map(buildRGP).toList) map( list =>
      if (list.size == 1) list(0) else throw new Exception("Zero or >1 query results found") )
  }

  // Row => Domain Object construction
  def buildRGP(r: Row): ReverseGeocoding3Params = {
    val lat = r.getDouble(ColumnNames.lat)
    val lon = r.getDouble(ColumnNames.lon)
    val postal_code = r.getString(ColumnNames.postal_code)
    ReverseGeocoding3Params(lat, lon, postal_code)
  }

  // Check that the current cluster and session are defined and alive
  // If not, reinitialize the session and/or cluster
  def ifAlive(func: => Unit) = {
    if (session == null || session.isClosed) createSession()
    if (session != null && !session.isClosed) func
    else sender ! new Exception("Could not connect to Cassandra DB instance")
  }

  def createSession() = {
    try {
      session = cluster.connect(Keyspaces.akkaCassandra)
    } catch {
      case e : Throwable => log.warning("Could not connect to Cassandra DB instance: " concat e.toString)
    }
  }

  def closeSession() = {
    if (session != null && !session.isClosed) {
      session.close()
    }
  }
}
