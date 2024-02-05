# Note that this image requires a version of Mongodb of 3.6 or later
FROM confluentinc/cp-kafka-connect:6.1.9

COPY connect_wait.sh /scripts/connect_wait.sh

# Docs: https://www.mongodb.com/docs/kafka-connector/current/
RUN confluent-hub install --no-prompt mongodb/kafka-connect-mongodb:1.11.1
# Docs: https://docs.confluent.io/platform/current/connect/transforms/overview.html
RUN confluent-hub install --no-prompt confluentinc/connect-transforms:1.4.3

CMD ["bash", "-c", "/scripts/connect_wait.sh"]