# jpo-s3-deposit

## GitHub Repository Link
https://github.com/usdot-jpo-ode/jpo-s3-deposit

## Purpose
The purpose of the jpo-s3-deposit program is to deposit messages to permanent storage, such as an Amazon S3 bucket, MongoDB instance or other similar system.

## How to pull the latest image
The latest image can be pulled using the following command:
> docker pull usdotjpoode/jpo-s3-deposit:latest

## Required environment variables
- DOCKER_HOST_IP
- DEPOSIT_GROUP
- DEPOSIT_TOPIC
- DEPOSIT_BUCKET_NAME
- DEPOSIT_KEY_NAME
- API_ENDPOINT
- HEADER_ACCEPT
- HEADER_X_API_KEY

## Direct Dependencies
The S3D will fail to start up if the following containers are not already present:
- Kafka
- Zookeeper (relied on by Kafka)

## Indirect Dependencies
The S3D will not receive messages to process if the ODE is not running.

## Example docker-compose.yml with direct dependencies:
```
version: '2'
services:
  zookeeper:
    image: wurstmeister/zookeeper
    ports:
      - "2181:2181"

  kafka:
    image: wurstmeister/kafka
    ports:
      - "9092:9092"
    environment:
      KAFKA_ADVERTISED_HOST_NAME: ${DOCKER_HOST_IP}
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_CREATE_TOPICS: "test:1:1"
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock

  s3d:
    image: usdotjpoode/jpo-s3-deposit:release_q3
    environment:
      # required
      DOCKER_HOST_IP: ${DOCKER_HOST_IP}
      DEPOSIT_GROUP: ${DEPOSIT_GROUP}
      DEPOSIT_TOPIC: ${DEPOSIT_TOPIC}
      DEPOSIT_BUCKET_NAME: ${DEPOSIT_BUCKET_NAME}
      DEPOSIT_KEY_NAME: ${DEPOSIT_KEY_NAME}
      API_ENDPOINT: ${API_ENDPOINT}
      HEADER_ACCEPT: ${HEADER_ACCEPT}
      HEADER_X_API_KEY: ${HEADER_X_API_KEY}
      # optional
      AWS_SESSION_TOKEN: ${AWS_SESSION_TOKEN}
      AWS_EXPIRATION: ${AWS_EXPIRATION}
      AWS_ACCESS_KEY_ID: ${AWS_ACCESS_KEY_ID}
      AWS_SECRET_ACCESS_KEY: ${AWS_SECRET_ACCESS_KEY}
    depends_on:
      - kafka
```

## Expected startup output.
The latest logs should look like this:
```
log4j:WARN No appenders could be found for logger (us.dot.its.jpo.ode.aws.depositor.AwsDepositor).
log4j:WARN Please initialize the log4j system properly.
log4j:WARN See http://logging.apache.org/log4j/1.2/faq.html#noconfig for more info.
```
