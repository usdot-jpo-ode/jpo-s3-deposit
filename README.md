# jpo-s3-deposit (Message Deposit Service)
This project is intended to serve as a consumer application to subscribe to a Kafka topic of streaming JSON, package the results as a JSON file, and deposit the resulting file into a predetermined Firehose/Kinesis, S3 Bucket, or Google Cloud Storage Bucket (GCS). This runs alongside the ODE and when deployed using Docker Compose, runs in a Docker container.

## Table of Contents
- [Release Notes](#release-notes)
- [Usage](#usage)
- [Installation](#installation)
- [Configuration](#configuration)
- [Debugging](#debugging)
- [Testing](#testing)

## Release Notes
The current version and release history of the Jpo-s3-deposit: [Jpo-s3-deposit Release Notes](<docs/Release_notes.md>)

## Usage
### Run with Docker
1. Create a copy of `sample.env` and rename it to `.env`.
2. Update the variable `DOCKER_HOST_IP` to the local IP address of the system running docker and set an admin user password with the `MONGO_DB_PASS` variable.
   1. If connecting to a separately deployed mongo cluster make sure to specify the `MONGO_IP` and `MONGO_PORT`.
3. Navigate back to the root directory and run the following command: `docker compose -f docker-compose-mongo.yml up -d`
4. Using either a local kafka install or [kcat](https://github.com/edenhill/kcat) to produce a sample message to one of the sink topics. Optionally, you can separately run the [ODE](https://github.com/usdot-jpo-ode/jpo-ode) and process messages directly from it's output.
5. Using [MongoDB Compass](https://www.mongodb.com/products/compass) or another DB visualizer connect to the MongoDB database using this connection string: `mongodb://[admin_user]:[admin_password]@localhost:27017/`
6. Now we are done! If everything is working properly you should see an ODE database with a collection for each kafka sink topic that contains messages.

### Run manually
The use of AWS credentials is being read from the machine's environment variables. You may also set them in your bash profile. Note that when using Docker Compose from the main `jpo-ode` repository, these variables are set in the `.env` present in that repo.

If depositing to GCS, credentials are read from a JSON service account key file. A sample service account file can be found at ./resources/google/sample_gcp_service_account.json. 
Please note that if depositing to GCS the service account will need the storage.buckets.get and storage.objects.create permissions.

```
export K_AWS_ACCESS_KEY_ID = AccessKeyId
export K_AWS_SECRET_ACCESS_SECRET = SecretAccessKey
export K_AWS_SESSION_TOKEN = SessionToken
export K_AWS_EXPIRATION = Expiration
export API_ENDPOINT = <API_ENDPOINT>
export HEADER_ACCEPT = application/json
export HEADER_X_API_KEY = <HEADER_X_API_KEY>
```

The project needs to be compiled with assembly to ensure that that resulting jar is runnable with the Kafka libraries. It will produce a jar under `target/` with a "with-dependencies" tag.

```
mvn clean compile assembly:single install
```

To run the jar, be sure to include the topic at the end and group id at the end. If this is not a distributed system, the group can be any string.

Triggering an upload into the ODE, the output should be seen decoded into JSON in the console.

![CLI-output](images/cli-output.png)

## Installation
### Run Script
The run.sh script can be utilized to run the project. This script will export the necessary environment variables, compile the project, and run the jar file.

It should be noted that this script must be run from the project root folder, or it will not work.

### Launch Configurations
A launch.json file with some launch configurations have been included to allow developers to debug the project in VSCode.

The values between braces < > are stand-in and need to be replaced by the developer. 

To run the project through the launch configuration and start debugging, the developer can navigate to the Run panel (View->Run or Ctrl+Shift+D), select the configuration at the top, and click the green arrow or press F5 to begin.

### Docker Compose Files
The docker-compose.yml file can be used to spin up the depositor as a container, along with instances of kafka and zookeeper.

The docker-compose-confluent-cloud.yml file can be used to spin up the depositor as a container by itself. This depends on an instance of kafka hosted by Confluent Cloud.

## Configuration
### Confluent Cloud Integration
Rather than using a local kafka instance, this project can utilize an instance of kafka hosted by Confluent Cloud via SASL.

#### Environment variables
##### Purpose & Usage
- The DOCKER_HOST_IP environment variable is used to communicate with the bootstrap server that the instance of Kafka is running on.
- The KAFKA_TYPE environment variable specifies what type of kafka connection will be attempted and is used to check if Confluent should be utilized.
- The CONFLUENT_KEY and CONFLUENT_SECRET environment variables are used to authenticate with the bootstrap server.

##### Values
- DOCKER_HOST_IP must be set to the bootstrap server address (excluding the port)
- KAFKA_TYPE must be set to "CONFLUENT"
- CONFLUENT_KEY must be set to the API key being utilized for CC
- CONFLUENT_SECRET must be set to the API secret being utilized for CC

#### CC Docker Compose File
There is a provided docker-compose file (docker-compose-confluent-cloud.yml) that passes the above environment variables into the container that gets created. Further, this file doesn't spin up a local kafka instance since it is not required.

##### Note
This has only been tested with Confluent Cloud but technically all SASL authenticated Kafka brokers can be reached using this method.

### MongoDB Deposit Service
The mongo-connector service connects to specified Kafka topics (as defined in the mongo-connector/connect_start.sh script) and deposits these messages to separate collections in the MongoDB Database. The codebase that provides this functionality comes from Confluent using their community licensed [cp-kafka-connect image](https://hub.docker.com/r/confluentinc/cp-kafka-connect). Documentation for this image can be found [here](https://docs.confluent.io/platform/current/connect/index.html#what-is-kafka-connect).

Provided in the mongo-connector directory is a sample configuration shell script ([connect_start.sh](./mongo-connector/connect_start.sh)) that can be used to create kafka connectors to MongoDB. The connectors in kafka connect are defined in the format that follows:
``` shell
declare -A config_name=([name]="topic_name" [collection]="mongo_collection_name"
    [convert_timestamp]=true [timefield]="timestamp" [use_key]=true [key]="key" [add_timestamp]=true)
```
The format above describes the basic configuration for configuring a sink connector, this should be placed at the beginning of the connect_start.sh file. In general we recommend to keep the MongoDB collection name the same as the topic name to avoid confusion. Additionally, if there is a top level timefield set `convert_timestamp` to true and then specify the time field name that appears in the message. This will allow MongoDB to transform that message into a date object to allow for TTL creation and reduce message size. To override MongoDB's default message `_id` field, set `use_key` to true and then set the `key` property to "key". The "add_timestamp" field defines whether the connector will add a auto generated timestamp to each document. This allows for creation of Time To Live (TTL) indexes on the collections to help limit collection size growth. 

After the sink connector is configured above, then make sure to call the createSink function with the config_name of the configuration like so:
``` shell
createSink config_name
```
This needs to be put after the createSink function definition.

## Debugging
If the Kafka connect image crashes with the following error:
``` bash
bash: /scripts/connect_wait.sh: /bin/bash^M: bad interpreter: No such file or directory
```
Please verify that the line endings in the ([connect_start.sh](./mongo-connector/connect_start.sh)) and ([connect_wait.sh](./mongo-connector/connect_wait.sh)) are set to LF instead of CRLF.

## Testing
### Unit Tests
To run the unit tests, reopen the project in the provided dev container and run the following command:
``` bash
mvn test
```
This will run the unit tests and provide a report of the results.
