package uk.ac.ed.inf.cw2service.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SortedMessage
{

    @JsonProperty("Id")
    private int Id;

    @JsonProperty("Payload")
    private String Payload;

    public SortedMessage() {}

    @JsonProperty("Id")
    public int getId() { return Id; }
    public void setId(int id) { this.Id = id; }

    @JsonProperty("Payload")
    public String getPayload() { return Payload; }
    public void setPayload(String p) { this.Payload = p; }
}