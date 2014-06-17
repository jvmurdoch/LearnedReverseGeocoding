package com.example.learnedreversegeocoding.domain

/**
 * Domain objects used by actors for message passing,
 * and JSON formats for marshalling/unmarshalling
 *
 * Created by jmurdoch on 2014-06-16.
 */

import spray.json._

// Formats for JSON marshalling of domain objects
private[learnedreversegeocoding] object ReverseGeocodingParamsProtocol extends DefaultJsonProtocol {
  implicit val reverseGeocodingParams3Format = jsonFormat3(ReverseGeocoding3Params.apply)
  implicit val reverseGeocodingParams4Format = jsonFormat4(ReverseGeocoding4Params.apply)
}

// Domain objects
private[learnedreversegeocoding] case class ReverseGeocoding2Params(lat: Double, lon: Double)

private[learnedreversegeocoding] case class ReverseGeocoding3Params(lat: Double, lon: Double, var postal_code: String) {
  def this(rg2p: ReverseGeocoding2Params, postal_code: String) = this(rg2p.lat, rg2p.lon, postal_code)

  def prettyPrint() : String = {

    import ReverseGeocodingParamsProtocol.reverseGeocodingParams3Format

    var prettyRGP = this
    if (this.postal_code == null)
    {
      prettyRGP = ReverseGeocoding3Params(this.lat, this.lon, "NOT_AVAILABLE")
    }

    prettyRGP.toJson.prettyPrint
  }
}

private[learnedreversegeocoding] object ReverseGeocoding3Params {
  def apply(rg2p: ReverseGeocoding2Params, postal_code: String) = new ReverseGeocoding3Params(rg2p, postal_code)
}

private[learnedreversegeocoding] case class ReverseGeocoding4Params(lat: Double, lon: Double, var predicted_postal_code: String, var actual_postal_code: String) {

  def prettyPrint() : String = {

    import ReverseGeocodingParamsProtocol.reverseGeocodingParams4Format

    var predPc = predicted_postal_code
    var realPc = actual_postal_code
    if (this.predicted_postal_code == null)
    {
      predPc = "NOT_AVAILABLE"
    }
    if (this.actual_postal_code == null)
    {
      realPc = "NOT_AVAILABLE"
    }

    ReverseGeocoding4Params(this.lat, this.lon, predPc, realPc).toJson.prettyPrint
  }
}

