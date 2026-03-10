package com.safelight.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "booking")
public class Booking {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    
    @ManyToOne
    @JoinColumn(name = "flight_schedule_id")
    private FlightSchedule flightSchedule;
    
    private String status;
    
    @Column(name = "booked_time")
    private LocalDateTime bookedTime;
    
    @OneToMany(mappedBy = "booking")
    private Set<BookingPassenger> bookingPassengers = new HashSet<>();
    
    @OneToMany(mappedBy = "booking")
    private Set<Payment> payments = new HashSet<>();
    
    // Getters and Setters
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public FlightSchedule getFlightSchedule() {
        return flightSchedule;
    }
    
    public void setFlightSchedule(FlightSchedule flightSchedule) {
        this.flightSchedule = flightSchedule;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public LocalDateTime getBookedTime() {
        return bookedTime;
    }
    
    public void setBookedTime(LocalDateTime bookedTime) {
        this.bookedTime = bookedTime;
    }
    
    public Set<BookingPassenger> getBookingPassengers() {
        return bookingPassengers;
    }
    
    public void setBookingPassengers(Set<BookingPassenger> bookingPassengers) {
        this.bookingPassengers = bookingPassengers;
    }
    
    public Set<Payment> getPayments() {
        return payments;
    }
    
    public void setPayments(Set<Payment> payments) {
        this.payments = payments;
    }
}
