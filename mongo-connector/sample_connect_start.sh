# bin/bash
echo "------------------------------------------"
echo "Kafka connector creation started."
echo "------------------------------------------"

declare -A testing=([name]="testing" [collection]="testing"
    [convert_timestamp]=false [timefield]="" [use_key]=false [key]="")

function createSink() {
    local -n topic=$1
    local name=${topic[name]}
    local collection=${topic[collection]}
    local timefield=${topic[timefield]}
    local convert_timestamp=${topic[convert_timestamp]}
    local use_key=${topic[use_key]}
    local key=${topic[key]}

    echo "Creating sink connector with parameters:"
    echo "name=$name"
    echo "collection=$collection"
    echo "timefield=$timefield"
    echo "convert_timestamp=$convert_timestamp"
    echo "use_key=$use_key"
    echo "key=$key"

    local connectConfig=' {
        "group.id":"connector-consumer",
        "connector.class":"com.mongodb.kafka.connect.MongoSinkConnector",
        "tasks.max":3,
        "topics":"'$name'",
        "connection.uri":"'$MONGO_URI'",
        "database":"ODE",
        "collection":"'$collection'",
        "key.converter":"org.apache.kafka.connect.storage.StringConverter",
        "key.converter.schemas.enable":false,
        "value.converter":"org.apache.kafka.connect.json.JsonConverter",
        "value.converter.schemas.enable":false,
        "errors.tolerance": "all",
        "mongo.errors.tolerance": "all",
        "errors.deadletterqueue.topic.name": "dlq.'$collection'.sink",
        "errors.deadletterqueue.context.headers.enable": true,
        "errors.log.enable": true,
        "errors.log.include.messages": true,
        "errors.deadletterqueue.topic.replication.factor": 1'    


    if [ "$convert_timestamp" == true ]
    then
        local connectConfig=''$connectConfig',
        "transforms": "TimestampConverter",
        "transforms.TimestampConverter.field": "'$timefield'",
        "transforms.TimestampConverter.type": "org.apache.kafka.connect.transforms.TimestampConverter$Value",
        "transforms.TimestampConverter.target.type": "Timestamp"'
    fi

    if [ "$use_key" == true ]
    then
        local connectConfig=''$connectConfig',
        "document.id.strategy": "com.mongodb.kafka.connect.sink.processor.id.strategy.PartialValueStrategy",
        "document.id.strategy.partial.value.projection.list": "'$key'",
        "document.id.strategy.partial.value.projection.type": "AllowList",
        "document.id.strategy.overwrite.existing": true'
    fi

    local connectConfig=''$connectConfig' }'

    echo " Creating connector with Config : $connectConfig"

    curl -X PUT http://localhost:8083/connectors/MongoSink.${name}/config -H "Content-Type: application/json" -d "$connectConfig"
}

createSink testing

echo "----------------------------------"
echo "ODE Kafka connector creation complete!"
echo "----------------------------------"