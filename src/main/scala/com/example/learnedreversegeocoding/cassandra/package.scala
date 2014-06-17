package com.example.learnedreversegeocoding

/**
 * Configuration of the Cassandra DB "schema"
 *
 * Created by jmurdoch on 2014-06-16.
 */

package object cassandra {
  private[cassandra] object Keyspaces {
    val akkaCassandra = "reverseGeocodingKS"
  }

  private[cassandra] object ColumnFamilies {
    val tuples = "tuples"
  }

  private[cassandra] object ColumnNames {
    val lat = "lat"
    val lon = "lon"
    val postal_code = "postal_code"
  }
}


