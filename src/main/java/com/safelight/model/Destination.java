package com.safelight.model;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "destination")
public class Destination {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    private String country;
    private String city;
    private String airport;
    
    @OneToMany(mappedBy = "fromDestination")
    private Set<Route> routesFrom = new HashSet<>();
    
    @OneToMany(mappedBy = "toDestination")
    private Set<Route> routesTo = new HashSet<>();
    
    // Getters and Setters
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public String getCountry() {
        return country;
    }
    
    public void setCountry(String country) {
        this.country = country;
    }
    
    public String getCity() {
        return city;
    }
    
    public void setCity(String city) {
        this.city = city;
    }
    
    public String getAirport() {
        return airport;
    }
    
    public void setAirport(String airport) {
        this.airport = airport;
    }
    
    public Set<Route> getRoutesFrom() {
        return routesFrom;
    }
    
    public void setRoutesFrom(Set<Route> routesFrom) {
        this.routesFrom = routesFrom;
    }
    
    public Set<Route> getRoutesTo() {
        return routesTo;
    }
    
    public void setRoutesTo(Set<Route> routesTo) {
        this.routesTo = routesTo;
    }
}
