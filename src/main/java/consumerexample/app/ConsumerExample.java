package consumerexample.app;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose;
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehoseClientBuilder;
import com.amazonaws.services.kinesisfirehose.model.PutRecordRequest;
import com.amazonaws.services.kinesisfirehose.model.PutRecordResult;
import com.amazonaws.services.kinesisfirehose.model.Record;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.apache.commons.cli.*;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.log4j.Logger;
import us.dot.its.jpo.ode.plugin.j2735.J2735Bsm;
import us.dot.its.jpo.ode.util.SerializationUtils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Properties;
public class ConsumerExample {
	private static final Logger log = Logger.getLogger(ConsumerRecord.class);


	public static void main( String[] args )  throws Exception{

		// Option parsing
		Options options = new Options();
		
		Option destination_option = new Option("d", "destination", true, "Optional, destination defaults to Firehose. Enter \"s3\" to override");
		destination_option.setRequired(false);
		options.addOption(destination_option);
		
		Option bucket_name_option = new Option("s", "bucket-name", true, "Bucket Name");
		bucket_name_option.setRequired(true);
		options.addOption(bucket_name_option);
		
		Option key_name_option = new Option("k", "key-name", true, "Key Name");
		key_name_option.setRequired(true);
		options.addOption(key_name_option);
		
		Option bootstrap_server = new Option("b", "bootstrap-server", true, "Endpoint ('ip:port')");
		bootstrap_server.setRequired(true);
		options.addOption(bootstrap_server);

		Option topic_option = new Option("t", "topic", true, "Topic Name");
		topic_option.setRequired(true);
		options.addOption(topic_option);

		Option group_option = new Option("g", "group", true, "Consumer Group");
		group_option.setRequired(true);
		options.addOption(group_option);

		Option type_option = new Option("type", "type", true, "string|byte message type");
		type_option.setRequired(true);
		options.addOption(type_option);

		CommandLineParser parser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		CommandLine cmd;

		 try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			System.out.println(e.getMessage());
			formatter.printHelp("Consumer Example", options);

			System.exit(1);
			return;
		}

		String endpoint = cmd.getOptionValue("bootstrap-server");
		String topic = cmd.getOptionValue("topic");
		String group = cmd.getOptionValue("group");
		String type = cmd.getOptionValue("type");
		String destination = cmd.getOptionValue("destination");

		//S3 properties
		String bucketName = cmd.getOptionValue("bucket-name");
		String keyName = cmd.getOptionValue("key-name");
		
		System.out.printf("DEBUG - Bucket name: %s\n", bucketName);
		System.out.printf("DEBUG - Key name: %s\n", keyName);
		System.out.printf("DEBUG - Kafka topic: %s\n", topic);


		// Properties for the kafka topic
		Properties props = new Properties();
		props.put("bootstrap.servers", endpoint);
		props.put("group.id", group);
		props.put("enable.auto.commit", "false");
		props.put("auto.commit.interval.ms", "1000");
		props.put("session.timeout.ms", "30000");
		props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		if (type.equals("byte")){ 
			props.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
		} else {
			props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		}

