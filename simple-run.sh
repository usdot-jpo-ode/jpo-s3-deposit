echo "Executing."
java -Dlogback.configurationFile=./src/main/resources/logback.xml -jar ./target/jpo-aws-depositor-jar-with-dependencies.jar --bootstrap-server ${DOCKER_HOST_IP}:9092 -g ${DEPOSIT_GROUP} -t ${DEPOSIT_TOPIC} -b ${DEPOSIT_BUCKET_NAME} -k ${DEPOSIT_KEY_NAME} -d s3
echo "Finished executing."