package uk.ac.ed.inf.tut2_storage;

import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@Data
@DynamoDbBean // 1. Tells AWS SDK to map this class to a table
public class Student {

    private String matricNumber;
    private String firstName;
    private int yearOfStudy;

    // 2. Define the Primary Key (The unique ID for each student)
    // In DynamoDB, this is called the "Partition Key"
    @DynamoDbPartitionKey
    public String getMatricNumber() {
        return matricNumber;
    }
}