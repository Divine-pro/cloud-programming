package uk.ac.ed.inf.cw2service.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rabbitmq.client.*;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.ac.ed.inf.cw2service.models.*;

import java.time.Duration;
import java.util.*;

@RestController
@RequestMapping("/api/v1/acp")
public class MessagingController
{

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    @Value("${spring.rabbitmq.host}")
    private String rabbitHost;

    @Value("${spring.rabbitmq.port}")
    private int rabbitPort;

    @Value("${spring.kafka.bootstrap-servers}")
    private String kafkaServers;

    @Value("${student.id}")
    private String sid;

    public MessagingController(StringRedisTemplate redis, ObjectMapper mapper)
    {
        this.redis = redis;
        this.mapper = mapper;
    }

    //Rabbit factory
    private ConnectionFactory rabbitFactory()
    {
        var f = new ConnectionFactory();
        f.setHost(rabbitHost);
        f.setPort(rabbitPort);
        return f;
    }

    //Kafka props
    private Properties producerProps()
    {
        var p = new Properties();
        p.put("bootstrap.servers", kafkaServers);
        p.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        p.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        p.put("acks", "all");
        return p;
    }

    private Properties consumerProps(String grp)
    {
        var p = new Properties();
        p.put("bootstrap.servers", kafkaServers);
        p.put("group.id", grp);
        p.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        p.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        p.put("auto.offset.reset", "earliest");
        p.put("enable.auto.commit", "true");
        p.put("max.poll.records", "500");
        return p;
    }

    //Redis helpers
    private long getLong(String key)
    {
        var v = redis.opsForValue().get(key);
        return v != null ? Long.parseLong(v) : 0L;
    }

    private double getDouble(String key)
    {
        var v = redis.opsForValue().get(key);
        return v != null ? Double.parseDouble(v) : 0.0;
    }

    //PUT messages/rabbitmq/{queueName}/{messageCount}
    @PutMapping("/messages/rabbitmq/{queue}/{cnt}")
    public ResponseEntity<Void> putRabbit(
            @PathVariable String queue,
            @PathVariable int cnt)
    {
        try (var conn = rabbitFactory().newConnection();
             var ch = conn.createChannel())
        {
            ch.queueDeclare(queue, true, false, false, null);
            for (int i = 0; i < cnt; i++)
            {
                var payload = new MessagePayload(sid, i);
                var json = mapper.writeValueAsString(payload);
                ch.basicPublish("", queue, MessageProperties.PERSISTENT_TEXT_PLAIN, json.getBytes());
            }
            return ResponseEntity.ok().build();
        }
        catch (Exception e)
        {
            return ResponseEntity.notFound().build();
        }
    }

    //PUT messages/kafka/{writeTopic}/{messageCount}
    @PutMapping("/messages/kafka/{topic}/{cnt}")
    public ResponseEntity<Void> putKafka(
            @PathVariable String topic,
            @PathVariable int cnt)
    {
        try (var prod = new KafkaProducer<String, String>(producerProps()))
        {
            for (int i = 0; i < cnt; i++)
            {
                var payload = new MessagePayload(sid, i);
                var json = mapper.writeValueAsString(payload);
                prod.send(new ProducerRecord<>(topic, String.valueOf(i), json));
            }
            prod.flush();
            return ResponseEntity.ok().build();
        }
        catch (Exception e)
        {
            return ResponseEntity.notFound().build();
        }
    }

    //GET messages/rabbitmq/{queueName}/{timeoutInMsec}
    @GetMapping("/messages/rabbitmq/{queue}/{timeout}")
    public ResponseEntity<List<String>> getRabbit(
            @PathVariable String queue,
            @PathVariable long timeout)
    {
        var msgs = new ArrayList<String>();
        try (var conn = rabbitFactory().newConnection();
             var ch = conn.createChannel())
        {
            ch.queueDeclare(queue, true, false, false, null);
            var deadline = System.currentTimeMillis() + timeout;
            while (System.currentTimeMillis() < deadline)
            {
                var resp = ch.basicGet(queue, true);
                if (resp != null)
                {
                    msgs.add(new String(resp.getBody()));
                }
                else
                {
                    Thread.sleep(10);
                }
            }
            return ResponseEntity.ok(msgs);
        }
        catch (Exception e)
        {
            return ResponseEntity.ok(msgs);
        }
    }

