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
	--bootstrap-server $DOCKER_HOST_IP:9092 \
	-g $DEPOSIT_GROUP \
	-t $DEPOSIT_TOPIC \
	-b $DEPOSIT_BUCKET_NAME \
	-k $DEPOSIT_KEY_NAME \
	-i $K_AWS_ACCESS_KEY_ID \
	-a $K_AWS_SECRET_ACCESS_SECRET \
	-n $K_AWS_SESSION_TOKEN \
	-e $K_AWS_EXPIRATION \
	-u $API_ENDPOINT \
	-h $HEADER_ACCEPT \
	-x $HEADER_X_API_KEY