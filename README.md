
Reverse Geocoding using Machine Learning Demo Application
(J. Murdoch - jv.murdoch@gmail.com)
Monday, June 16, 2014

	Table of Contents
	1.	Introduction
	2.	Architecture Overview
	3.	REST Service Endpoints
	4.	Installation and Setup


	1.	Introduction

	This simple demo application illustrates a solution to a contrived learning problem: Assuming that a description of the 
	boundaries of each US ZIP code is not available, while a web service that provides the ZIP code assignment given a 
	provided GIS point (i.e. a Reverse Geocoder) is only intermittently available, can we build a simple reverse geocoder 
	that is learned-system driven rather than data driven?  The intention of this application is not to build a tool that is 
	even remotely useful, but rather, to demonstrate the application of a range of concepts and technologies:

	•	Akka and the Actor model, 
	•	Spray and REST services, 
	•	Cassandra and distributed databases, 
	•	JSON and marshalling/unmarshalling,
	•	Network query response caching, and
	•	WEKA and simple on-the-fly machine learning.


	2.	Architecture Overview

The system is composed of the following components:

	1.	A RESTful web service to query the system
	2.	A Cassandra database providing a persistent cache of Reverse Geocoding queries made to a Google API: 
	    https://maps.googleapis.com/maps/api/geocode
	3.	A Weka implementation of a Naive Bayes classifier that is incrementally trained according to queries to the REST API

Each time a user queries the REST API to train or predict a US postal code, a lookup for the groundtruth postal code is 
performed.  The system first checks the local cache of queries, and if not present, will proceed to query Google’s geocoding
API for a value.  If a groundtruth US ZIP code is found, the value will be persisted to the local cache and also used as 
input to incrementally train the learning model.  The system performs gracefully both when the Cassandra database connection
is lost (i.e. when the cache is not accessible) and when used locally in the absence of consistent internet connectivity or 
access to the Google API.


	3.	REST Service Endpoints

  The following is a list of the prescribed high-level requirements of the system’s REST API:

	•	The server application supports a REST web service that provides two endpoints supplying JSON-formatted responses:

	  ◦	POST /reverse_geocode?lat=YYY&lon=XXX
	    ▪	Performs a lookup (from the cache or the Google geocoding API) of the actual Postal Code represented by the GIS 
	      point (latitude, longitude), and uses this to train the system in a supervised manner.  The response includes the 
	      ground truth Postal Code for the GIS point.

	  ◦	GET /reverse_geocode?lat=YYY&lon=XXX
	    ▪	Returns the predicted Postal Code, according to a Naive Bayes Classifier, of the GIS point along with the actual 
	      Postal Code for the point.  The trained system model is the result of all training input provided to the system 
	      since the last call to POST /reverse_geocode/clear_model

	  ◦	POST /reverse_geocode/clear_model
	    ▪	Reinitialize the learned-system model.  The query cache is not modified.

	  ◦	POST /reverse_geocode/clear_cache
	    ▪	Clear the database-cached query results.  The learned-system model is not modified.

	  ◦	POST /reverse_geocode/retrain_model
	    ▪	Retrains the learned-system model using all cached results (from Google API calls) currently stored in the database.


	4.	Installation and Setup

This installation guide assumes you are installing/running the application on Mac OSX 10.8+.

1. If you don’t already have it installed, install Java 8 (required by the Cassandra) from here:
    http://www.oracle.com/technetwork/java/javase/downloads/index.html
	◦	Once installed, ensure that it is the latest Java 8 installation that is referenced from the commandline: 
	⁃	$which java

2. If you don’t already have it installed, install Scala from here: http://www.scala-lang.org

3. Download and extract Cassandra 2.0.8 from here: http://cassandra.apache.org/download
	◦	Move the extracted Cassandra directory inside the top-level directory of the LearnedReverseGeocoding project.
	◦	Ensure the Cassandra directory is called/renamed to: [DEMO_APP_DIR]/cassandra

4. Start Cassandra: 
	⁃	$cd [DEMO_APP_DIR]
	⁃	$./bin/startCassandra.sh
  	◦	If an error occurs during startup, ensure that the Java 8 installation Home directory is referenced by the 
  	  $JAVA_HOME environment variable, e.g. $export JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk1.8.0_05.jdk/Contents/Home"

5. Navigate to the top-level directory of this demo application

6. With Cassandra running, run the following script to setup the required Cassandra Keyspace and Table: 
	⁃	$cd [DEMO_APP_DIR]
	⁃	$./bin/setupCassandra.sh

7. Use the Scala Build Tool (SBT) to build and run the project:
	⁃	$sbt
	⁃	> compile
	⁃	> run
	

