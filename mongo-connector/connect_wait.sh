#!/bin/bash

/etc/confluent/docker/run & 
echo "Waiting for Kafka Connect to start listening on kafka-connect"
while [ $(curl -s -o /dev/null -w %{http_code} http://localhost:8083/connectors) -eq 000 ] ; do 
    echo -e $(date) " Kafka Connect listener HTTP state: " $(curl -s -o /dev/null -w %{http_code} http://localhost:8083/connectors) " (waiting for 200)"
    sleep 5
done
sleep 10
echo -e "\n--\n+> Creating Kafka Connect MongoDB sink"

# Check if connect_start.sh exists
if [ ! -f /scripts/connect_start.sh ]; then
    echo "Error: connect_start.sh does not exist, starting without any connectors."
else
    echo "Connect_start.sh exists, starting with connectors."
    bash /scripts/connect_start.sh
fi

sleep infinity