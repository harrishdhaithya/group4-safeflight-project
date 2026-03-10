package com.safelight.model;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "airlines")
public class Airline {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    private String name;
    private String country;
    
    @OneToMany(mappedBy = "airline")
    private Set<AirlineUser> airlineUsers = new HashSet<>();
    
    @OneToMany(mappedBy = "airline")
    private Set<Flight> flights = new HashSet<>();
    
    // Getters and Setters
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getCountry() {
        return country;
    }
    
    public void setCountry(String country) {
        this.country = country;
    }
    
    public Set<AirlineUser> getAirlineUsers() {
        return airlineUsers;
    }
    
    public void setAirlineUsers(Set<AirlineUser> airlineUsers) {
        this.airlineUsers = airlineUsers;
    }
    
    public Set<Flight> getFlights() {
        return flights;
    }
    
    public void setFlights(Set<Flight> flights) {
        this.flights = flights;
    }
}
