#!/bin/sh

export JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk1.8.0_05.jdk/Contents/Home"
./cassandra/bin/cqlsh -f ./bin/setupCassandraCQL.cql

