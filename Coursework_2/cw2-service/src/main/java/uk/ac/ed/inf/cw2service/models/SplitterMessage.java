package uk.ac.ed.inf.cw2service.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SplitterMessage
{

    @JsonProperty("Id")
    private int Id;

    @JsonProperty("Value")
    private double Value;

    @JsonProperty("AdditionalData")
    private String AdditionalData;

    public SplitterMessage() {}

    @JsonProperty("Id")
    public int getId() { return Id; }
    public void setId(int id) { this.Id = id; }

    @JsonProperty("Value")
    public double getValue() { return Value; }
    public void setValue(double v) { this.Value = v; }

    @JsonProperty("AdditionalData")
    public String getAdditionalData() { return AdditionalData; }
    public void setAdditionalData(String d) { this.AdditionalData = d; }
}