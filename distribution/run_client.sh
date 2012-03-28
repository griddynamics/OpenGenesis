#!/bin/sh
VERSION=1.0-SNAPSHOT
TARGET=./target/genesis-${VERSION}
java -cp "${TARGET}/*" com.griddynamics.genesis.client.Client $*