    //GET messages/kafka/{readTopic}/{timeoutInMsec}
    @GetMapping("/messages/kafka/{topic}/{timeout}")
    public ResponseEntity<List<String>> getKafka(
            @PathVariable String topic,
            @PathVariable long timeout)
    {
        var msgs = new ArrayList<String>();
        var grp = "cw2-" + UUID.randomUUID();
        try (var cons = new KafkaConsumer<String, String>(consumerProps(grp)))
        {
            cons.subscribe(Collections.singletonList(topic));
            var deadline = System.currentTimeMillis() + timeout;
            while (System.currentTimeMillis() < deadline)
            {
                var remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) break;
                ConsumerRecords<String, String> recs = cons.poll(Duration.ofMillis(Math.min(remaining, 100)));
                for (var r : recs) msgs.add(r.value());
            }
            return ResponseEntity.ok(msgs);
        }
        catch (Exception e)
        {
            return ResponseEntity.ok(msgs);
        }
    }

    //GET messages/sorted/rabbitmq/{queueName}/{messagesToConsider}
    @GetMapping("/messages/sorted/rabbitmq/{queue}/{cnt}")
    public ResponseEntity<List<SortedMessage>> getSortedRabbit(
            @PathVariable String queue,
            @PathVariable int cnt)
    {
        var msgs = new ArrayList<SortedMessage>();
        try (var conn = rabbitFactory().newConnection();
             var ch = conn.createChannel())
        {
            ch.queueDeclare(queue, true, false, false, null);
            while (msgs.size() < cnt)
            {
                var resp = ch.basicGet(queue, true);
                if (resp != null)
                {
                    var msg = mapper.readValue(new String(resp.getBody()), SortedMessage.class);
                    msgs.add(msg);
                }
                else
                {
                    Thread.sleep(10);
                }
            }
            msgs.sort(Comparator.comparingInt(SortedMessage::getId));
            return ResponseEntity.ok(msgs);
        }
        catch (Exception e)
        {
            return ResponseEntity.notFound().build();
        }
    }

    //GET messages/sorted/kafka/{topic}/{messagesToConsider}
    @GetMapping("/messages/sorted/kafka/{topic}/{cnt}")
    public ResponseEntity<List<SortedMessage>> getSortedKafka(
            @PathVariable String topic,
            @PathVariable int cnt)
    {
        var msgs = new ArrayList<SortedMessage>();
        var grp = "cw2-sorted-" + UUID.randomUUID();
        try (var cons = new KafkaConsumer<String, String>(consumerProps(grp)))
        {
            cons.subscribe(Collections.singletonList(topic));
            while (msgs.size() < cnt)
            {
                ConsumerRecords<String, String> recs = cons.poll(Duration.ofMillis(500));
                for (var r : recs)
                {
                    if (msgs.size() < cnt)
                    {
                        msgs.add(mapper.readValue(r.value(), SortedMessage.class));
                    }
                }
            }
            msgs.sort(Comparator.comparingInt(SortedMessage::getId));
            return ResponseEntity.ok(msgs);
        }
        catch (Exception e)
        {
            return ResponseEntity.notFound().build();
        }
    }

    //POST splitter
    @PostMapping("/splitter")
    public ResponseEntity<Void> splitter(@RequestBody SplitterRequest req)
    {
        try (var conn = rabbitFactory().newConnection();
             var ch = conn.createChannel();
             var prod = new KafkaProducer<String, String>(producerProps()))
        {

            ch.queueDeclare(req.getReadQueue(), true, false, false, null);

            // Load existing counters - resume across calls
            long cntEven = getLong("count_even");
            long cntOdd  = getLong("count_odd");
            double sumEven = getDouble("sum_even");
            double sumOdd  = getDouble("sum_odd");

            int recv = 0;
            while (recv < req.getMessageCount())
            {
                var resp = ch.basicGet(req.getReadQueue(), true);
                if (resp == null) { Thread.sleep(10); continue;
                }

                var msg = mapper.readValue(new String(resp.getBody()), SplitterMessage.class);
                recv++;
                var json = mapper.writeValueAsString(msg);

                if (msg.getId() % 2 == 0)
                {
                    cntEven++;
                    sumEven += msg.getValue();
                    prod.send(new ProducerRecord<>(req.getWriteTopicEven(), String.valueOf(msg.getId()), json));
                    redis.opsForHash().put(req.getRedisHashEven(), String.valueOf(msg.getId()), json);
                }
                else
                {
                    cntOdd++;
                    sumOdd += msg.getValue();
                    prod.send(new ProducerRecord<>(req.getWriteTopicOdd(), String.valueOf(msg.getId()), json));
                    redis.opsForHash().put(req.getRedisHashOdd(), String.valueOf(msg.getId()), json);
                }
            }
            prod.flush();

            // Persist counters and averages back to Redis
            redis.opsForValue().set("count_even", String.valueOf(cntEven));
            redis.opsForValue().set("count_odd",  String.valueOf(cntOdd));
            redis.opsForValue().set("sum_even",   String.valueOf(sumEven));
            redis.opsForValue().set("sum_odd",    String.valueOf(sumOdd));

            var avgEven = cntEven > 0 ? Math.round((sumEven / cntEven) * 100.0) / 100.0 : 0.0;
            var avgOdd  = cntOdd  > 0 ? Math.round((sumOdd  / cntOdd)  * 100.0) / 100.0 : 0.0;
            redis.opsForValue().set("average_even", String.format("%.2f", avgEven));
            redis.opsForValue().set("average_odd",  String.format("%.2f", avgOdd));

            return ResponseEntity.ok().build();
        }
        catch (Exception e)
        {
            return ResponseEntity.notFound().build();
        }
    }

    //POST transformMessages
    @PostMapping("/transformMessages")
    public ResponseEntity<Void> transformMessages(@RequestBody TransformRequest req)
    {
        try (var conn = rabbitFactory().newConnection();
             var ch = conn.createChannel())
        {

            ch.queueDeclare(req.getReadQueue(),  true, false, false, null);
            ch.queueDeclare(req.getWriteQueue(), true, false, false, null);

            int written = 0, processed = 0, redisUpdates = 0;
            double totalVal = 0.0, totalAdded = 0.0;

            int recv = 0;
            while (recv < req.getMessageCount())
            {
                var resp = ch.basicGet(req.getReadQueue(), true);
                if (resp == null) { Thread.sleep(10); continue;
                }

                var body = new String(resp.getBody());
                recv++;
                processed++;

                JsonNode node = mapper.readTree(body);
                var key = node.get("key").asText();

                if ("TOMBSTONE".equals(key))
                {
                    //Clear all tracked transform keys from Redis
                    var keys = redis.keys("transform:*");
                    if (keys != null && !keys.isEmpty()) redis.delete(keys);
                    written++;

                    //Write stats to outbound queue
                    var stats = mapper.createObjectNode();
                    stats.put("totalMessagesWritten",   written);
                    stats.put("totalMessagesProcessed", processed);
                    stats.put("totalRedisUpdates",      redisUpdates);
                    stats.put("totalValueWritten",      totalVal);
                    stats.put("totalAdded",             totalAdded);

                    ch.basicPublish("", req.getWriteQueue(),
                            MessageProperties.PERSISTENT_TEXT_PLAIN,
                            mapper.writeValueAsString(stats).getBytes());
                    continue;
                }

                int ver = node.get("version").asInt();
                double val = node.get("value").asDouble();

                var storedStr = redis.opsForValue().get("transform:" + key);
                int storedVer = storedStr != null ? Integer.parseInt(storedStr) : -1;

                var out = mapper.createObjectNode();
                out.put("key", key);
                out.put("version", ver);

                double toWrite;
                if (storedVer >= ver)
                {
                    toWrite = val;
                }
                else
                {
                    redis.opsForValue().set("transform:" + key, String.valueOf(ver));
                    redisUpdates++;
                    toWrite = val + 10.5;
                    totalAdded += 10.5;
                }

                out.put("value", toWrite);
                totalVal += toWrite;
                ch.basicPublish("", req.getWriteQueue(),
                        MessageProperties.PERSISTENT_TEXT_PLAIN,
                        mapper.writeValueAsString(out).getBytes());
                written++;
            }

            return ResponseEntity.ok().build();
        }
        catch (Exception e)
        {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/test/seed/kafka/{topic}/{cnt}")
    public ResponseEntity<Void> seedKafka(
            @PathVariable String topic,
            @PathVariable int cnt)
    {
        try (var prod = new KafkaProducer<String, String>(producerProps()))
        {
            for (int i = 1; i <= cnt; i++) {
                // Write in reverse order to test sorting
                int id = cnt - i + 1;
                var json = "{\"Id\":" + id + ",\"Payload\":\"item" + id + "\"}";
                prod.send(new ProducerRecord<>(topic, String.valueOf(id), json));
            }
            prod.flush();
            return ResponseEntity.ok().build();
        }
        catch (Exception e)
        {
            return ResponseEntity.notFound().build();
        }
    }
}
