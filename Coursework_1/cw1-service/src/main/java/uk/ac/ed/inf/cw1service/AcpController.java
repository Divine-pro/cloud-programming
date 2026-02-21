package uk.ac.ed.inf.cw1service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Object;
import uk.ac.ed.inf.cw1service.models.Drone;
import uk.ac.ed.inf.cw1service.models.SystemLog;
import uk.ac.ed.inf.cw1service.models.UrlRequest;
import uk.ac.ed.inf.cw1service.repositories.DroneRepository;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/acp")
public class AcpController
{

    @Autowired private RestTemplate restTemplate;
    @Autowired private DroneRepository droneRepository;
    @Autowired private S3Client s3Client;
    @Autowired private DynamoDbEnhancedClient dynamoDbClient;
    @Autowired private ObjectMapper objectMapper;

    private final String BUCKET_NAME = "drone-data-archive";

    //Postgres Read
    @GetMapping("/drones")
    public ResponseEntity<List<Drone>> getAllDrones()
    {
        return ResponseEntity.ok(droneRepository.findAll());
    }

    //The main pipeline
    @PostMapping("/process/dump")
    public ResponseEntity<List<Drone>> processDump(@RequestBody UrlRequest request)
    {
        logToDynamo("PROCESS_START", "Processing: " + request.getUrlPath());

        // Fetch from Azure
        Drone[] response = restTemplate.getForObject(request.getUrlPath(), Drone[].class);
        if (response == null) return ResponseEntity.notFound().build();

        // Archive to S3
        archiveToS3(response);

        // Process Costs & Save to Postgres
        List<Drone> droneList = Arrays.asList(response);
        for (Drone d : droneList)
        {
            if (d.getUrl() == null || d.getUrl().isEmpty())
            {
                d.setUrl(UUID.randomUUID().toString());
            }

            var cap = d.getCapability();
            if (cap != null)
            {
                double cost = cap.getIc() + cap.getFc() + (cap.getCpm() * 100);
                d.setCost100(Math.round(cost * 100.0) / 100.0);
            }
        }
        droneRepository.saveAll(droneList);

        // Log Success
        logToDynamo("SUCCESS", "Processed " + droneList.size() + " records");
        return ResponseEntity.ok(droneList);
    }

    //S3 Readers
    @GetMapping("/s3/list")
    public ResponseEntity<List<String>> listS3Objects()
    {
        try
        {
            List<String> files = s3Client.listObjectsV2(b -> b.bucket(BUCKET_NAME)).contents()
                    .stream().map(S3Object::key).collect(Collectors.toList());
            return ResponseEntity.ok(files);
        }
        catch (Exception e)
        {
            return ResponseEntity.ok(List.of("Bucket empty or missing"));
        }
    }

    @GetMapping("/s3/read/{key}")
    public ResponseEntity<String> readS3Object(@PathVariable String key)
    {
        try
        {
            ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(b -> b.bucket(BUCKET_NAME).key(key));
            return ResponseEntity.ok(new String(s3Object.readAllBytes(), StandardCharsets.UTF_8));
        }
        catch (Exception e)
        {
            return ResponseEntity.notFound().build();
        }
    }

    //DynamoDB Readers
    @GetMapping("/dynamo/all")
    public ResponseEntity<List<SystemLog>> getAllLogs()
    {
        try
        {
            List<SystemLog> logs = new ArrayList<>();
            getLogTable().scan().items().forEach(logs::add);
            return ResponseEntity.ok(logs);
        }
        catch (Exception e)
        {
            return ResponseEntity.ok(List.of());
        }
    }

    @GetMapping("/dynamo/{id}")
    public ResponseEntity<SystemLog> getLogById(@PathVariable String id)
    {
        try
        {
            SystemLog log = getLogTable().getItem(Key.builder().partitionValue(id).build());
            return log != null ? ResponseEntity.ok(log) : ResponseEntity.notFound().build();
        }
        catch (Exception e)
        {
            return ResponseEntity.notFound().build();
        }
    }

    //Copying the content to S3 and DynamoDB

    @PostMapping("/s3/dump")
    public ResponseEntity<String> copyToS3()
    {
        try
        {
            List<Drone> allDrones = droneRepository.findAll();
            if (allDrones.isEmpty()) return ResponseEntity.badRequest().body("No drones in Postgres to copy!");

            for (Drone drone : allDrones)
            {
                String fileName = "drone-record-" + drone.getUrl() + ".json";
                s3Client.putObject(b -> b.bucket(BUCKET_NAME).key(fileName),
                        software.amazon.awssdk.core.sync.RequestBody.fromString(objectMapper.writeValueAsString(drone)));
            }
            logToDynamo("S3_SYNC", "Copied " + allDrones.size() + " individual drones to S3");
            return ResponseEntity.ok("Successfully copied " + allDrones.size() + " drones to S3");
        }
        catch (Exception e)
        {
            return ResponseEntity.internalServerError().body("S3 Copying Failed: " + e.getMessage());
        }
    }

    @PostMapping("/dynamo/dump")
    public ResponseEntity<String> copyToDynamo()
    {
        try
        {
            List<Drone> allDrones = droneRepository.findAll();
            if (allDrones.isEmpty()) return ResponseEntity.badRequest().body("No drones in Postgres to copy!");

            for (Drone drone : allDrones)
            {
                logToDynamo("DYNAMO_SYNC_DRONE", objectMapper.writeValueAsString(drone));
            }
            return ResponseEntity.ok("Successfully copied " + allDrones.size() + " drones to DynamoDB");
        }
        catch (Exception e)
        {
            return ResponseEntity.internalServerError().body("DynamoDB Copying Failed: " + e.getMessage());
        }
    }

    //HELPERS
    private void archiveToS3(Drone[] drones)
    {
        try
        {
            try
            {
                s3Client.createBucket(b -> b.bucket(BUCKET_NAME));
            }
            catch (Exception e) {}
            s3Client.putObject(b -> b.bucket(BUCKET_NAME).key("drones-" + UUID.randomUUID() + ".json"),
                    software.amazon.awssdk.core.sync.RequestBody.fromString(objectMapper.writeValueAsString(drones)));
            System.out.println("Archived to S3");
        }
        catch (Exception e)
        {
            System.err.println("S3 Error: " + e.getMessage());
        }
    }

    private void logToDynamo(String action, String details)
    {
        try
        {
            DynamoDbTable<SystemLog> table = getLogTable();
            try
            {
                table.createTable();
            }
            catch (Exception e) {}
            SystemLog log = new SystemLog();
            log.setLogId(UUID.randomUUID().toString());
            log.setTimestamp(Instant.now().toString());
            log.setAction(action);
            log.setDetails(details);
            table.putItem(log);
            System.out.println("Logged to Dynamo");
        }
        catch (Exception e)
        {
            System.err.println("Dynamo Error: " + e.getMessage());
        }
    }

    private DynamoDbTable<SystemLog> getLogTable()
    {
        return dynamoDbClient.table("SystemLogs", TableSchema.fromBean(SystemLog.class));
    }
}