package com.safelight.model;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "flight_model")
public class FlightModel {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "model_number")
    private String modelNumber;
    
    @Column(name = "model_name")
    private String modelName;
    
    private String manufacturer;
    
    @Column(name = "seat_mapping", columnDefinition = "json")
    private String seatMapping;
    
    @OneToMany(mappedBy = "flightModel")
    private Set<Flight> flights = new HashSet<>();
    
    // Getters and Setters
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public String getModelNumber() {
        return modelNumber;
    }
    
    public void setModelNumber(String modelNumber) {
        this.modelNumber = modelNumber;
    }
    
    public String getModelName() {
        return modelName;
    }
    
    public void setModelName(String modelName) {
        this.modelName = modelName;
    }
    
    public String getManufacturer() {
        return manufacturer;
    }
    
    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }
    
    public String getSeatMapping() {
        return seatMapping;
    }
    
    public void setSeatMapping(String seatMapping) {
        this.seatMapping = seatMapping;
    }
    
    public Set<Flight> getFlights() {
        return flights;
    }
    
    public void setFlights(Set<Flight> flights) {
        this.flights = flights;
    }
}
