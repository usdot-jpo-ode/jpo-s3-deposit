package us.dot.its.jpo.ode.aws.depositor;

import java.io.IOException;

import org.apache.http.HttpVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicStatusLine;
import org.json.JSONObject;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import mockit.Verifications;

public class GenerateAWSProfileTest {
    @Test
    void testGenerateAWSProfileSuccess() throws Exception {

    AwsDepositor depositor = spy(new AwsDepositor());

    // Mock the CloseableHttpResponse
    CloseableHttpResponse mockResponse = mock(CloseableHttpResponse.class);
    when(mockResponse.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, 200, "OK"));
    when(mockResponse.getEntity()).thenReturn(new StringEntity("{\"key\":\"value\"}"));
    
    // Mock the CloseableHttpClient
    CloseableHttpClient mockClient = mock(CloseableHttpClient.class);
    when(mockClient.execute(any())).thenReturn(mockResponse);

    doReturn(mockClient).when(depositor).getHttpClient();

    depositor.depositorSetup();
    JSONObject result = depositor.generateAWSProfile();

    assertNotNull(result);
    assertEquals("value", result.getString("key"));

    // Verify interactions
    new Verifications() {{
        mockClient.execute((HttpPost) any);
        times = 1;

        mockResponse.close();
        times = 1;

        mockClient.close();
        times = 1;
    }};
    }

    @Test
    void testGenerateAWSProfileException() throws IOException {
        AwsDepositor depositor = spy(new AwsDepositor());

        // Mock the CloseableHttpResponse
        CloseableHttpResponse mockResponse = mock(CloseableHttpResponse.class);
        when(mockResponse.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, 200, "OK"));
        when(mockResponse.getEntity()).thenReturn(null);
        
        // Mock the CloseableHttpClient
        CloseableHttpClient mockClient = mock(CloseableHttpClient.class);
        when(mockClient.execute(any())).thenReturn(mockResponse);
    
        doReturn(mockClient).when(depositor).getHttpClient();
        Exception exception = assertThrows(Exception.class, depositor::generateAWSProfile);

        // Verify the exception
        assertNotNull(exception);

        // Verify interactions
        new Verifications() {{
            mockClient.execute((HttpPost) any);
            times = 1;

            mockClient.close();
            times = 1;
        }};
    }
}
