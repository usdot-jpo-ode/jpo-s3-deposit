package us.dot.its.jpo.ode.aws.depositor;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.amazonaws.services.s3.AmazonS3;

public class CreateS3ClientTest {

    @Test
    public void testCreateS3Client() {
        AwsDepositor awsDepositor = new AwsDepositor();
        AmazonS3 s3Client = awsDepositor.createS3Client("us-east-1");
        assertNotNull(s3Client);
    }

    @Test
    public void testCreateS3Client_InvalidCredentials() {
        AwsDepositor awsDepositor = new AwsDepositor();
        assertThrows(IllegalArgumentException.class, () -> {
            awsDepositor.createS3Client("invalid-region");
        });
    }
}
