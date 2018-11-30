FROM maven:3.5.4-jdk-8-alpine as builder
MAINTAINER 583114@bah.com

WORKDIR /home
COPY ./pom.xml .
COPY ./src ./src

RUN mvn clean package assembly:single

FROM openjdk:8u171-jre-alpine

COPY --from=builder /home/src/main/resources/logback.xml /home
COPY --from=builder /home/target/jpo-aws-depositor-0.0.1-SNAPSHOT-jar-with-dependencies.jar /home

CMD java -Dlogback.configurationFile=/home/logback.xml -jar /home/jpo-aws-depositor-0.0.1-SNAPSHOT-jar-with-dependencies.jar --bootstrap-server 192.168.1.12:9092 -g group1 -t $DEPOSIT_TOPIC -b $DEPOSIT_BUCKET_NAME -k $DEPOSIT_KEY_NAME
