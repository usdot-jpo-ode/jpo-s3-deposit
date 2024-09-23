package us.dot.its.jpo.ode.aws.depositor;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class CreateSampleFileTest {
    @Test
    public void testCreateSampleFile() throws IOException {
        AwsDepositor awsDepositor = new AwsDepositor();
        String json = "{\"key\": \"value\"}";
        File file = awsDepositor.createSampleFile(json);
        assertNotNull(file);
        assertTrue(file.exists());
        assertTrue(file.isFile());
        assertEquals(".json", file.getName().substring(file.getName().lastIndexOf(".")));
        file.delete();
    }
}
