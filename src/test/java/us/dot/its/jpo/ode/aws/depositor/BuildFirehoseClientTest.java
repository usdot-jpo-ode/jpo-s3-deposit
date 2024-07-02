package us.dot.its.jpo.ode.aws.depositor;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehoseAsync;

public class BuildFirehoseClientTest {
    @Test
    public void testBuildFirehoseClient() {
        AwsDepositor awsDepositor = new AwsDepositor();
        String awsRegion = "us-east-1";

        AmazonKinesisFirehoseAsync firehose = awsDepositor.buildFirehoseClient(awsRegion);

        assertNotNull(firehose);
    }
}
