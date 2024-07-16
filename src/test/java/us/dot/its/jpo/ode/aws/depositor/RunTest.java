package us.dot.its.jpo.ode.aws.depositor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RunTest {
    @Test
    public void testRunNoRecords() throws Exception {
        AwsDepositor depositor = spy(new AwsDepositor());

        KafkaConsumer<String, String> mockConsumer = mock(KafkaConsumer.class);
        when(mockConsumer.poll(any())).thenReturn(null);

        doReturn(mockConsumer).when(depositor).getKafkaConsumer(any());
        doReturn(true, true, false).when(depositor).getRunDepositor();

        JSONObject generateAwsReturnVal = new JSONObject();
        generateAwsReturnVal.put("testAccessKey", "test-access-key-id");
        generateAwsReturnVal.put("testSecretKey", "test-secret-key");
        generateAwsReturnVal.put("testSessionToken", "test-token");
        generateAwsReturnVal.put("2020-01-01 00:00:00", "test-expiration");

        doReturn(generateAwsReturnVal).when(depositor).generateAWSProfile();

        depositor.run();

        verify(depositor, times(1)).getKafkaConsumer(any());
        verify(depositor, times(4)).getRunDepositor();
    }

    @Test
    public void testRunRecords() throws Exception {
        AwsDepositor depositor = spy(new AwsDepositor());

        KafkaConsumer<String, String> mockConsumer = mock(KafkaConsumer.class);
        when(mockConsumer.poll(any())).thenReturn(null);

        doReturn(mockConsumer).when(depositor).getKafkaConsumer(any());
        doReturn(true, true, false).when(depositor).getRunDepositor();

        JSONObject generateAwsReturnVal = new JSONObject();
        generateAwsReturnVal.put("testAccessKey", "test-access-key-id");
        generateAwsReturnVal.put("testSecretKey", "test-secret-key");
        generateAwsReturnVal.put("testSessionToken", "test-token");
        generateAwsReturnVal.put("2020-01-01 00:00:00", "test-expiration");

        doReturn(generateAwsReturnVal).when(depositor).generateAWSProfile();

        doNothing().when(depositor).depositToFirehose(any(), any());

        List<ConsumerRecord<String, String>> records = new ArrayList<>();
        records.add(new ConsumerRecord<>("topic", 0, 0, "test", "test-value"));
        
        TopicPartition topicPartition = new TopicPartition("topic", 0);

        Map<TopicPartition, List<ConsumerRecord<String, String>>> recordsMap = new HashMap<>();
        recordsMap.put(topicPartition, records);

        ConsumerRecords<String, String> mockRecords = new ConsumerRecords<>(recordsMap);

        when(mockConsumer.poll(any())).thenReturn(mockRecords);

        depositor.run();

        verify(depositor, times(1)).getKafkaConsumer(any());
        verify(depositor, times(4)).getRunDepositor();
        verify(depositor, times(1)).depositToFirehose(any(), any());
    }
}
