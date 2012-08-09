:: Batch file needed to run all REST API tests from console in Windows systems

set javaTestProjectPath=%CD%
set classpath=%javaTestProjectPath%\bin;%javaTestProjectPath%\lib\*
java -Dhost=172.18.128.49 -Dport=9094 -Dcredentials.path=\path\to\jclouds\credentials\ -cp %classpath% org.testng.TestNG testng.xml