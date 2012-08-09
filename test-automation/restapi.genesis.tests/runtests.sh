# Shell file needed to run all REST API tests from console in Linux systems
#!/bin/bash

javaTestProjectPath=$(pwd)
classpath=$javaTestProjectPath/bin:$javaTestProjectPath/lib/*
java -Dhost=localhost -Dport=9094 -Dcredentials.path=/path/to/jclouds/credentials/ -cp $classpath org.testng.TestNG testng.xml
