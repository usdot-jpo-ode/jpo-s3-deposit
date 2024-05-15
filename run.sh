#!/bin/bash

# note: must be run from the project root folder

# set env variables
envFile="$PWD/.env"
source "$envFile"
export BOOTSTRAP_SERVER=$BOOTSTRAP_SERVER
export DEPOSIT_GROUP=$DEPOSIT_GROUP
export DEPOSIT_TOPIC=$DEPOSIT_TOPIC
export DESTINATION=$DESTINATION
export DEPOSIT_BUCKET_NAME=$DEPOSIT_BUCKET_NAME
export REGION=$REGION
export DEPOSIT_KEY_NAME=$DEPOSIT_KEY_NAME
export AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID
export AWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY
export AWS_SESSION_TOKEN=$AWS_SESSION_TOKEN
export AWS_EXPIRATION=$AWS_EXPIRATION
export HEADER_ACCEPT=$HEADER_ACCEPT
export HEADER_X_API_KEY=$HEADER_X_API_KEY
export API_ENDPOINT=$API_ENDPOINT
export KAFKA_TYPE=$KAFKA_TYPE
export CONFLUENT_KEY=$CONFLUENT_KEY
export CONFLUENT_SECRET=$CONFLUENT_SECRET
export GOOGLE_APPLICATION_CREDENTIALS=$GOOGLE_APPLICATION_CREDENTIALS

# build
echo "Compiling."
mvn clean package assembly:single

# run
echo "Executing."
java -Dlogback.configurationFile=./src/main/resources/logback.xml \
	-jar ./target/jpo-aws-depositor-jar-with-dependencies.jar