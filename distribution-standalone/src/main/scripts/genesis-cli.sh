#!/bin/sh
# 
# Copyright (c) 2010-2013 Grid Dynamics Consulting Services, Inc, All Rights Reserved
# 
# OpenGenesis command line tool
#

declare quiet="false"

QUOTED_ARGS=""
while [ "$1" != "" ] ; do
  QUOTED_ARGS="$QUOTED_ARGS $1"
  shift
done


declare GENESIS_HOME="$(cd "$(cd "$(dirname "$0")"; pwd -P)"/..; pwd)"

[ -n "$GENESIS_CLASSPATH" ] || GENESIS_CLASSPATH="$GENESIS_HOME/lib/*"

[ -n "$JAVA_OPTS" ] || JAVA_OPTS="-XX:MaxPermSize=400M"

java $JAVA_OPTS -cp "$GENESIS_CLASSPATH" -Dgenesis_home=$GENESIS_HOME -Dbackend.properties="file:$GENESIS_HOME/conf/genesis.properties" -Dlogback.configurationFile="$GENESIS_HOME/conf/logback-cli.xml" -Dakka.loglevel="ERROR" com.griddynamics.genesis.cli.GenesisShell $QUOTED_ARGS
