package com.safelight.model;

import jakarta.persistence.*;

@Entity
@Table(name = "booking_passengers")
public class BookingPassenger {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @ManyToOne
    @JoinColumn(name = "booking_id")
    private Booking booking;
    
    @ManyToOne
    @JoinColumn(name = "passenger_id")
    private Passenger passenger;
    
    @Column(name = "seat_no")
    private String seatNo;
    
    @Column(name = "seat_type")
    private String seatType;
    
    @Column(name = "baggage_quantity")
    private Integer baggageQuantity;
    
    // Getters and Setters
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public Booking getBooking() {
        return booking;
    }
    
    public void setBooking(Booking booking) {
        this.booking = booking;
    }
    
    public Passenger getPassenger() {
        return passenger;
    }
    
    public void setPassenger(Passenger passenger) {
        this.passenger = passenger;
    }
    
    public String getSeatNo() {
        return seatNo;
    }
    
    public void setSeatNo(String seatNo) {
        this.seatNo = seatNo;
    }
    
    public String getSeatType() {
        return seatType;
    }
    
    public void setSeatType(String seatType) {
        this.seatType = seatType;
    }
    
    public Integer getBaggageQuantity() {
        return baggageQuantity;
    }
    
    public void setBaggageQuantity(Integer baggageQuantity) {
        this.baggageQuantity = baggageQuantity;
    }
}
