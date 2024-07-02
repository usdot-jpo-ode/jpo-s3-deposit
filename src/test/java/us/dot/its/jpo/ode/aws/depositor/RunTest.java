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
import static org.mockito.Mockito.when;

import mockit.Verifications;

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

        new Verifications() {
            {
                depositor.getKafkaConsumer(any());
                times = 1;
                depositor.getRunDepositor();
                times = 3;
            }
        };

        depositor.run();
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

        // Create a list of ConsumerRecord<String, String>
        List<ConsumerRecord<String, String>> records = new ArrayList<>();
        records.add(new ConsumerRecord<>("topic", 0, 0, "test", "test-value"));

        // Create a TopicPartition object for your topic and partition
        TopicPartition topicPartition = new TopicPartition("topic", 0);

        // Create a map of TopicPartition to List of ConsumerRecord
        Map<TopicPartition, List<ConsumerRecord<String, String>>> recordsMap = new HashMap<>();
        recordsMap.put(topicPartition, records);

        // Initialize ConsumerRecords with the map
        ConsumerRecords<String, String> mockRecords = new ConsumerRecords<>(recordsMap);

        when(mockConsumer.poll(any())).thenReturn(mockRecords);

        new Verifications() {
            {
                depositor.getKafkaConsumer(any());
                times = 1;
                depositor.getRunDepositor();
                times = 3;
                depositor.depositToFirehose(any(), any());
                times = 1;
            }
        };

        depositor.run();
    }
}
