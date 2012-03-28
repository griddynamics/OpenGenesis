#!/bin/sh
ENV=${1:-dev}
VERSION=1.0-SNAPSHOT
TARGET=./target/genesis-${VERSION}
java -Dbackend.properties=file:${TARGET}/environments/${ENV}.properties -cp "${TARGET}/*" com.griddynamics.genesis.GenesisFrontend