		if (type.equals("byte")) {
			KafkaConsumer<String, byte[]> byteArrayConsumer = new KafkaConsumer<String, byte[]>(props);

			byteArrayConsumer.subscribe(Arrays.asList(topic));
			System.out.println("Subscribed to topic " + topic);
			while (true) {
				ConsumerRecords<String, byte[]> records = byteArrayConsumer.poll(100);
				for (ConsumerRecord<String, byte[]> record : records) {
					// Serialize the record value
					SerializationUtils<J2735Bsm> serializer = new SerializationUtils<J2735Bsm>();
					J2735Bsm bsm =  serializer.deserialize(record.value());
					System.out.print(bsm.toString()); 
				}
			}
		} else {
			KafkaConsumer<String, String> stringConsumer = new KafkaConsumer<String, String>(props);

			stringConsumer.subscribe(Arrays.asList(topic));
			System.out.println("Subscribed to topic " + topic);


			while (true) {		
				ConsumerRecords<String, String> records = stringConsumer.poll(100);
				for (ConsumerRecord<String, String> record : records) {
					System.out.print(record.value());
					AWSCredentials credentials = null;

					;
					try {
						credentials = new EnvironmentVariableCredentialsProvider().getCredentials();
					} catch (Exception e) {
						throw new AmazonClientException(
								"Cannot load the credentials from the credential profiles file. " +
										"Please make sure that your credentials file is at the correct " +
										"location (~/.aws/credentials), and is in valid format.",
								e);
					}

					AmazonS3 s3 = new AmazonS3Client(credentials);
					Region usEast1 = Region.getRegion(Regions.US_EAST_1);
					s3.setRegion(usEast1);

					AmazonKinesisFirehose firehoseClient =  AmazonKinesisFirehoseClientBuilder.standard().withRegion("us-east-1").build();

					long time = System.currentTimeMillis();
					String timeStamp = Long.toString(time);

					 if (destination.equals("s3")) {
						System.out.println("===========================================");
						System.out.println("Getting Started with Amazon S3");
						System.out.println("===========================================\n");

						try {
							ObjectMetadata objectMetadata = new ObjectMetadata();
							objectMetadata.setSSEAlgorithm(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION);
							PutObjectRequest putRequest = new PutObjectRequest(bucketName, keyName + timeStamp + ".json", createSampleFile(record.value()));
							putRequest.setMetadata(objectMetadata);

						/*
						 * Upload an object to your bucket - You can easily upload a file to
						 * S3, or upload directly an InputStream if you know the length of
						 * the data in the stream. You can also specify your own metadata
						 * when uploading to S3, which allows you set a variety of options
						 * like content-type and content-encoding, plus additional metadata
						 * specific to your applications.
						 */
							System.out.println("Uploading a new object to S3 from a file\n");
							s3.putObject(putRequest);


						} catch (AmazonServiceException ase) {
							System.out.println("Caught an AmazonServiceException, which means your request made it "
									+ "to Amazon S3, but was rejected with an error response for some reason.");
							System.out.println("Error Message:    " + ase.getMessage());
							System.out.println("HTTP Status Code: " + ase.getStatusCode());
							System.out.println("AWS Error Code:   " + ase.getErrorCode());
							System.out.println("Error Type:       " + ase.getErrorType());
							System.out.println("Request ID:       " + ase.getRequestId());
						} catch (AmazonClientException ace) {
							System.out.println("Caught an AmazonClientException, which means the client encountered "
									+ "a serious internal problem while trying to communicate with S3, "
									+ "such as not being able to access the network.");
							System.out.println("Error Message: " + ace.getMessage());
						}
					} else {
                  // Default is to deposit to Kinesis/Firehose, override via .env variables if S3 deposit desired
                  System.out.println("===========================================");
                  System.out.println("Getting Started with Amazon Firehose");
                  System.out.println("===========================================\n");
                  try {
                     // IMPORTANT!!!
                     // Append "\n" to separate individual messages in a blob!!!

                     String msg = record.value() + "\n";

                     ByteBuffer data = convertStringToByteBuffer(msg, Charset.defaultCharset());

                     PutRecordRequest putRecordRequest = new PutRecordRequest()
                           .withDeliveryStreamName(bucketName);
                     Record entry = new Record().withData(data);
                     putRecordRequest.setRecord(entry);
                     PutRecordResult result = firehoseClient.putRecord(putRecordRequest);
                     log.info(result.toString());
                  } catch (AmazonClientException ex) {
                     log.error(ex.toString());
                  }
               }
				}
			}
		
		}
	}

	public static ByteBuffer convertStringToByteBuffer(String msg, Charset charset){
		return ByteBuffer.wrap(msg.getBytes(charset));
	}
	private static File createSampleFile(String json) throws IOException {
		File file = File.createTempFile("aws-java-sdk-", ".json");
		file.deleteOnExit();

		Writer writer = new OutputStreamWriter(new FileOutputStream(file));
		writer.write(json);
		writer.close();

		return file;
	}


}
