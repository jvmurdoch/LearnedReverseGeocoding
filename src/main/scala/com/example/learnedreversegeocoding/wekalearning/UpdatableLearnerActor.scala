package com.example.learnedreversegeocoding.wekalearning

import akka.actor.Actor
import com.example.learnedreversegeocoding._

/**
 * Class: UpdatableLearnerActor
 * Actor managing training and prediction of a
 * WEKA NaiveBayesUpdatable learning model
 *
 * Created by jmurdoch on 2014-06-16.
 */
private[learnedreversegeocoding] class UpdatableLearnerActor extends Actor{

  import akka.event.Logging
  import weka.core.{Instance, Attribute, Instances, FastVector}
  import weka.classifiers.bayes.NaiveBayesUpdateable
  import BrokeredActorMessages.{LEARNERPredictResult, BROKERLearnerPredict, BROKERLearnerTrain, BROKERLearnerClearModel}
  import domain.{ReverseGeocoding2Params, ReverseGeocoding3Params}

  implicit val system = context.system
  val log = Logging(system, getClass)

  var dataset: Instances = null
  var nb: NaiveBayesUpdateable = null

  override def preStart() = initNewModel()

  def receive = {
    // Re-initialize the model
    case BROKERLearnerClearModel() =>
      initNewModel()

    // Train the model with data instances that have not
    // been used to train the current model
    case BROKERLearnerTrain(data: List[ReverseGeocoding3Params]) =>
      data.foreach(rgp =>
        if (!alreadyTrained(rgp)) {
          try {
            nb.updateClassifier(createTrainInstance(rgp))
            log.info("Training {}, {} => Updated Training Set Size: {}", rgp.lat, rgp.lon, dataset.numInstances())
          }
          catch {
            case e: Throwable => log.warning("Error during update of classifier: {} - {}", rgp, e)
          }
        }
      )

    // If the current model has been trained with at least 1 instance,
    // use it to predict a postal code
    case BROKERLearnerPredict(rgp: ReverseGeocoding2Params) =>
      try {
        if (dataset.numInstances() > 0) {
          val classIndex = nb.classifyInstance(createPredictInstance(rgp)).toInt
          log.info("Predicted POSTAL_CODE: {}", PostalCodeDomain.USA(classIndex))
          sender ! LEARNERPredictResult(ReverseGeocoding3Params(rgp.lat, rgp.lon, PostalCodeDomain.USA(classIndex)))
        }
        else {
          sender ! LEARNERPredictResult(ReverseGeocoding3Params(rgp.lat, rgp.lon, null))
        }
      }
      catch
        {
          case e : Throwable =>
            log.warning("Error during classifier predict: {} - {}", rgp, e)
            sender ! LEARNERPredictResult(ReverseGeocoding3Params(rgp.lat, rgp.lon, null))
        }
  }

  // Initialize a fresh untrained model
  def initNewModel() = {
    dataset = createInstances()
    nb = new NaiveBayesUpdateable()
    nb.buildClassifier(dataset)

    log.info("Reinitializing the Model")
  }

  // Initialize a description of the modelled problem
  def createInstances() : Instances = {

    val attPostalCodes: FastVector = new FastVector()
    PostalCodeDomain.USA.foreach(pc => attPostalCodes.addElement(pc))

    val atts: FastVector = new FastVector()
    atts.addElement(new Attribute("lat"))
    atts.addElement(new Attribute("lon"))
    atts.addElement(new Attribute("postal_code", attPostalCodes))

    val instances = new Instances("postal-codes", atts, 0)
    instances.setClassIndex(instances.numAttributes()-1)
    instances
  }

  // Determine if the data point has already been
  // used in training the current model
  def alreadyTrained(rgp: ReverseGeocoding3Params) : Boolean = {
    val iter = dataset.enumerateInstances()
    while (iter.hasMoreElements) {
      var ar = iter.nextElement().asInstanceOf[Instance].toDoubleArray
      if (ar(0) == rgp.lat && ar(1) == rgp.lon) {
        log.info("Ignoring already trained instance: {}", rgp)
        return true
      }
    }
    false
  }

  // Create an instance for on-the-fly training/updating of the current model,
  // i.e. lat, lon, postal_code
  def createTrainInstance(rgp: ReverseGeocoding3Params) : Instance = {
    val vals: Array[Double] = Array(rgp.lat, rgp.lon, PostalCodeDomain.USA.indexOf(rgp.postal_code))
    val inst = new Instance(1.0, vals.map(java.lang.Double.valueOf(_).doubleValue()))
    inst.setDataset(dataset)
    dataset.add(inst)
    inst
  }

  // Create an instance for prediction, i.e. lat, lon
  def createPredictInstance(rgp: ReverseGeocoding2Params) : Instance = {
    val vals: Array[Double] = Array(rgp.lat, rgp.lon)
    val inst = new Instance(1.0, vals.map(java.lang.Double.valueOf(_).doubleValue()))
    inst.setDataset(dataset)
    inst
  }
}
