FROM ubuntu:18.04

RUN apt-get update && \
 apt-get install -y openjdk-8-jdk

ADD . /home/jpo-s3-deposit

CMD java -jar /home/jpo-s3-deposit/target/consumer-example-0.0.1-SNAPSHOT-jar-with-dependencies.jar --bootstrap-server $DOCKER_HOST_IP:9092 -g group1 -t $DEPOSIT_TOPIC -b $DEPOSIT_BUCKET_NAME -k $DEPOSIT_KEY_NAME -type string -d $DEPOSIT_OPTION 
