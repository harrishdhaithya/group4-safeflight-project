package com.safelight.service;

import com.safelight.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class TicketPdfServiceTest {

    private TicketPdfService service;

    @BeforeEach
    void setUp() {
        service = new TicketPdfService();
    }

    // PDF-U-01/02: fully populated booking → non-empty valid PDF bytes
    @Test
    void generateTicketPdfShouldReturnNonEmptyPdfBytesForFullyPopulatedBooking() throws IOException {
        Booking booking = buildBooking("CONFIRMED", "SUCCESS");
        booking.setBookingPassengers(Set.of(buildBp("1A", "John", "Doe",
                LocalDate.of(1990, 1, 1), "j@test.com", "P1")));

        byte[] result = service.generateTicketPdf(booking);

        assertThat(result).isNotEmpty();
        // %PDF magic bytes
        assertThat(result[0]).isEqualTo((byte) 0x25);
        assertThat(result[1]).isEqualTo((byte) 0x50);
        assertThat(result[2]).isEqualTo((byte) 0x44);
        assertThat(result[3]).isEqualTo((byte) 0x46);
    }

    // PDF-U-03: multiple passengers → no exception
    @Test
    void generateTicketPdfShouldHandleMultiplePassengers() throws IOException {
        Booking booking = buildBooking("CONFIRMED", "SUCCESS");
        Set<BookingPassenger> bps = new HashSet<>();
        bps.add(buildBp("1A", "Alice", "Smith", LocalDate.of(1985, 3, 15), "a@test.com", "PA1"));
        bps.add(buildBp("2B", "Bob", "Jones", LocalDate.of(1990, 7, 20), "b@test.com", "PB2"));
        bps.add(buildBp("3C", "Carol", "White", LocalDate.of(1995, 11, 5), "c@test.com", "PC3"));
        booking.setBookingPassengers(bps);

        byte[] result = service.generateTicketPdf(booking);
        assertThat(result).isNotEmpty();
    }

    // PDF-U-04: booking.status null → no NPE
    @Test
    void generateTicketPdfShouldHandleNullStatus() {
        Booking booking = buildBooking(null, "SUCCESS");
        booking.setBookingPassengers(Set.of());
        assertThatNoException().isThrownBy(() -> service.generateTicketPdf(booking));
    }

    // PDF-U-05: booking.paymentStatus null → no NPE
    @Test
    void generateTicketPdfShouldHandleNullPaymentStatus() {
        Booking booking = buildBooking("CONFIRMED", null);
        booking.setBookingPassengers(Set.of());
        assertThatNoException().isThrownBy(() -> service.generateTicketPdf(booking));
    }

    // PDF-U-06: travelDate null → no NPE
    @Test
    void generateTicketPdfShouldHandleNullTravelDate() {
        Booking booking = buildBooking("CONFIRMED", "SUCCESS");
        booking.getFlightSchedule().setTravelDate(null);
        booking.setBookingPassengers(Set.of());
        assertThatNoException().isThrownBy(() -> service.generateTicketPdf(booking));
    }

    // PDF-U-07: travelTime null → no NPE
    @Test
    void generateTicketPdfShouldHandleNullTravelTime() {
        Booking booking = buildBooking("CONFIRMED", "SUCCESS");
        booking.getFlightSchedule().setTravelTime(null);
        booking.setBookingPassengers(Set.of());
        assertThatNoException().isThrownBy(() -> service.generateTicketPdf(booking));
    }

    // PDF-U-08: bp.seatNo null → filtered, no NPE
    @Test
    void generateTicketPdfShouldHandleNullSeatNo() {
        Booking booking = buildBooking("CONFIRMED", "SUCCESS");
        BookingPassenger bp = buildBp(null, "John", "Doe", null, null, null);
        booking.setBookingPassengers(Set.of(bp));
        assertThatNoException().isThrownBy(() -> service.generateTicketPdf(booking));
    }

    // PDF-U-09: bp.seatNo blank → filtered, no NPE
    @Test
    void generateTicketPdfShouldHandleBlankSeatNo() {
        Booking booking = buildBooking("CONFIRMED", "SUCCESS");
        BookingPassenger bp = buildBp("  ", "John", "Doe", null, null, null);
        booking.setBookingPassengers(Set.of(bp));
        assertThatNoException().isThrownBy(() -> service.generateTicketPdf(booking));
    }

    // PDF-U-10: bp.passenger null → shows "—", no NPE
    @Test
    void generateTicketPdfShouldHandleNullPassenger() {
        Booking booking = buildBooking("CONFIRMED", "SUCCESS");
        BookingPassenger bp = new BookingPassenger();
        bp.setSeatNo("1A");
        bp.setPassenger(null);
        booking.setBookingPassengers(Set.of(bp));
        assertThatNoException().isThrownBy(() -> service.generateTicketPdf(booking));
    }

    // PDF-U-11: passenger.fname null → no NPE
    @Test
    void generateTicketPdfShouldHandleNullFname() {
        Booking booking = buildBooking("CONFIRMED", "SUCCESS");
        booking.setBookingPassengers(Set.of(buildBp("1A", null, "Doe", null, null, null)));
        assertThatNoException().isThrownBy(() -> service.generateTicketPdf(booking));
    }

    // PDF-U-12: passenger.lname null → no NPE
    @Test
    void generateTicketPdfShouldHandleNullLname() {
        Booking booking = buildBooking("CONFIRMED", "SUCCESS");
        booking.setBookingPassengers(Set.of(buildBp("1A", "John", null, null, null, null)));
        assertThatNoException().isThrownBy(() -> service.generateTicketPdf(booking));
    }

    // PDF-U-13: passenger.dob null → shows "—", no NPE
    @Test
    void generateTicketPdfShouldHandleNullDob() {
        Booking booking = buildBooking("CONFIRMED", "SUCCESS");
        booking.setBookingPassengers(Set.of(buildBp("1A", "John", "Doe", null, "j@test.com", "P1")));
        assertThatNoException().isThrownBy(() -> service.generateTicketPdf(booking));
    }

    // PDF-U-14: passenger.email null → shows "—", no NPE
    @Test
    void generateTicketPdfShouldHandleNullEmail() {
        Booking booking = buildBooking("CONFIRMED", "SUCCESS");
        booking.setBookingPassengers(Set.of(buildBp("1A", "John", "Doe",
                LocalDate.of(1990, 1, 1), null, "P1")));
        assertThatNoException().isThrownBy(() -> service.generateTicketPdf(booking));
    }

    // PDF-U-15: passenger.passport null → shows "—", no NPE
    @Test
    void generateTicketPdfShouldHandleNullPassport() {
        Booking booking = buildBooking("CONFIRMED", "SUCCESS");
        booking.setBookingPassengers(Set.of(buildBp("1A", "John", "Doe",
                LocalDate.of(1990, 1, 1), "j@test.com", null)));
        assertThatNoException().isThrownBy(() -> service.generateTicketPdf(booking));
    }

    // PDF-U-16: no passengers → empty table, no NPE
    @Test
    void generateTicketPdfShouldHandleNoPassengers() {
        Booking booking = buildBooking("CONFIRMED", "SUCCESS");
        booking.setBookingPassengers(new HashSet<>());
        assertThatNoException().isThrownBy(() -> service.generateTicketPdf(booking));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Booking buildBooking(String status, String paymentStatus) {
        Destination from = new Destination(); from.setAirport("SYD");
        Destination to = new Destination(); to.setAirport("MEL");
        Route route = new Route(); route.setFromDestination(from); route.setToDestination(to);
        Airline airline = new Airline(); airline.setName("SafeAir");
        Flight flight = new Flight(); flight.setFlightCode("SF101"); flight.setAirline(airline);
        FlightSchedule schedule = new FlightSchedule();
        schedule.setFlight(flight); schedule.setRoute(route);
        schedule.setTravelDate(LocalDate.of(2026, 6, 1));
        schedule.setTravelTime(LocalTime.of(9, 0));

        Booking booking = new Booking();
        booking.setId(1); booking.setStatus(status); booking.setPaymentStatus(paymentStatus);
        booking.setFlightSchedule(schedule);
        return booking;
    }

    private BookingPassenger buildBp(String seatNo, String fname, String lname,
                                     LocalDate dob, String email, String passport) {
        Passenger p = new Passenger();
        p.setFname(fname); p.setLname(lname); p.setDob(dob);
        p.setEmail(email); p.setPassport(passport);

        BookingPassenger bp = new BookingPassenger();
        bp.setSeatNo(seatNo); bp.setPassenger(p);
        return bp;
    }
}
