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

        // Test when the environment variable is set
        String result = AwsDepositor.getEnvironmentVariable(TEST_VARIABLE, "");
        assertEquals(expectedValue, result);
    }

    @Test
    void testGetEnvironmentVariableNotSetOrEmpty() {
        // Test when the environment variable is not set
        String notSetResult = AwsDepositor.getEnvironmentVariable(TEST_VARIABLE_NO_ENV, DEFAULT_VALUE);
        assertEquals(DEFAULT_VALUE, notSetResult);

        // Test when the environment variable is empty
        String emptyResult = AwsDepositor.getEnvironmentVariable(TEST_VARIABLE_EMPTY, DEFAULT_VALUE);
        assertEquals(DEFAULT_VALUE, emptyResult);
    }
}