package com.example.learnedreversegeocoding.cassandra

/**
 * Class: CassandraWriterActor
 * Actor for writing data to a Cassandra cluster
 *
 * Created by jmurdoch on 2014-06-16.
 */

import akka.actor.Actor
import com.example.learnedreversegeocoding._

private[learnedreversegeocoding] class CassandraWriterActor extends Actor with ConfigCassandraCluster {

  import akka.event.Logging
  import akka.pattern.pipe
  import context.dispatcher
  import com.datastax.driver.core.querybuilder.QueryBuilder
  import com.datastax.driver.core.{Session, PreparedStatement}
  import cassandra.resultset._
  import BrokeredActorMessages.{BROKERCassWriteData, BROKERCassDeleteAllData, CASSOk}
  import domain.ReverseGeocoding3Params

  implicit val system = context.system
  val log = Logging(system, getClass)
  var session: Session = null
  var preparedStatement: PreparedStatement = null

  // Session management for the actor
  override def preStart() = createSession()
  override def postStop() = closeSession()

  def receive: Receive = {
    // Write data rows
    case BROKERCassWriteData(dataList : List[ReverseGeocoding3Params]) =>
      ifAlive({ dataList.foreach(saveData) })

    // Delete all data rows
    case BROKERCassDeleteAllData() =>
      ifAlive({ deleteAll() pipeTo sender })
  }

  // Save a number of data items
  def saveData(rgp: ReverseGeocoding3Params): Unit = {
    val boundStatement = preparedStatement.bind(rgp.lat.asInstanceOf[java.lang.Double], rgp.lon.asInstanceOf[java.lang.Double], rgp.postal_code)
    log.info(preparedStatement.getQueryString)
    session.executeAsync(boundStatement)
  }

  // Delete all data rows
  def deleteAll() = {
    val query = QueryBuilder.truncate(Keyspaces.akkaCassandra, ColumnFamilies.tuples)
    log.info(query.toString)
    session.executeAsync(query) map( x => CASSOk() )
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
      preparedStatement = session.prepare(
        "INSERT INTO " concat ColumnFamilies.tuples concat "(" concat
          ColumnNames.lat concat ", " concat
          ColumnNames.lon concat ", " concat
          ColumnNames.postal_code concat ") VALUES (?, ?, ?);")      
    } catch {
      case e : Throwable => log.warning("Could not connect to Cassandra DB instance: " concat e.toString)
    }
  }

  def closeSession() = {
    if (session != null && !session.isClosed)
    {
      session.close()
    }
  }  
}
