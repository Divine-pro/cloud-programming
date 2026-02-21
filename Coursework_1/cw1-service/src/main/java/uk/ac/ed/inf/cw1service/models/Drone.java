package uk.ac.ed.inf.cw1service.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "drones")
@Data
public class Drone
{

    @Id
    @Column(name = "drone_id")
    @JsonProperty("url")
    private String url;

    private String name;
    private int capacity;

    @JsonProperty("max_moves")
    private int maxMoves;

    private boolean heat;
    private boolean cool;

    @Embedded
    private Capability capability;

    private double cost100;

    @Embeddable
    @Data
    public static class Capability
    {
        private double ic;
        private double fc;
        private double cpm;
    }
}