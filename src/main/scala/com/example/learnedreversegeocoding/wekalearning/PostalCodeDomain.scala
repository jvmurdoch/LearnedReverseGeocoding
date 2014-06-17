package com.example.learnedreversegeocoding.wekalearning

/**
 * Object: PostalCodeDomain
 * Storage of the list of all possible US zip codes,
 * i.e. the list of all possible class labels for the Updatable Learner Model
 *
 * Created by jmurdoch on 2014-06-16.
 */
private[learnedreversegeocoding] object PostalCodeDomain {

  val USA: List[String] = {
    (for (i <- 10000 to 99999) yield i.toString).toList
  }
}
