package us.dot.its.jpo.ode.aws.depositor;

import java.io.IOException;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import static org.junit.Assert.assertNotNull;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;


public class DepositToS3Test {
    @Test
    public void testDepositToS3() throws IOException {
        // Mock necessary classes
        AmazonS3 s3 = mock(AmazonS3.class);
        ConsumerRecord<String, String> mockRecord = mock(ConsumerRecord.class);
        
        PutObjectResult result = new PutObjectResult();
        when(mockRecord.value()).thenReturn("Test Record");
        when(s3.putObject(any())).thenReturn(result);

        AwsDepositor awsDepositor = new AwsDepositor();
        awsDepositor.depositToS3(s3, mockRecord);

        // Verify that the putObject method was called on the mock AmazonS3 instance
        ArgumentCaptor<PutObjectRequest> putObjectRequestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3).putObject(putObjectRequestCaptor.capture());

        // Assert that the putObjectRequest was created correctly
        PutObjectRequest putObjectRequestResult = putObjectRequestCaptor.getValue();
        assertNotNull(putObjectRequestResult);
    }
}
