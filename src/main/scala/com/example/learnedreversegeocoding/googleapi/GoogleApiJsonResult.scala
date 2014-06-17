package com.example.learnedreversegeocoding.googleapi

/**
 * Domain objects and JSON formats for marshalling/unmarshalling
 * results from Google's Geocoding API
 *
 * Created by jmurdoch on 2014-06-16.
 */

import spray.json.{JsonFormat, DefaultJsonProtocol}

private[googleapi] case class GoogleApiAddressComponent(long_name: String, short_name: String, types: List[String])
private[googleapi] case class GoogleApiResult(address_components: List[GoogleApiAddressComponent])
private[googleapi] case class GoogleApiResults(results: List[GoogleApiResult])

private[googleapi] object GoogleApiJsonResultProtocol extends DefaultJsonProtocol {
  implicit val googleApiAddressComponentFormat = jsonFormat3(GoogleApiAddressComponent)
  implicit val googleApiResultFormat = jsonFormat1(GoogleApiResult)
  implicit def googleApiResultsFormat = jsonFormat1(GoogleApiResults.apply)
}
