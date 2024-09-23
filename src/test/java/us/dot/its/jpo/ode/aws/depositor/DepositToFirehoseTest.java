package us.dot.its.jpo.ode.aws.depositor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.json.JSONObject;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehoseAsync;
import com.amazonaws.services.kinesisfirehose.model.PutRecordRequest;

public class DepositToFirehoseTest {
    
    @Test
    public void testDepositToFirehose() throws InterruptedException, ExecutionException, IOException {
        
        // Create a mock AmazonKinesisFirehoseAsync instance
        AmazonKinesisFirehoseAsync firehose = mock(AmazonKinesisFirehoseAsync.class);

        // Create a mock ConsumerRecord
        ConsumerRecord<String, String> mockRecord = mock(ConsumerRecord.class);
        when(mockRecord.value()).thenReturn("Test Record");

        AwsDepositor depositor = spy(new AwsDepositor());
        doReturn(LocalDateTime.of(2024, 6, 26, 12, 0, 0)).when(depositor).getLocalDateTime();
        doReturn(LocalDateTime.of(2024, 6, 26, 10, 0, 0)).when(depositor).getExpirationDateTime();

        JSONObject generateAwsReturnVal = new JSONObject();
        generateAwsReturnVal.put("testAccessKey", "test-access-key-id");
        generateAwsReturnVal.put("testSecretKey", "test-secret-key");
        generateAwsReturnVal.put("testSessionToken", "test-token");
        generateAwsReturnVal.put("2020-01-01 00:00:00", "test-expiration");

        doReturn(generateAwsReturnVal).when(depositor).generateAWSProfile();

        // pull in necessary environment variables
        depositor.depositorSetup();

        // Call the depositToFirehose method
        depositor.depositToFirehose(firehose, mockRecord);

        // Verify that the putRecordAsync method was called on the mock AmazonKinesisFirehoseAsync instance
        ArgumentCaptor<PutRecordRequest> putRecordRequestCaptor = ArgumentCaptor.forClass(PutRecordRequest.class);
        verify(firehose).putRecordAsync(putRecordRequestCaptor.capture());

        // Assert PutRecordRequest value is as expected
        PutRecordRequest putRecordRequestResult = putRecordRequestCaptor.getValue();
        assertEquals("Test Record\n", convertByteBufferToString(putRecordRequestResult.getRecord().getData()));
    }

    @Test
    public void testGetExpirationDateTime() {
        AwsDepositor depositor = new AwsDepositor();
        depositor.depositorSetup();
        LocalDateTime result = depositor.getExpirationDateTime();
        assertEquals(LocalDateTime.of(2020, 01, 01, 0, 0, 0), result);
    }

    private String convertByteBufferToString(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return new String(bytes, Charset.defaultCharset());
    }
}
