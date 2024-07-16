/*******************************************************************************
 * Copyright 2018 572682
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package us.dot.its.jpo.ode.aws.depositor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.auth.SystemPropertiesCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehoseAsync;
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehoseAsyncClientBuilder;
import com.amazonaws.services.kinesisfirehose.model.PutRecordRequest;
import com.amazonaws.services.kinesisfirehose.model.PutRecordResult;
import com.amazonaws.services.kinesisfirehose.model.Record;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

public class AwsDepositor {
	private final Logger logger = LoggerFactory.getLogger(AwsDepositor.class);
	private final long CONSUMER_POLL_TIMEOUT_MS = 60000;

	private String endpoint;
	private String topic;
	private String group;
	private String destination;
	private String bucketName;
	private String awsRegion;
	private String keyName;
	private boolean waitOpt;

	private boolean runDepositor = true;

	private String K_AWS_ACCESS_KEY_ID;
	private String K_AWS_SECRET_ACCESS_KEY;
	private String K_AWS_SESSION_TOKEN;
	private String K_AWS_EXPIRATION;
	private String API_ENDPOINT;
	private String HEADER_Accept;
	private String HEADER_X_API_KEY;

	private String AWS_ACCESS_KEY_ID;
	private String AWS_SECRET_ACCESS_KEY;
	private String AWS_SESSION_TOKEN;
	private String AWS_EXPIRATION;

	private String KAFKA_ENABLE_AUTO_COMMIT;
	private String KAFKA_AUTO_COMMIT_INTERVAL_MS;
	private String KAFKA_SESSION_TIMEOUT_MS;

	public static void main(String[] args) throws Exception {
		AwsDepositor awsDepositor = new AwsDepositor();
		awsDepositor.run();
	}

	public void run() throws Exception {
		// Pull in environment variables
		depositorSetup();

		if (API_ENDPOINT.length() > 0) {
			JSONObject profile = generateAWSProfile();
			AWS_ACCESS_KEY_ID = profile.get(K_AWS_ACCESS_KEY_ID).toString();
			AWS_SECRET_ACCESS_KEY = profile.get(K_AWS_SECRET_ACCESS_KEY).toString();
			AWS_SESSION_TOKEN = profile.get(K_AWS_SESSION_TOKEN).toString();
			AWS_EXPIRATION = profile.get(K_AWS_EXPIRATION).toString().split("\\+")[0];
		}

		// Properties for the kafka topic
		Properties props = new Properties();
		props.put("bootstrap.servers", endpoint);

		props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

		String kafkaType = System.getenv("KAFKA_TYPE");
		if (kafkaType != null && kafkaType.equals("CONFLUENT")) {
			addConfluentProperties(props);
		}

		KAFKA_AUTO_COMMIT_INTERVAL_MS = getEnvironmentVariable("KAFKA_AUTO_COMMIT_INTERVAL_MS", "1000");
		KAFKA_ENABLE_AUTO_COMMIT = getEnvironmentVariable("KAFKA_ENABLE_AUTO_COMMIT", "true");
		KAFKA_SESSION_TIMEOUT_MS = getEnvironmentVariable("KAFKA_SESSION_TIMEOUT_MS", "30000");

		props.put("group.id", group);
		props.put("enable.auto.commit", KAFKA_ENABLE_AUTO_COMMIT);
		props.put("auto.commit.interval.ms", KAFKA_AUTO_COMMIT_INTERVAL_MS);
		props.put("session.timeout.ms", KAFKA_SESSION_TIMEOUT_MS);


		boolean depositToS3 = false;
		AmazonS3 s3 = null;
		AmazonKinesisFirehoseAsync firehose = null;
		Storage gcsStorage = null;

		if (destination.equals("s3")) {
			depositToS3 = true;
			s3 = createS3Client(awsRegion);
		} else if (destination.equals("firehose")) {
			firehose = buildFirehoseClient(awsRegion);
		} else if (destination.equals("gcs")) {
			// The file path specified by GOOGLE_APPLICATION_CREDENTIALS will be used here.
			gcsStorage = StorageOptions.getDefaultInstance().getService();
		} else {
			logger.error("Invalid destination: " + destination);
			System.exit(1);
		}


		while (getRunDepositor()) {
			KafkaConsumer<String, String> stringConsumer = getKafkaConsumer(props);

			logger.debug("Subscribing to topic " + topic);
			stringConsumer.subscribe(Arrays.asList(topic));

			try {
				boolean gotMessages = false;

				while (getRunDepositor()) {
					ConsumerRecords<String, String> records = stringConsumer.poll(Duration.ofMillis(CONSUMER_POLL_TIMEOUT_MS));
					if (records != null && !records.isEmpty()) {
						for (ConsumerRecord<String, String> record : records) {
							try {
								gotMessages = true;
								if (depositToS3) {
									depositToS3(s3, record);
								} else if (destination.equals("firehose")){
									depositToFirehose(firehose, record);
								} else if (destination.equals("gcs")) {
									depositToGCS(gcsStorage, bucketName, record);
								} else {
									logger.error("Invalid destination: " + destination);
									System.exit(1);
								}
							} catch (Exception e) {
								int retryTimeout = 5000;
								String destinationName = depositToS3 ? "S3" : destination;
								logger.error("Error depositing to destination '" + destinationName + "'. Retrying in " + retryTimeout / 1000 + " seconds", e);
								Thread.sleep(retryTimeout);
							}
						}
					} else {
						if (gotMessages) {
							logger.debug("No messages consumed in " + CONSUMER_POLL_TIMEOUT_MS / 1000 + " seconds.");
							gotMessages = false;
						}
					}
				}
			} catch (Exception e) {
				logger.error("Server Error. reconnecting to destination ", e);
			} finally {
				stringConsumer.close();
			}
		}
	}

	static void addConfluentProperties(Properties props) {
		props.put("ssl.endpoint.identification.algorithm", "https");
		props.put("security.protocol", "SASL_SSL");
		props.put("sasl.mechanism", "PLAIN");
  
		String username = getEnvironmentVariable("CONFLUENT_KEY", "");
		String password = getEnvironmentVariable("CONFLUENT_SECRET", "");
  
		if (username != null && password != null) {
		   String auth = "org.apache.kafka.common.security.plain.PlainLoginModule required " +
				   "username=\"" + username + "\" " +
				   "password=\"" + password + "\";";
		   props.put("sasl.jaas.config", auth);
		}
	 }

	void depositToFirehose(AmazonKinesisFirehoseAsync firehose, ConsumerRecord<String, String> record)
			throws InterruptedException, ExecutionException, IOException {
		try {
			// IMPORTANT!!!
			// Append "\n" to separate individual messages in a blob!!!

			String msg = record.value() + "\n";

			ByteBuffer data = convertStringToByteBuffer(msg, Charset.defaultCharset());

			// Check the expiration time for the profile credentials
			LocalDateTime current_datetime = getLocalDateTime();
			LocalDateTime expiration_datetime = getExpirationDateTime();
			System.out.println();
			if (expiration_datetime.isBefore(current_datetime) && API_ENDPOINT.length() > 0) {
				// If credential is expired, generate aws credentials
				JSONObject profile = generateAWSProfile();
				AWS_ACCESS_KEY_ID = profile.get(K_AWS_ACCESS_KEY_ID).toString();
				AWS_SECRET_ACCESS_KEY = profile.get(K_AWS_SECRET_ACCESS_KEY).toString();
				AWS_SESSION_TOKEN = profile.get(K_AWS_SESSION_TOKEN).toString();
				AWS_EXPIRATION = profile.get(K_AWS_EXPIRATION).toString().split("\\+")[0];
			}

			System.setProperty("aws.accessKeyId", AWS_ACCESS_KEY_ID);
			System.setProperty("aws.secretKey", AWS_SECRET_ACCESS_KEY);
			System.setProperty("aws.sessionToken", AWS_SESSION_TOKEN);
			logger.debug("aws.secretKey: {}", AWS_ACCESS_KEY_ID);
			logger.debug("aws.secretKey: {}", AWS_SECRET_ACCESS_KEY);
			logger.debug("aws.secretKey: {}", AWS_SESSION_TOKEN);
			logger.debug("aws.expiration: {}", AWS_EXPIRATION);
			logger.debug("bucketName: {}", bucketName);

			AWSCredentialsProvider credentialsProvider = new SystemPropertiesCredentialsProvider();
			PutRecordRequest putRecordRequest = new PutRecordRequest();
			putRecordRequest.withDeliveryStreamName(bucketName);
			putRecordRequest.setRequestCredentialsProvider(credentialsProvider);

			Record entry = new Record().withData(data);
			putRecordRequest.setRecord(entry);
			logger.debug("Uploading a new record to Firehose: " + record.value());

			// Future<PutRecordResult> result =
			Future<PutRecordResult> prFuture = firehose.putRecordAsync(putRecordRequest);

			// TODO: use result to get response in a separate thread.

			if (waitOpt) {
				PutRecordResult prResult = prFuture.get();
				logger.info(prResult.toString());
			}
		} catch (AmazonClientException ex) {
			logger.error(ex.toString());
			throw ex;
		}
	}

	void depositToS3(AmazonS3 s3, ConsumerRecord<String, String> record) throws IOException {
		try {
			long time = System.currentTimeMillis();
			String timeStamp = Long.toString(time);

			ObjectMetadata objectMetadata = new ObjectMetadata();
			objectMetadata.setSSEAlgorithm(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION);
			PutObjectRequest putRequest = new PutObjectRequest(bucketName, keyName + timeStamp + ".json",
					createSampleFile(record.value()));
			putRequest.setMetadata(objectMetadata);

			/*
			 * Upload an object to your bucket - You can easily upload a file to S3, or
			 * upload directly an InputStream if you know the length of the data in the
			 * stream. You can also specify your own metadata when uploading to S3, which
			 * allows you set a variety of options like content-type and content-encoding,
			 * plus additional metadata specific to your applications.
			 */
			logger.debug("Uploading a new object to S3: " + record.value());
			PutObjectResult result = s3.putObject(putRequest);
			logger.debug(result.toString());
		} catch (AmazonServiceException ase) {
			logger.debug("Caught an AmazonServiceException, which means your request made it "
					+ "to Amazon S3, but was rejected with an error response for some reason.");
			logger.debug("Error Message:    " + ase.getMessage());
			logger.debug("HTTP Status Code: " + ase.getStatusCode());
			logger.debug("AWS Error Code:   " + ase.getErrorCode());
			logger.debug("Error Type:       " + ase.getErrorType());
			logger.debug("Request ID:       " + ase.getRequestId());
			throw ase;
		} catch (AmazonClientException ace) {
			logger.debug("Caught an AmazonClientException, which means the client encountered "
					+ "a serious internal problem while trying to communicate with S3, "
					+ "such as not being able to access the network.");
			logger.debug("Error Message: " + ace.getMessage());
			throw ace;
		}
	}

	void depositToGCS(Storage gcsStorage, String depositBucket, ConsumerRecord<String, String> record) {
		String recordValue = record.value();
		Bucket bucket = gcsStorage.get(depositBucket);
		byte[] bytes = recordValue.getBytes(Charset.defaultCharset());

		long time = System.currentTimeMillis();
		String timeStamp = Long.toString(time);

		Blob blob = bucket.create(timeStamp, bytes);
		if (blob != null) {
			logger.debug("Record successfully uploaded to GCS");
		} else {
			logger.error("Failed to upload record to GCS bucket: " + recordValue);
		}
	}

	AmazonKinesisFirehoseAsync buildFirehoseClient(String awsRegion) {
		// Default is to deposit to Kinesis/Firehose, override via .env
		// variables if S3 deposit desired
		logger.debug("=============================");
		logger.debug("Connecting to Amazon Firehose");
		logger.debug("=============================");

		return AmazonKinesisFirehoseAsyncClientBuilder.standard().withRegion(awsRegion).build();
	}

	AmazonS3 createS3Client(String awsRegion) {
		logger.debug("============== ========");
		logger.debug("Connecting to Amazon S3");
		logger.debug("=======================");
		AWSCredentials credentials = null;
		try {
			credentials = new EnvironmentVariableCredentialsProvider().getCredentials();
		} catch (Exception e) {
			throw new AmazonClientException("Cannot load the credentials from the credential profiles file. "
					+ "Please make sure that your credentials file is at the correct "
					+ "location (~/.aws/credentials), and is in valid format.", e);
		}

		@SuppressWarnings("deprecation")
		AmazonS3 s3 = new AmazonS3Client(credentials);
		Region usEast1 = Region.getRegion(Regions.fromName(awsRegion));
		s3.setRegion(usEast1);

		return s3;
	}

	public ByteBuffer convertStringToByteBuffer(String msg, Charset charset) {
		return ByteBuffer.wrap(msg.getBytes(charset));
	}

	File createSampleFile(String json) throws IOException {
		File file = File.createTempFile("aws-java-sdk-", ".json");
		file.deleteOnExit();

		Writer writer = new OutputStreamWriter(new FileOutputStream(file));
		writer.write(json);
		writer.close();

		return file;
	}

	JSONObject generateAWSProfile() throws IOException {
		CloseableHttpClient client = getHttpClient();
		HttpPost httpPost = new HttpPost(API_ENDPOINT);
		JSONObject jsonResult = new JSONObject();
		String json = "{}";
		StringEntity entity;
		CloseableHttpResponse response = null;
		try {
			entity = new StringEntity(json);
			httpPost.setEntity(entity);
			httpPost.addHeader("Accept", HEADER_Accept);
			httpPost.addHeader("x-api-key", HEADER_X_API_KEY);

			response = client.execute(httpPost);
			String result = EntityUtils.toString(response.getEntity());
			jsonResult = new JSONObject(result);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (response != null) {
				response.close();
			}
			client.close();
		}
		return jsonResult;
	}

	static String getEnvironmentVariable(String variableName, String defaultValue) {
		// get all environment variables
		Map<String, String> env = System.getenv();
		String value = System.getenv(variableName);
		if (value == null || value.equals("")) {
		   System.out.println("Something went wrong retrieving the environment variable " + variableName);
		   System.out.println("Using default value: " + defaultValue);
		   return defaultValue;
		}
		return value;
	 }

	 CloseableHttpClient getHttpClient() {
		return HttpClients.createDefault();
	 }

	 LocalDateTime getLocalDateTime() {
		return LocalDateTime.now();
	 }

	 LocalDateTime getExpirationDateTime() {
		return LocalDateTime.parse(K_AWS_EXPIRATION,
					DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
	 }

	 void depositorSetup() {
		endpoint = getEnvironmentVariable("BOOTSTRAP_SERVER", "");
		topic = getEnvironmentVariable("DEPOSIT_TOPIC", "");
		group = getEnvironmentVariable("DEPOSIT_GROUP", "");
		destination = getEnvironmentVariable("DESTINATION", "firehose");
		if (System.getenv("WAIT") != null && System.getenv("WAIT") != "") 
		{ waitOpt = true; } 
		else 
		{ waitOpt = false; }

		// S3 properties
		bucketName = getEnvironmentVariable("DEPOSIT_BUCKET_NAME", "");
		awsRegion = getEnvironmentVariable("REGION", "us-east-1");
		keyName = getEnvironmentVariable("DEPOSIT_KEY_NAME", "");

		K_AWS_ACCESS_KEY_ID = getEnvironmentVariable("AWS_ACCESS_KEY_ID", "AccessKeyId");
		K_AWS_SECRET_ACCESS_KEY = getEnvironmentVariable("AWS_SECRET_ACCESS_KEY", "SecretAccessKey");
		K_AWS_SESSION_TOKEN = getEnvironmentVariable("AWS_SESSION_TOKEN", "SessionToken");
		K_AWS_EXPIRATION = getEnvironmentVariable("AWS_EXPIRATION", "Expiration");
		API_ENDPOINT = getEnvironmentVariable("API_ENDPOINT", "");
		HEADER_Accept = getEnvironmentVariable("HEADER_ACCEPT", "application/json");
		HEADER_X_API_KEY = getEnvironmentVariable("HEADER_X_API_KEY", "");

		logger.debug("Bucket name: {}", bucketName);
		logger.debug("AWS Region: {}", awsRegion);
		logger.debug("Key name: {}", keyName);
		logger.debug("Kafka topic: {}", topic);
		logger.debug("Destination: {}", destination);
		logger.debug("Wait: {}", waitOpt);
		logger.debug("AWS_ACCESS_KEY_ID: {}", K_AWS_ACCESS_KEY_ID);
		logger.debug("AWS_SECRET_ACCESS_KEY: {}", K_AWS_SECRET_ACCESS_KEY);
		logger.debug("AWS_SESSION_TOKEN: {}", K_AWS_SESSION_TOKEN);
		logger.debug("AWS_EXPIRATION: {}", K_AWS_EXPIRATION);
		logger.debug("API_ENDPOINT: {}", API_ENDPOINT);
		logger.debug("HEADER_Accept: {}", HEADER_Accept);
		logger.debug("HEADER_X_API_KEY: {}", HEADER_X_API_KEY);
	 }

	 boolean getRunDepositor() {
		return runDepositor;
	 }

	 KafkaConsumer<String, String> getKafkaConsumer(Properties props) {
		return new KafkaConsumer<>(props);
	 }
}
