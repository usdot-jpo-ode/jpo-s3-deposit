package us.dot.its.jpo.ode.aws.depositor;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class AddConfluentPropertiesTest {
    @Test
    public void testAddConfluentProperties() {
        Properties props = new Properties();
        AwsDepositor.addConfluentProperties(props);

        assertEquals("https", props.getProperty("ssl.endpoint.identification.algorithm"));
        assertEquals("SASL_SSL", props.getProperty("security.protocol"));
        assertEquals("PLAIN", props.getProperty("sasl.mechanism"));
        assertEquals("org.apache.kafka.common.security.plain.PlainLoginModule required username=\"testConfluentKey\" password=\"testConfluentSecret\";" , props.getProperty("sasl.jaas.config"));
    }
}
