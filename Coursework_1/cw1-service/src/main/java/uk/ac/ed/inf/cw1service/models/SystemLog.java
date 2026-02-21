package uk.ac.ed.inf.cw1service.models;

import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@Data
@DynamoDbBean
public class SystemLog
{
    private String logId;
    private String timestamp;
    private String action;
    private String details;

    @DynamoDbPartitionKey
    public String getLogId()
    {
        return logId;
    }
}