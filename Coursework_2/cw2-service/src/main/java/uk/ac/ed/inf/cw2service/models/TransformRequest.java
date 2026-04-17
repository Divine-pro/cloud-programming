package uk.ac.ed.inf.cw2service.models;

public class TransformRequest
{
    private String readQueue;
    private String writeQueue;
    private int messageCount;

    public String getReadQueue() { return readQueue; }
    public void setReadQueue(String q) { this.readQueue = q; }
    public String getWriteQueue() { return writeQueue; }
    public void setWriteQueue(String q) { this.writeQueue = q; }
    public int getMessageCount() { return messageCount; }
    public void setMessageCount(int c) { this.messageCount = c; }
}