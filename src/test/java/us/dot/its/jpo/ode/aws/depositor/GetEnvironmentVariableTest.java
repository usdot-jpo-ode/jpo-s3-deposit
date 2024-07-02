package us.dot.its.jpo.ode.aws.depositor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class GetEnvironmentVariableTest {
    private final String TEST_VARIABLE = "TEST_VARIABLE";
    private final String TEST_VARIABLE_NO_ENV = "TEST_VARIABLE_NO_ENV";
    private final String TEST_VARIABLE_EMPTY = "TEST_VARIABLE_EMPTY";
    private final String DEFAULT_VALUE = "default";

    @Test
    void testGetEnvironmentVariableExists() throws Exception {
        String expectedValue = "testValue";

        // Test
        String result = AwsDepositor.getEnvironmentVariable(TEST_VARIABLE, "");
        assertEquals(expectedValue, result);
    }

    @Test
    void testGetEnvironmentVariableNotSet() {
        // Test when the environment variable is not set
        String result = AwsDepositor.getEnvironmentVariable(TEST_VARIABLE_NO_ENV, DEFAULT_VALUE);
        assertEquals(DEFAULT_VALUE, result);
    }

    @Test
    void testGetEnvironmentVariableEmpty() {
        // Test
        String result = AwsDepositor.getEnvironmentVariable(TEST_VARIABLE_EMPTY, DEFAULT_VALUE);
        assertEquals(DEFAULT_VALUE, result);
    }
}