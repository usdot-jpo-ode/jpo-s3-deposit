FROM ubuntu:18.04

RUN apt-get update && \
    apt-get install -y software-properties-common git
RUN apt-get update && \
    apt-get install -y apt-utils
RUN apt-get update && \
    apt-get install -y wget supervisor dnsutils curl jq net-tools
RUN apt-get update && \
    apt-get install -y openjdk-8-jdk
RUN apt-get update && \
    apt-get install -y vim
RUN apt-get update && \
    apt-get install -y nano
RUN apt-get update && \
   apt-cache search maven && apt-get install -y maven
	
RUN apt-get clean

ADD . /home/jpo-s3-deposit
RUN cd /home/jpo-s3-deposit && mvn package assembly:single

CMD java -jar /home/jpo-s3-deposit/target/jpo-aws-depositor-0.0.1-SNAPSHOT-jar-with-dependencies.jar --bootstrap-server $DOCKER_HOST_IP:9092 -g group1 -t $DEPOSIT_TOPIC -b $DEPOSIT_BUCKET_NAME -k $DEPOSIT_KEY_NAME
