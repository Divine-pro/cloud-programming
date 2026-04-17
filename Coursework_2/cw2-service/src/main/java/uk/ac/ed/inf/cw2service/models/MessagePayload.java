package uk.ac.ed.inf.cw2service.models;

public class MessagePayload
{
    private String uid;
    private int counter;

    public MessagePayload() {}

    public MessagePayload(String uid, int cnt)
    {
        this.uid = uid;
        this.counter = cnt;
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public int getCounter() { return counter; }
    public void setCounter(int cnt) { this.counter = cnt; }
}