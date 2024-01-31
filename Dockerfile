FROM maven:3.8-eclipse-temurin-21-alpine as builder

WORKDIR /home
COPY ./pom.xml .
COPY ./src ./src

RUN mvn clean package assembly:single

FROM eclipse-temurin:21-jre-alpine

COPY --from=builder /home/src/main/resources/logback.xml /home
COPY --from=builder /home/src/main/resources/log4j.properties /home
COPY --from=builder /home/target/jpo-aws-depositor-jar-with-dependencies.jar /home


CMD java -Dlogback.configurationFile=/home/logback.xml \
	-jar /home/jpo-aws-depositor-jar-with-dependencies.jar \