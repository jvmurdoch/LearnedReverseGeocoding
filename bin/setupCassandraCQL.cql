DROP KEYSPACE reversegeocodingks;
CREATE KEYSPACE reversegeocodingks WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };
USE reversegeocodingks;
CREATE TABLE tuples ( lat double, lon double, postal_code text, PRIMARY KEY (lat, lon) );






