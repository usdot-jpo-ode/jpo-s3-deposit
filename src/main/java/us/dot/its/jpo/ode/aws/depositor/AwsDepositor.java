package us.dot.its.jpo.ode.aws.depositor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
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

public class AwsDepositor {
   private static Logger logger = LoggerFactory.getLogger(AwsDepositor.class);
   private static final long CONSUMER_POLL_TIMEOUT_MS = 60000;

   private static String endpoint;
   private static String topic;
   private static String group;
   private static String destination;
   private static String bucketName;
   private static String awsRegion;
   private static String keyName;
   private static boolean waitOpt;

   public static void main(String[] args) throws Exception {

      CommandLine cmd = commandLineOptions(args);

      endpoint = cmd.getOptionValue("bootstrap-server");
      topic = cmd.getOptionValue("topic");
      group = cmd.getOptionValue("group");
      destination = cmd.getOptionValue("destination", "firehose");
      waitOpt = cmd.hasOption("w");

      // S3 properties
      bucketName = cmd.getOptionValue("bucket-name");
      awsRegion = cmd.getOptionValue("region", "us-east-1");
      keyName = cmd.getOptionValue("key-name");

      logger.debug("Bucket name: {}", bucketName);
      logger.debug("AWS Region: {}", awsRegion);
      logger.debug("Key name: {}", keyName);
      logger.debug("Kafka topic: {}", topic);
      logger.debug("Destination: {}", destination);
      logger.debug("Wait: {}", waitOpt);

      // Properties for the kafka topic
      Properties props = new Properties();
      props.put("bootstrap.servers", endpoint);
      props.put("group.id", group);
      props.put("enable.auto.commit", "false");
      props.put("auto.commit.interval.ms", "1000");
      props.put("session.timeout.ms", "30000");
      props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
      props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

      boolean depositToS3 = false;
      AmazonS3 s3 = null;
      AmazonKinesisFirehoseAsync firehose = null;
      if (destination != null && destination.equals("s3")) {
         depositToS3 = true;
         s3 = createS3Client(awsRegion);

      } else {
         firehose = buildFirehoseClient(awsRegion);
      }
      
      while (true) {
         KafkaConsumer<String, String> stringConsumer = new KafkaConsumer<String, String>(props);

         logger.debug("Subscribing to topic " + topic);
         stringConsumer.subscribe(Arrays.asList(topic));

         try {
            boolean gotMessages = false;
            
            while (true) {
               ConsumerRecords<String, String> records = stringConsumer.poll(CONSUMER_POLL_TIMEOUT_MS);
               if (records != null && !records.isEmpty()) {
                  for (ConsumerRecord<String, String> record : records) {
                     try {
                        gotMessages = true;
                           if (depositToS3) {
                              depositToS3(s3, record);
                           } else {
                              depositToFirehose(firehose, record);
                           }
                     } catch (Exception e) {
                        int retryTimeout = 5000;
                        logger.error("Error depositing to AWS. Retrying in " + retryTimeout/1000 + " seconds", e);
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
            logger.error("Sever Error. reconnecting to AWS ", e);
         } finally {
            stringConsumer.close();
         }
      }
   }

   private static void depositToFirehose(AmazonKinesisFirehoseAsync firehose, ConsumerRecord<String, String> record) throws InterruptedException, ExecutionException {
      try {
         // IMPORTANT!!!
         // Append "\n" to separate individual messages in a blob!!!
      
         String msg = record.value() + "\n";
      
         ByteBuffer data = convertStringToByteBuffer(msg, Charset.defaultCharset());
      
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

   private static void depositToS3(AmazonS3 s3, ConsumerRecord<String, String> record) throws IOException {
      try {
         long time = System.currentTimeMillis();
         String timeStamp = Long.toString(time);
          
         ObjectMetadata objectMetadata = new ObjectMetadata();
         objectMetadata.setSSEAlgorithm(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION);
         PutObjectRequest putRequest = new PutObjectRequest(bucketName, keyName + timeStamp + ".json",
               createSampleFile(record.value()));
         putRequest.setMetadata(objectMetadata);
      
         /*
          * Upload an object to your bucket - You can easily upload a
          * file to S3, or upload directly an InputStream if you know
          * the length of the data in the stream. You can also specify
          * your own metadata when uploading to S3, which allows you
          * set a variety of options like content-type and
          * content-encoding, plus additional metadata specific to your
          * applications.
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

   private static CommandLine commandLineOptions(String[] args) throws ParseException {
      // Option parsing
      Options options = new Options();

      Option destination_option = new Option("d", "destination", true,
            "Optional, destination defaults to Firehose. Enter \"s3\" to override");
      destination_option.setRequired(false);
      destination_option.setOptionalArg(true);
      options.addOption(destination_option);

      Option bucket_name_option = new Option("b", "bucket-name", true, "Bucket Name");
      bucket_name_option.setRequired(true);
      options.addOption(bucket_name_option);

      Option region_option = new Option("r", "region", true, "AWS Region");
      region_option.setRequired(false);
      region_option.setOptionalArg(true);
      options.addOption(region_option);

      Option key_name_option = new Option("k", "key-name", true, "Key Name");
      key_name_option.setRequired(true);
      options.addOption(key_name_option);

      Option bootstrap_server = new Option("s", "bootstrap-server", true, "Endpoint ('ip:port')");
      bootstrap_server.setRequired(true);
      options.addOption(bootstrap_server);

      Option topic_option = new Option("t", "topic", true, "Topic Name");
      topic_option.setRequired(true);
      options.addOption(topic_option);

      Option group_option = new Option("g", "group", true, "Consumer Group");
      group_option.setRequired(true);
      options.addOption(group_option);

      Option wait_option = new Option("w", "wait", false, "Wait for AWS deposit results");
      wait_option.setRequired(false);
      options.addOption(wait_option);

      CommandLineParser parser = new DefaultParser();
      HelpFormatter formatter = new HelpFormatter();
      CommandLine cmd;

      try {
         cmd = parser.parse(options, args);
      } catch (ParseException e) {
         logger.debug(e.getMessage());
         formatter.printHelp("JPO Firehose and S3 Depositor", options);
         throw e;
         //System.exit(1);
      }
      return cmd;
   }

   private static AmazonKinesisFirehoseAsync buildFirehoseClient(String awsRegion) {
      // Default is to deposit to Kinesis/Firehose, override via .env
      // variables if S3 deposit desired
      logger.debug("=============================");
      logger.debug("Connecting to Amazon Firehose");
      logger.debug("=============================");
      
      return AmazonKinesisFirehoseAsyncClientBuilder.standard().withRegion(awsRegion).build();
   }

   private static AmazonS3 createS3Client(String awsRegion) {
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

   public static ByteBuffer convertStringToByteBuffer(String msg, Charset charset) {
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
