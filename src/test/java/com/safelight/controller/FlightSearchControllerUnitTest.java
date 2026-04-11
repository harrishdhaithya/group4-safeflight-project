package com.safelight.controller;

import com.safelight.dto.FlightSearchResultDto;
import com.safelight.dto.SeatMapResponse;
import com.safelight.model.*;
import com.safelight.repository.DestinationRepository;
import com.safelight.repository.FlightScheduleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FlightSearchControllerUnitTest {

    @Mock
    private DestinationRepository destinationRepository;

    @Mock
    private FlightScheduleRepository flightScheduleRepository;

    @InjectMocks
    private FlightSearchController controller;

    private FlightSchedule buildSampleSchedule(int scheduleId) {
        Destination from = new Destination();
        from.setId(1);
        from.setAirport("SYD");

        Destination to = new Destination();
        to.setId(2);
        to.setAirport("MEL");

        Route route = new Route();
        route.setId(10);
        route.setFromDestination(from);
        route.setToDestination(to);

        Airline airline = new Airline();
        airline.setId(5);
        airline.setName("Safe Air");

        FlightModel model = new FlightModel();
        model.setId(3);
        model.setSeatMapping("""
                {"seatPricing":{"Economy Class":300,"Business Class":1500}}
                """);

        Flight flight = new Flight();
        flight.setId(7);
        flight.setFlightCode("SF101");
        flight.setFlightName("Safe Air Test");
        flight.setAirline(airline);
        flight.setFlightModel(model);

        FlightSchedule schedule = new FlightSchedule();
        schedule.setId(scheduleId);
        schedule.setRoute(route);
        schedule.setFlight(flight);
        schedule.setTravelDate(LocalDate.of(2026, 3, 20));
        schedule.setTravelTime(LocalTime.of(8, 30));

        return schedule;
    }

    @Test
    void toSearchResultShouldMapScheduleAndComputeMinPrice() {
        FlightSchedule schedule = buildSampleSchedule(1001);

        // call private logic indirectly via public method
        when(flightScheduleRepository
                .findByRoute_FromDestination_IdAndRoute_ToDestination_IdAndTravelDateOrderByTravelTime(
                        eq(1), eq(2), any(LocalDate.class)))
                .thenReturn(List.of(schedule));

        ResponseEntity<?> response = controller.searchFlights(1, 2, LocalDate.of(2026, 3, 20));

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        @SuppressWarnings("unchecked")
        List<FlightSearchResultDto> body = (List<FlightSearchResultDto>) response.getBody();
        assertThat(body).hasSize(1);
        FlightSearchResultDto dto = body.get(0);
        assertThat(dto.getScheduleId()).isEqualTo(1001);
        assertThat(dto.getFlightCode()).isEqualTo("SF101");
        assertThat(dto.getFromAirport()).isEqualTo("SYD");
        assertThat(dto.getToAirport()).isEqualTo("MEL");
        // min of 300 and 1500
        assertThat(dto.getPriceFrom()).isEqualTo(300);
    }

    @Test
    void searchFlightsShouldReturnEmptyListWhenNoSchedules() {
        when(flightScheduleRepository
                .findByRoute_FromDestination_IdAndRoute_ToDestination_IdAndTravelDateOrderByTravelTime(
                        any(), any(), any()))
                .thenReturn(Collections.emptyList());

        ResponseEntity<?> response = controller.searchFlights(1, 2, LocalDate.of(2026, 3, 21));

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        @SuppressWarnings("unchecked")
        List<FlightSearchResultDto> body = (List<FlightSearchResultDto>) response.getBody();
        assertThat(body).isEmpty();
    }

    @Test
    void getSeatMapShouldReturnSeatMapResponseForValidSchedule() {
        FlightSchedule schedule = buildSampleSchedule(2001);
        when(flightScheduleRepository.findById(2001)).thenReturn(Optional.of(schedule));

        ResponseEntity<?> response = controller.getSeatMap(2001);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        SeatMapResponse dto = (SeatMapResponse) response.getBody();
        assertThat(dto).isNotNull();
        assertThat(dto.getScheduleId()).isEqualTo(2001);
        assertThat(dto.getFlightCode()).isEqualTo("SF101");
        assertThat(dto.getAirlineName()).isEqualTo("Safe Air");
        assertThat(dto.getFromAirport()).isEqualTo("SYD");
        assertThat(dto.getToAirport()).isEqualTo("MEL");
    }

    @Test
    void getSeatMapShouldReturnNotFoundForMissingSchedule() {
        when(flightScheduleRepository.findById(9999)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.getSeatMap(9999);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNull();
    }

    // FSC-U-01/02/03: listDestinations
    @Test
    void listDestinationsShouldReturnMappedDtos() {
        Destination d1 = new Destination(); d1.setId(1); d1.setCountry("AU"); d1.setCity("Sydney"); d1.setAirport("SYD");
        Destination d2 = new Destination(); d2.setId(2); d2.setCountry("AU"); d2.setCity("Melbourne"); d2.setAirport("MEL");
        when(destinationRepository.findAll()).thenReturn(List.of(d1, d2));

        List<com.safelight.dto.DestinationResponse> result = controller.listDestinations();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getAirport()).isEqualTo("SYD");
        assertThat(result.get(0).getCity()).isEqualTo("Sydney");
        assertThat(result.get(0).getCountry()).isEqualTo("AU");
        assertThat(result.get(1).getAirport()).isEqualTo("MEL");
    }

    @Test
    void listDestinationsShouldReturnEmptyListWhenNoneExist() {
        when(destinationRepository.findAll()).thenReturn(List.of());
        assertThat(controller.listDestinations()).isEmpty();
    }

    // FSC-U-07: extractPriceFrom null seatMapping → priceFrom null
    @Test
    void searchFlightsShouldReturnNullPriceWhenSeatMappingIsNull() {
        FlightSchedule schedule = buildSampleSchedule(3001);
        schedule.getFlight().getFlightModel().setSeatMapping(null);
        when(flightScheduleRepository
                .findByRoute_FromDestination_IdAndRoute_ToDestination_IdAndTravelDateOrderByTravelTime(
                        any(), any(), any()))
                .thenReturn(List.of(schedule));

        ResponseEntity<?> response = controller.searchFlights(1, 2, LocalDate.of(2026, 3, 20));

        @SuppressWarnings("unchecked")
        List<com.safelight.dto.FlightSearchResultDto> body =
                (List<com.safelight.dto.FlightSearchResultDto>) response.getBody();
        assertThat(body.get(0).getPriceFrom()).isNull();
    }

    // FSC-U-08: blank seatMapping → priceFrom null
    @Test
    void searchFlightsShouldReturnNullPriceWhenSeatMappingIsBlank() {
        FlightSchedule schedule = buildSampleSchedule(3002);
        schedule.getFlight().getFlightModel().setSeatMapping("   ");
        when(flightScheduleRepository
                .findByRoute_FromDestination_IdAndRoute_ToDestination_IdAndTravelDateOrderByTravelTime(
                        any(), any(), any()))
                .thenReturn(List.of(schedule));

        @SuppressWarnings("unchecked")
        List<com.safelight.dto.FlightSearchResultDto> body =
                (List<com.safelight.dto.FlightSearchResultDto>) controller.searchFlights(1, 2, LocalDate.now()).getBody();
        assertThat(body.get(0).getPriceFrom()).isNull();
    }

    // FSC-U-10: no seatPricing key → priceFrom null
    @Test
    void searchFlightsShouldReturnNullPriceWhenNoPricingKey() {
        FlightSchedule schedule = buildSampleSchedule(3003);
        schedule.getFlight().getFlightModel().setSeatMapping("{\"other\":\"value\"}");
        when(flightScheduleRepository
                .findByRoute_FromDestination_IdAndRoute_ToDestination_IdAndTravelDateOrderByTravelTime(
                        any(), any(), any()))
                .thenReturn(List.of(schedule));

        @SuppressWarnings("unchecked")
        List<com.safelight.dto.FlightSearchResultDto> body =
                (List<com.safelight.dto.FlightSearchResultDto>) controller.searchFlights(1, 2, LocalDate.now()).getBody();
        assertThat(body.get(0).getPriceFrom()).isNull();
    }

    // FSC-U-11: empty seatPricing object → priceFrom null
    @Test
    void searchFlightsShouldReturnNullPriceWhenPricingIsEmpty() {
        FlightSchedule schedule = buildSampleSchedule(3004);
        schedule.getFlight().getFlightModel().setSeatMapping("{\"seatPricing\":{}}");
        when(flightScheduleRepository
                .findByRoute_FromDestination_IdAndRoute_ToDestination_IdAndTravelDateOrderByTravelTime(
                        any(), any(), any()))
                .thenReturn(List.of(schedule));

        @SuppressWarnings("unchecked")
        List<com.safelight.dto.FlightSearchResultDto> body =
                (List<com.safelight.dto.FlightSearchResultDto>) controller.searchFlights(1, 2, LocalDate.now()).getBody();
        assertThat(body.get(0).getPriceFrom()).isNull();
    }

    // FSC-U-12: non-numeric pricing values → priceFrom null
    @Test
    void searchFlightsShouldReturnNullPriceWhenPricingValuesAreNonNumeric() {
        FlightSchedule schedule = buildSampleSchedule(3005);
        schedule.getFlight().getFlightModel().setSeatMapping("{\"seatPricing\":{\"Economy\":\"free\"}}");
        when(flightScheduleRepository
                .findByRoute_FromDestination_IdAndRoute_ToDestination_IdAndTravelDateOrderByTravelTime(
                        any(), any(), any()))
                .thenReturn(List.of(schedule));

        @SuppressWarnings("unchecked")
        List<com.safelight.dto.FlightSearchResultDto> body =
                (List<com.safelight.dto.FlightSearchResultDto>) controller.searchFlights(1, 2, LocalDate.now()).getBody();
        assertThat(body.get(0).getPriceFrom()).isNull();
    }

    // FSC-U-13: malformed JSON → exception caught → priceFrom null
    @Test
    void searchFlightsShouldReturnNullPriceWhenJsonIsMalformed() {
        FlightSchedule schedule = buildSampleSchedule(3006);
        schedule.getFlight().getFlightModel().setSeatMapping("not-json");
        when(flightScheduleRepository
                .findByRoute_FromDestination_IdAndRoute_ToDestination_IdAndTravelDateOrderByTravelTime(
                        any(), any(), any()))
                .thenReturn(List.of(schedule));

        @SuppressWarnings("unchecked")
        List<com.safelight.dto.FlightSearchResultDto> body =
                (List<com.safelight.dto.FlightSearchResultDto>) controller.searchFlights(1, 2, LocalDate.now()).getBody();
        assertThat(body.get(0).getPriceFrom()).isNull();
    }

    // FSC-U-14: single price → that price returned
    @Test
    void searchFlightsShouldReturnSinglePriceWhenOnlyOneEntry() {
        FlightSchedule schedule = buildSampleSchedule(3007);
        schedule.getFlight().getFlightModel().setSeatMapping("{\"seatPricing\":{\"Economy\":250}}");
        when(flightScheduleRepository
                .findByRoute_FromDestination_IdAndRoute_ToDestination_IdAndTravelDateOrderByTravelTime(
                        any(), any(), any()))
                .thenReturn(List.of(schedule));

        @SuppressWarnings("unchecked")
        List<com.safelight.dto.FlightSearchResultDto> body =
                (List<com.safelight.dto.FlightSearchResultDto>) controller.searchFlights(1, 2, LocalDate.now()).getBody();
        assertThat(body.get(0).getPriceFrom()).isEqualTo(250);
    }

    // FSC-U-15: flightModel null → priceFrom null, no NPE
    @Test
    void searchFlightsShouldHandleNullFlightModel() {
        FlightSchedule schedule = buildSampleSchedule(3008);
        schedule.getFlight().setFlightModel(null);
        when(flightScheduleRepository
                .findByRoute_FromDestination_IdAndRoute_ToDestination_IdAndTravelDateOrderByTravelTime(
                        any(), any(), any()))
                .thenReturn(List.of(schedule));

        @SuppressWarnings("unchecked")
        List<com.safelight.dto.FlightSearchResultDto> body =
                (List<com.safelight.dto.FlightSearchResultDto>) controller.searchFlights(1, 2, LocalDate.now()).getBody();
        assertThat(body.get(0).getPriceFrom()).isNull();
    }

    // FSC-U-18: CONFIRMED+SUCCESS booking → seat in bookedSeats
    @Test
    void getSeatMapShouldIncludeConfirmedPaidSeats() {
        FlightSchedule schedule = buildSampleSchedule(4001);
        Booking booking = new Booking();
        booking.setStatus("CONFIRMED"); booking.setPaymentStatus("SUCCESS");
        BookingPassenger bp = new BookingPassenger(); bp.setSeatNo("3A"); bp.setBooking(booking);
        booking.setBookingPassengers(new java.util.HashSet<>(List.of(bp)));
        schedule.setBookings(new java.util.HashSet<>(List.of(booking)));
        when(flightScheduleRepository.findById(4001)).thenReturn(Optional.of(schedule));

        SeatMapResponse dto = (SeatMapResponse) controller.getSeatMap(4001).getBody();
        assertThat(dto.getBookedSeats()).contains("3A");
    }

    // FSC-U-19: PENDING booking → seat NOT in bookedSeats
    @Test
    void getSeatMapShouldExcludePendingBookingSeats() {
        FlightSchedule schedule = buildSampleSchedule(4002);
        Booking booking = new Booking();
        booking.setStatus("PENDING"); booking.setPaymentStatus("PENDING");
        BookingPassenger bp = new BookingPassenger(); bp.setSeatNo("3A"); bp.setBooking(booking);
        booking.setBookingPassengers(new java.util.HashSet<>(List.of(bp)));
        schedule.setBookings(new java.util.HashSet<>(List.of(booking)));
        when(flightScheduleRepository.findById(4002)).thenReturn(Optional.of(schedule));

        SeatMapResponse dto = (SeatMapResponse) controller.getSeatMap(4002).getBody();
        assertThat(dto.getBookedSeats()).doesNotContain("3A");
    }

    // FSC-U-20: CANCELLED booking → seat NOT in bookedSeats
    @Test
    void getSeatMapShouldExcludeCancelledBookingSeats() {
        FlightSchedule schedule = buildSampleSchedule(4003);
        Booking booking = new Booking();
        booking.setStatus("CANCELLED"); booking.setPaymentStatus("SUCCESS");
        BookingPassenger bp = new BookingPassenger(); bp.setSeatNo("4B"); bp.setBooking(booking);
        booking.setBookingPassengers(new java.util.HashSet<>(List.of(bp)));
        schedule.setBookings(new java.util.HashSet<>(List.of(booking)));
        when(flightScheduleRepository.findById(4003)).thenReturn(Optional.of(schedule));

        SeatMapResponse dto = (SeatMapResponse) controller.getSeatMap(4003).getBody();
        assertThat(dto.getBookedSeats()).doesNotContain("4B");
    }

    // FSC-U-21: CONFIRMED+FAILED → seat NOT in bookedSeats
    @Test
    void getSeatMapShouldExcludeConfirmedFailedPaymentSeats() {
        FlightSchedule schedule = buildSampleSchedule(4004);
        Booking booking = new Booking();
        booking.setStatus("CONFIRMED"); booking.setPaymentStatus("FAILED");
        BookingPassenger bp = new BookingPassenger(); bp.setSeatNo("5C"); bp.setBooking(booking);
        booking.setBookingPassengers(new java.util.HashSet<>(List.of(bp)));
        schedule.setBookings(new java.util.HashSet<>(List.of(booking)));
        when(flightScheduleRepository.findById(4004)).thenReturn(Optional.of(schedule));

        SeatMapResponse dto = (SeatMapResponse) controller.getSeatMap(4004).getBody();
        assertThat(dto.getBookedSeats()).doesNotContain("5C");
    }

    // FSC-U-22: null status → seat NOT in bookedSeats
    @Test
    void getSeatMapShouldExcludeNullStatusBookingSeats() {
        FlightSchedule schedule = buildSampleSchedule(4005);
        Booking booking = new Booking();
        booking.setStatus(null); booking.setPaymentStatus("SUCCESS");
        BookingPassenger bp = new BookingPassenger(); bp.setSeatNo("6D"); bp.setBooking(booking);
        booking.setBookingPassengers(new java.util.HashSet<>(List.of(bp)));
        schedule.setBookings(new java.util.HashSet<>(List.of(booking)));
        when(flightScheduleRepository.findById(4005)).thenReturn(Optional.of(schedule));

        SeatMapResponse dto = (SeatMapResponse) controller.getSeatMap(4005).getBody();
        assertThat(dto.getBookedSeats()).doesNotContain("6D");
    }

    // FSC-U-23: null seatNo filtered out
    @Test
    void getSeatMapShouldFilterNullSeatNumbers() {
        FlightSchedule schedule = buildSampleSchedule(4006);
        Booking booking = new Booking();
        booking.setStatus("CONFIRMED"); booking.setPaymentStatus("SUCCESS");
        BookingPassenger bp = new BookingPassenger(); bp.setSeatNo(null); bp.setBooking(booking);
        booking.setBookingPassengers(new java.util.HashSet<>(List.of(bp)));
        schedule.setBookings(new java.util.HashSet<>(List.of(booking)));
        when(flightScheduleRepository.findById(4006)).thenReturn(Optional.of(schedule));

        SeatMapResponse dto = (SeatMapResponse) controller.getSeatMap(4006).getBody();
        assertThat(dto.getBookedSeats()).doesNotContainNull();
    }

    // FSC-U-24: blank seatNo filtered out
    @Test
    void getSeatMapShouldFilterBlankSeatNumbers() {
        FlightSchedule schedule = buildSampleSchedule(4007);
        Booking booking = new Booking();
        booking.setStatus("CONFIRMED"); booking.setPaymentStatus("SUCCESS");
        BookingPassenger bp = new BookingPassenger(); bp.setSeatNo("  "); bp.setBooking(booking);
        booking.setBookingPassengers(new java.util.HashSet<>(List.of(bp)));
        schedule.setBookings(new java.util.HashSet<>(List.of(booking)));
        when(flightScheduleRepository.findById(4007)).thenReturn(Optional.of(schedule));

        SeatMapResponse dto = (SeatMapResponse) controller.getSeatMap(4007).getBody();
        assertThat(dto.getBookedSeats()).doesNotContain("  ");
    }

    // FSC-U-25: duplicate seat across two bookings → appears once (distinct)
    @Test
    void getSeatMapShouldDeduplicateBookedSeats() {
        FlightSchedule schedule = buildSampleSchedule(4008);
        Booking b1 = new Booking(); b1.setStatus("CONFIRMED"); b1.setPaymentStatus("SUCCESS");
        Booking b2 = new Booking(); b2.setStatus("CONFIRMED"); b2.setPaymentStatus("SUCCESS");
        BookingPassenger bp1 = new BookingPassenger(); bp1.setSeatNo("5C"); bp1.setBooking(b1);
        BookingPassenger bp2 = new BookingPassenger(); bp2.setSeatNo("5C"); bp2.setBooking(b2);
        b1.setBookingPassengers(new java.util.HashSet<>(List.of(bp1)));
        b2.setBookingPassengers(new java.util.HashSet<>(List.of(bp2)));
        schedule.setBookings(new java.util.HashSet<>(List.of(b1, b2)));
        when(flightScheduleRepository.findById(4008)).thenReturn(Optional.of(schedule));

        SeatMapResponse dto = (SeatMapResponse) controller.getSeatMap(4008).getBody();
        assertThat(dto.getBookedSeats()).containsExactly("5C");
    }
}

