package com.safelight.model;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "flights")
public class Flight {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @ManyToOne
    @JoinColumn(name = "airline_id")
    private Airline airline;
    
    @Column(name = "flight_name")
    private String flightName;
    
    @Column(name = "flight_code")
    private String flightCode;
    
    @ManyToOne
    @JoinColumn(name = "flight_model_id")
    private FlightModel flightModel;
    
    @OneToMany(mappedBy = "flight")
    private Set<FlightSchedule> flightSchedules = new HashSet<>();
    
    // Getters and Setters
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public Airline getAirline() {
        return airline;
    }
    
    public void setAirline(Airline airline) {
        this.airline = airline;
    }
    
    public String getFlightName() {
        return flightName;
    }
    
    public void setFlightName(String flightName) {
        this.flightName = flightName;
    }
    
    public String getFlightCode() {
        return flightCode;
    }
    
    public void setFlightCode(String flightCode) {
        this.flightCode = flightCode;
    }
    
    public FlightModel getFlightModel() {
        return flightModel;
    }
    
    public void setFlightModel(FlightModel flightModel) {
        this.flightModel = flightModel;
    }
    
    public Set<FlightSchedule> getFlightSchedules() {
        return flightSchedules;
    }
    
    public void setFlightSchedules(Set<FlightSchedule> flightSchedules) {
        this.flightSchedules = flightSchedules;
    }
}
