package consumerexample.app;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.log4j.Logger;

import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose;
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehoseClientBuilder;
import com.amazonaws.services.kinesisfirehose.model.PutRecordRequest;
import com.amazonaws.services.kinesisfirehose.model.PutRecordResult;
import com.amazonaws.services.kinesisfirehose.model.Record;

public class FirehoseWriter {

	private static final Logger log = Logger.getLogger(FirehoseWriter.class);
	
//	public static void main(String[] args) {
//		new FirehoseWriter().run();
//	}
	
	public void run() {		
		
		log.info("Run started.");
		
		String deliveryStreamName = "test-usdot-its-cvpilot-wydot-bsm";
		String dirName = "/tmp/test/sample/multi";
		
		File dir = new File(dirName);
		File[] filesList = dir.listFiles(new FilenameFilter() {
	        public boolean accept(File dir, String name) {
	            return name.toLowerCase().endsWith(".json");
	        }
		});
		
		AmazonKinesisFirehose firehoseClient =  AmazonKinesisFirehoseClientBuilder.defaultClient();

		for (File file : filesList) {
			try {
				// IMPORTANT!!!
				// Append "\n" to separate individual messages in a blob!!!
			   File f = new File(dirName, file.getName());
		      String res = new String(Files.readAllBytes(Paths.get(f.getPath())));
		      
				String msg = res + "\n";
				
				ByteBuffer data = convertStringToByteBuffer(msg, Charset.defaultCharset());
				
				PutRecordRequest putRecordRequest = new PutRecordRequest()
						.withDeliveryStreamName(deliveryStreamName);
				Record record = new Record().withData(data);
				putRecordRequest.setRecord(record);
				PutRecordResult result = firehoseClient.putRecord(putRecordRequest);
				log.info(file.getName() + " : " + result.toString());				
			}
			catch(IOException ex) {
				log.error(ex.toString());
			}
		}		
		
		firehoseClient.shutdown();
		
		log.info("Run complete.");
	}

	// https://stackoverflow.com/questions/1252468/java-converting-string-to-and-from-bytebuffer-and-associated-problems
	public static ByteBuffer convertStringToByteBuffer(String msg, Charset charset){
	    return ByteBuffer.wrap(msg.getBytes(charset));
	}

	public static String convertByteBufferToString(ByteBuffer buffer, Charset charset){
	    byte[] bytes;
	    if(buffer.hasArray()) {
	        bytes = buffer.array();
	    } else {
	        bytes = new byte[buffer.remaining()];
	        buffer.get(bytes);
	    }
	    return new String(bytes, charset);
	}
}