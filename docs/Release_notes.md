Jpo-s3-deposit Release Notes
----------------------------

Version 1.7.0, released January 2025
----------------------------------------
### **Summary**
The jpo-s3-deposit 1.7.0 release updates GitHub Actions workflows with the latest versions of third-party actions from external repositories to eliminate Node.js and other deprecation warnings.

Enhancements in this release:
- [USDOT PR 67](https://github.com/usdot-jpo-ode/jpo-s3-deposit/pull/67): Update GitHub Actions Third-Party Action Versions

Known Issues:
- No known issues at this time.


Version 1.6.0, released September 2024
----------------------------------------
### **Summary**
The changes for the jpo-s3-deposit 1.6.0 release include a GitHub action to publish a java artifact to the GitHub repository whenever a release is created, a change to the default value for the enable.auto.commit property to 'true', unit tests, and revised documentation for accuracy & clarity.

Enhancements in this release:
- CDOT PR 23: Added GitHub action to publish a java artifact to the GitHub repository whenever a release is created
- CDOT PR 25: Changed default value for enable.auto.commit property to 'true'
- CDOT PR 26: Added unit tests
- CDOT PR 27: Revised documentation for accuracy & clarity


Version 1.5.0, released June 2024
----------------------------------------
### **Summary**
The changes for the jpo-s3-deposit 1.5.0 release include updated hard-coded Kafka properties to be configurable, added support for GCS BLOB storage & an updated Java version for the dev container.

Enhancements in this release
- CDOT PR 18: Updated hard-coded Kafka properties to be configurable
- CDOT PR 19: Added support for GCS BLOB storage
- CDOT PR 22: Updated dev container Java version to 21

Known Issues:
- No known issues at this time.


Version 1.4.0, released February 2024
----------------------------------------

### **Summary**
The changes for the jpo-s3-deposit 1.4.0 release include a log4j initialization fix, an update for Java, dockerhub image documentation & a MongoDB connector implementation.

Enhancements in this release:
- CDOT PR 12: Fixed log4j initialization issues
- CDOT PR 11: Updated Java to v21
- CDOT PR 10: Added dockerhub image documentation
- CDOT PR 9: Implemented MongoDB Connector

Known Issues:
- No known issues at this time.


Version 1.3.0, released November 2023
----------------------------------------

### **Summary**
The updates for the jpo-s3-deposit 1.3.0 release consist of fixed GitHub workflow job names and adjustments to the run.sh script to reference the correct JAR file.
- Fixed github workflow job names.
- The run.sh script has been modified to point to the accurate JAR file.

Known Issues:
- No known issues at this time.


Version 1.2.0, released July 5th 2023
----------------------------------------

### **Summary**
The updates for jpo-s3-deposit 1.2.0 include CI/CD and dependency changes.

Enhancements in this release:
- The JSON version being used has been bumped to 20230227.
- CI/CD has been added.

Known Issues
- The `run.sh` script incorrectly references the built JAR and does not work at this time. The dockerfile correctly references the built JAR, however, so this is non-critical.
  
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
