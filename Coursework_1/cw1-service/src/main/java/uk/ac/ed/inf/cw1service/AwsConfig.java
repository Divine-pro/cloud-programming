package uk.ac.ed.inf.cw1service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Configuration
public class AwsConfig
{

    private static final URI LOCAL_STACK_URI = URI.create("http://localhost:4566");
    private static final Region REGION = Region.US_EAST_1;
    private static final StaticCredentialsProvider CREDENTIALS = StaticCredentialsProvider.create(
            AwsBasicCredentials.create("test", "test")
    );

    @Bean
    public S3Client s3Client()
    {
        return S3Client.builder()
                .endpointOverride(LOCAL_STACK_URI)
                .region(REGION)
                .credentialsProvider(CREDENTIALS)
                .forcePathStyle(true) // Required for LocalStack
                .build();
    }

    @Bean
    public DynamoDbClient dynamoDbClient()
    {
        return DynamoDbClient.builder()
                .endpointOverride(LOCAL_STACK_URI)
                .region(REGION)
                .credentialsProvider(CREDENTIALS)
                .build();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient)
    {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }

    @Bean
    public ObjectMapper objectMapper()
    {
        return new ObjectMapper();
    }

    @Bean
    public RestTemplate restTemplate()
    {
        return new RestTemplate();
    }
}
