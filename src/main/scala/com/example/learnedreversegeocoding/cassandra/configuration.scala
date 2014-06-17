package com.example.learnedreversegeocoding.cassandra

/**
 * Trait: ConfigCassandraCluster
 * Access to Cassandra config and cluster initialization
 *
 * Created by jmurdoch on 2014-06-16.
 */

import scala.collection.JavaConversions._
import com.datastax.driver.core.{ProtocolOptions, Cluster}
import com.example.learnedreversegeocoding.MessageBrokerActor

private[learnedreversegeocoding] trait ConfigCassandraCluster {
  def cluster = CassandraCluster.cluster
}

// Shared cluster object that may reinitialize
// itself if the Cassandra database goes up/down
private object CassandraCluster {
  private def config = MessageBrokerActor.system.settings.config

  private val cassandraConfig = config.getConfig("akka-cassandra.main.db.cassandra")
  private val port = cassandraConfig.getInt("port")
  private val hosts = cassandraConfig.getStringList("hosts").toList

  private var priCluster: Cluster = null

  def cluster = { this.synchronized {
    if (priCluster == null || priCluster.isClosed) {
      priCluster = Cluster.builder().
        addContactPoints(hosts: _*).
        withCompression(ProtocolOptions.Compression.SNAPPY).
        withPort(port).
        build()
    } }
    priCluster
  }
}
