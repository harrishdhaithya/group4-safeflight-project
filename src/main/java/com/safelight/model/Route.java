package com.safelight.model;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "routes")
public class Route {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @ManyToOne
    @JoinColumn(name = "from_destination_id")
    private Destination fromDestination;
    
    @ManyToOne
    @JoinColumn(name = "to_destination_id")
    private Destination toDestination;
    
    @OneToMany(mappedBy = "route")
    private Set<FlightSchedule> flightSchedules = new HashSet<>();
    
    // Getters and Setters
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public Destination getFromDestination() {
        return fromDestination;
    }
    
    public void setFromDestination(Destination fromDestination) {
        this.fromDestination = fromDestination;
    }
    
    public Destination getToDestination() {
        return toDestination;
    }
    
    public void setToDestination(Destination toDestination) {
        this.toDestination = toDestination;
    }
    
    public Set<FlightSchedule> getFlightSchedules() {
        return flightSchedules;
    }
    
    public void setFlightSchedules(Set<FlightSchedule> flightSchedules) {
        this.flightSchedules = flightSchedules;
    }
}
