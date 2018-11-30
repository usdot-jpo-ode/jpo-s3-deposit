FROM ubuntu:18.04

WORKDIR /home
COPY ./pom.xml .
COPY ./src ./src
	
RUN apt-get clean

ADD . /home/jpo-s3-deposit
RUN cd /home/jpo-s3-deposit && mvn package assembly:single

COPY --from=builder /home/src/main/resources/logback.xml /home
COPY --from=builder /home/target/jpo-aws-depositor-0.0.1-SNAPSHOT-jar-with-dependencies.jar /home

CMD java -Dlogback.configurationFile=/home/logback.xml -jar /home/jpo-aws-depositor-0.0.1-SNAPSHOT-jar-with-dependencies.jar --bootstrap-server 192.168.1.12:9092 -g group1 -t $DEPOSIT_TOPIC -b $DEPOSIT_BUCKET_NAME -k $DEPOSIT_KEY_NAME
