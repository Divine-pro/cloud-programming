package uk.ac.ed.inf.tut2_storage;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@RestController
public class TestController {

    private final S3Client s3Client;
    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;

    // Inject both clients
    public TestController(S3Client s3Client, DynamoDbEnhancedClient dynamoDbEnhancedClient) {
        this.s3Client = s3Client;
        this.dynamoDbEnhancedClient = dynamoDbEnhancedClient;
    }

    // --- S3 Tests ---
    @GetMapping("/test-bucket")
    public String createBucket() {
        String bucketName = "my-test-bucket";
        try {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
            return "Success! Bucket '" + bucketName + "' created.";
        } catch (Exception e) {
            return "Bucket might already exist: " + e.getMessage();
        }
    }

    @GetMapping("/upload-file")
    public String uploadFile() {
        s3Client.putObject(
                PutObjectRequest.builder().bucket("my-test-bucket").key("hello.txt").build(),
                RequestBody.fromString("Hello from Java!")
        );
        return "Success! File uploaded.";
    }

    // --- DynamoDB Test ---
    @GetMapping("/test-dynamo")
    public String testDynamo() {
        // 1. Get a reference to the "Students" table
        DynamoDbTable<Student> studentTable = dynamoDbEnhancedClient.table("Students", TableSchema.fromBean(Student.class));

        // 2. Create the table (if it doesn't exist)
        try {
            studentTable.createTable();
        } catch (Exception e) {
            // Ignore if table already exists
        }

        // 3. Create a Student Object
        Student student = new Student();
        student.setMatricNumber("s1234567");
        student.setFirstName("Kabir");
        student.setYearOfStudy(2026);

        // 4. Save it to the Cloud!
        studentTable.putItem(student);

        return "Success! Saved Student " + student.getMatricNumber() + " to DynamoDB table 'Students'.";
    }
}