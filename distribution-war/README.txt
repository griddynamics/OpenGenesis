========================================
 Genesis Web Archive (WAR) Distribution
========================================

This archive is intended to be deployed into servlet container like Tomcat. So far it is possible to use
only ROOT context ('/').


NOTE: Servlet Container should be run with system property 'backend.properties' which points to Genesis configuration file.
For example, to run Tomcat:

$ env CATALINA_OPTS='-Dbackend.properties=file:/etc/genesis' $TOMCAT_HOME/bin/startup.sh


To run using embedded Tomcat:

$ env MAVEN_OPTS="-Xmx512m -XX:MaxPermSize=256m" mvn tomcat7:run -Dbackend.properties=file:/etc/genesis