package us.dot.its.jpo.ode.aws.depositor;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;

public class DepositToGCSTest {
    @Test
    public void testDepositToGCS() {
        Storage gcsStorage = mock(Storage.class);
        Bucket bucket = mock(Bucket.class);
        Blob blob = mock(Blob.class);
        
        ConsumerRecord<String, String> record = mock(ConsumerRecord.class);
        when(record.value()).thenReturn("test");

        when(gcsStorage.get(anyString())).thenReturn(bucket);
        when(bucket.create(anyString(), any(byte[].class))).thenReturn(blob);

        AwsDepositor awsDepositor = new AwsDepositor();

        awsDepositor.depositToGCS(gcsStorage, "depositBucket", record);

        verify(gcsStorage).get("depositBucket");
        verify(bucket).create(anyString(), any(byte[].class));
    }
}
