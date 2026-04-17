package uk.ac.ed.inf.cw2service.models;

public class SplitterRequest
{
    private String readQueue;
    private String writeTopicOdd;
    private String redisHashOdd;
    private String writeTopicEven;
    private String redisHashEven;
    private int messageCount;

    public String getReadQueue() { return readQueue; }
    public void setReadQueue(String q) { this.readQueue = q; }
    public String getWriteTopicOdd() { return writeTopicOdd; }
    public void setWriteTopicOdd(String t) { this.writeTopicOdd = t; }
    public String getRedisHashOdd() { return redisHashOdd; }
    public void setRedisHashOdd(String h) { this.redisHashOdd = h; }
    public String getWriteTopicEven() { return writeTopicEven; }
    public void setWriteTopicEven(String t) { this.writeTopicEven = t; }
    public String getRedisHashEven() { return redisHashEven; }
    public void setRedisHashEven(String h) { this.redisHashEven = h; }
    public int getMessageCount() { return messageCount; }
    public void setMessageCount(int c) { this.messageCount = c; }
}
