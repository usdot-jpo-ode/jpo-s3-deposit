FROM maven:3.5.4-jdk-8-alpine as builder
MAINTAINER 583114@bah.com

WORKDIR /home
COPY . .

RUN mvn clean package assembly:single

FROM openjdk:8u171-jre-alpine

COPY --from=builder /home/target/jpo-aws-depositor-0.0.1-SNAPSHOT-jar-with-dependencies.jar /home

CMD ["java", "-jar", "/home/jpo-aws-depositor-0.0.1-SNAPSHOT-jar-with-dependencies.jar", "--bootstrap-server", "$DOCKER_HOST_IP:9092", "-g", "group1", "-t", "$DEPOSIT_TOPIC", "-b", "$DEPOSIT_BUCKET_NAME", "-k", "$DEPOSIT_KEY_NAME"]
