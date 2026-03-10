package com.safelight.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "passengers")
public class Passenger {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    private String fname;
    private String lname;
    private LocalDate dob;
    private String phone;
    private String email;
    private String passport;
    
    @OneToMany(mappedBy = "passenger")
    private Set<BookingPassenger> bookingPassengers = new HashSet<>();
    
    // Getters and Setters
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public String getFname() {
        return fname;
    }
    
    public void setFname(String fname) {
        this.fname = fname;
    }
    
    public String getLname() {
        return lname;
    }
    
    public void setLname(String lname) {
        this.lname = lname;
    }
    
    public LocalDate getDob() {
        return dob;
    }
    
    public void setDob(LocalDate dob) {
        this.dob = dob;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPassport() {
        return passport;
    }
    
    public void setPassport(String passport) {
        this.passport = passport;
    }
    
    public Set<BookingPassenger> getBookingPassengers() {
        return bookingPassengers;
    }
    
    public void setBookingPassengers(Set<BookingPassenger> bookingPassengers) {
        this.bookingPassengers = bookingPassengers;
    }
}
