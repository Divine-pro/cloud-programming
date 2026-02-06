package uk.ac.ed.inf.tut2_storage;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient; // NEW IMPORT
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Configuration
public class AwsConfig {

    // 1. Point to LocalStack (Docker)
    private static final URI LOCAL_STACK_URI = URI.create("http://localhost:4566");
    private static final Region REGION = Region.US_EAST_1;

    // 2. Fake Login (LocalStack accepts anything)
    private static final StaticCredentialsProvider CREDENTIALS = StaticCredentialsProvider.create(
            AwsBasicCredentials.create("test", "test")
    );

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .endpointOverride(LOCAL_STACK_URI) // Go to LocalHost
                .region(REGION)
                .credentialsProvider(CREDENTIALS)
                .forcePathStyle(true) // Crucial for LocalStack S3
                .build();
    }

    @Bean
    public DynamoDbClient dynamoDbClient() {
        return DynamoDbClient.builder()
                .endpointOverride(LOCAL_STACK_URI) // Go to LocalHost
                .region(REGION)
                .credentialsProvider(CREDENTIALS)
                .build();
    }

    // 3. NEW: The "Enhanced" Client (Makes saving Java objects easier)
    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }
}