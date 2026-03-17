package uk.ac.ed.inf.cw2service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import uk.ac.ed.inf.cw2service.model.AcpMessage;

import java.time.Duration;
import java.util.*;

@Service
public class MessagingService
{

    private final RabbitTemplate rabbitTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String studentId = "s2845427";

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    public MessagingService(RabbitTemplate rabbitTemplate, KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper)
    {
        this.rabbitTemplate = rabbitTemplate;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    //RABBITMQ METHODS

    public void writeToRabbit(String queueName, int messageCount) throws JsonProcessingException
    {
        for (int i = 0; i < messageCount; i++)
        {
            AcpMessage msg = new AcpMessage(studentId, i);
            String json = objectMapper.writeValueAsString(msg);
            rabbitTemplate.convertAndSend(queueName, json);
        }
    }

    public List<String> readFromRabbit(String queueName, int timeoutInMsec)
    {
        List<String> messages = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        // Loop strictly until the timeout is reached
        while (System.currentTimeMillis() - startTime < timeoutInMsec)
        {
            Message msg = rabbitTemplate.receive(queueName, 10);
            if (msg != null)
            {
                messages.add(new String(msg.getBody()));
            }
        }
        return messages;
    }

    //KAFKA METHODS

    public void writeToKafka(String topicName, int messageCount) throws JsonProcessingException
    {
        for (int i = 0; i < messageCount; i++)
        {
            AcpMessage msg = new AcpMessage(studentId, i);
            String json = objectMapper.writeValueAsString(msg);
            kafkaTemplate.send(topicName, json);
        }
    }

    public List<String> readFromKafka(String topicName, int timeoutInMsec)
    {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "acp-group-" + UUID.randomUUID());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        List<String> messages = new ArrayList<>();
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props))
        {
            consumer.subscribe(Collections.singletonList(topicName));
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(timeoutInMsec));
            records.forEach(record -> messages.add(record.value()));
        }
        return messages;
    }
}
