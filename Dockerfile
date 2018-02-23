FROM ubuntu:latest

ADD . /home/jpo-s3-deposit

RUN apt-get update && \
 apt-get install -y default-jdk

CMD java -jar /home/jpo-s3-deposit/target/consumer-example-0.0.1-SNAPSHOT-jar-with-dependencies.jar --bootstrap-server $DOCKER_HOST_IP:9092 -g group1 -t $DEPOSIT_TOPIC -s $DEPOSIT_BUCKET_NAME -k $DEPOSIT_KEY_NAME -type string -d $DEPOSIT_OPTION 
