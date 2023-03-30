Jpo-s3-deposit Release Notes
----------------------------

Version 1.1.0, released Mar 30th 2023
----------------------------------------

### **Summary**
The updates for jpo-s3-deposit 1.1.0 include Confluent Cloud integration, some fixes and documentation improvements.

Enhancements in this release:
- Ensured response and client objects were closed.
-	Allowed the depositor to connect to an instance of Kafka hosted by Confluent Cloud.
-	Added docker & dev container files.
-	Added a simple run script.
-	Added a launch configuration.
-	Utilized kafka_2.11 library instead of kafka-clients library.
-	Added a section to the README on Confluent Cloud Integration.
-	Added sections to the README on the run script and docker-compose files.
-	Added a section on launch configurations to the README.
-	Updated base image to eclipse-temurin:11-jre-alphine rather than the deprecated openjdk:8-jre-alpine image.
-	Updated the version in the pom.xml to 1.1.0 to match the version being used for release.

Fixes in this release:
-	Fixed a bug with Dockerfile not carrying the deposit group name.
-	Swapped out bullseye version.
-	Removed unnecessary static from AwsDepositor.java.

Known Issues
-	There are no known issues at this time.

Version 1.0.1, released Mar 8th 2021
----------------------------------------

### **Summary**
This release is to direct logging output to the console rather than a physical file.

Version 1.0.0, released Oct 9th 2020
----------------------------------------

### **Summary**
his release marks the resolution of issues #19 and #27 as well as providing compatibility with the 1.2.0 release of jpo-ode.
