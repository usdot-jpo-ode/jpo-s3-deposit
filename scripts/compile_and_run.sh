# note: must be run from the project root folder

# compile
echo "Compiling."
mvn clean package assembly:single
echo "Finished compiling."

# run
@echo "Executing."
java -Dlogback.configurationFile=./src/main/resources/logback.xml \
	-jar ./target/jpo-aws-depositor-with-dependencies.jar \
	--bootstrap-server $DOCKER_HOST_IP:9092 \
	-g group1 \
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
echo "Finished executing."