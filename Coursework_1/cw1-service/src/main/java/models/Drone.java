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
    private String id;
    private String name;
    @Embedded
    private Capability capability;
    @JsonProperty("costPer100Moves")
    private double cost100;
    @Embeddable
    @Data
    public static class Capability
    {
        @JsonProperty("cooling")
        private boolean cool;
        @JsonProperty("heating")
        private boolean heat;
        @JsonProperty("costPerMove")
        private double cpm;
        @JsonProperty("costInitial")
        private double ic;
        @JsonProperty("costFinal")
        private double fc;
        private int capacity;
        private int maxMoves;
    }
}