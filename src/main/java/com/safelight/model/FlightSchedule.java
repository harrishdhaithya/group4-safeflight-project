package com.safelight.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "flight_schedule")
public class FlightSchedule {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @ManyToOne
    @JoinColumn(name = "flight_id")
    private Flight flight;
    
    @ManyToOne
    @JoinColumn(name = "route_id")
    private Route route;
    
    @Column(name = "travel_date")
    private LocalDate travelDate;
    
    @Column(name = "travel_time")
    private LocalTime travelTime;
    
    @OneToMany(mappedBy = "flightSchedule")
    private Set<Booking> bookings = new HashSet<>();
    
    // Getters and Setters
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public Flight getFlight() {
        return flight;
    }
    
    public void setFlight(Flight flight) {
        this.flight = flight;
    }
    
    public Route getRoute() {
        return route;
    }
    
    public void setRoute(Route route) {
        this.route = route;
    }
    
    public LocalDate getTravelDate() {
        return travelDate;
    }
    
    public void setTravelDate(LocalDate travelDate) {
        this.travelDate = travelDate;
    }
    
    public LocalTime getTravelTime() {
        return travelTime;
    }
    
    public void setTravelTime(LocalTime travelTime) {
        this.travelTime = travelTime;
    }
    
    public Set<Booking> getBookings() {
        return bookings;
    }
    
    public void setBookings(Set<Booking> bookings) {
        this.bookings = bookings;
    }
}
