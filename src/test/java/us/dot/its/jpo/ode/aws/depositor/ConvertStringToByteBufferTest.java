package us.dot.its.jpo.ode.aws.depositor;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class ConvertStringToByteBufferTest {
    @Test
    public void testConvertStringToByteBuffer() {
        AwsDepositor awsDepositor = new AwsDepositor();
        String input = "Test";
        ByteBuffer expected = ByteBuffer.wrap(input.getBytes(StandardCharsets.UTF_8));

        ByteBuffer result = awsDepositor.convertStringToByteBuffer(input, StandardCharsets.UTF_8);

        assertEquals(expected, result);
    }
}
