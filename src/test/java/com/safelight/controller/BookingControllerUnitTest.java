package com.safelight.controller;

import com.safelight.dto.CreateBookingRequest;
import com.safelight.model.*;
import com.safelight.repository.*;
import com.safelight.service.TicketPdfService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingControllerUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FlightScheduleRepository flightScheduleRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private PassengerRepository passengerRepository;

    @Mock
    private BookingPassengerRepository bookingPassengerRepository;

    @Mock
    private TicketPdfService ticketPdfService;

    @Mock
    private HttpSession session;

    @InjectMocks
    private BookingController controller;

    @Test
    void createBookingShouldReturnUnauthorizedWhenNoSessionUser() {
        when(session.getAttribute("USER_ID")).thenReturn(null);

        ResponseEntity<?> response = controller.createBooking(null, session);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isEqualTo("Not logged in");
        verifyNoInteractions(userRepository, flightScheduleRepository, bookingRepository);
    }

    @Test
    void createBookingShouldReturnUnauthorizedWhenUserMissing() {
        when(session.getAttribute("USER_ID")).thenReturn(99);
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.createBooking(new CreateBookingRequest(), session);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isEqualTo("Not logged in");
        verify(userRepository).findById(99);
        verifyNoInteractions(flightScheduleRepository, bookingRepository);
    }

    @Test
    void createBookingShouldReturnBadRequestWhenScheduleInvalid() {
        when(session.getAttribute("USER_ID")).thenReturn(1);
        User user = new User();
        user.setId(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        CreateBookingRequest req = new CreateBookingRequest();
        req.setScheduleId(123);
        req.setSeats(List.of());

        ResponseEntity<?> response = controller.createBooking(req, session);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("Missing scheduleId or seats");
    }

    @Test
    void createBookingShouldReturnConflictWhenSeatAlreadyBooked() {
        when(session.getAttribute("USER_ID")).thenReturn(1);
        User user = new User();
        user.setId(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        FlightSchedule schedule = new FlightSchedule();
        schedule.setId(10);
        when(flightScheduleRepository.findById(10)).thenReturn(Optional.of(schedule));

        Booking existingBooking = new Booking();
        existingBooking.setStatus("CONFIRMED");
        existingBooking.setPaymentStatus("SUCCESS");
        
        BookingPassenger existingBp = new BookingPassenger();
        existingBp.setSeatNo("10B");
        existingBp.setBooking(existingBooking);
        when(bookingPassengerRepository.findByBooking_FlightSchedule_Id(10))
                .thenReturn(List.of(existingBp));

        CreateBookingRequest.SeatRequest seatReq = new CreateBookingRequest.SeatRequest();
        seatReq.setSeatNo("10B");
        seatReq.setFname("Test");
        seatReq.setLname("User");
        seatReq.setDob(LocalDate.of(1990, 1, 1).toString());
        seatReq.setPhone("1234567890");
        seatReq.setEmail("test@example.com");
        seatReq.setPassport("P12345");

        CreateBookingRequest req = new CreateBookingRequest();
        req.setScheduleId(10);
        req.setSeats(List.of(seatReq));

        ResponseEntity<?> response = controller.createBooking(req, session);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isEqualTo("Seat already booked: 10B");
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBookingShouldReturnBadRequestWhenSeatNumberMissing() {
        when(session.getAttribute("USER_ID")).thenReturn(1);
        User user = new User();
        user.setId(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        FlightSchedule schedule = new FlightSchedule();
        schedule.setId(20);
        when(flightScheduleRepository.findById(20)).thenReturn(Optional.of(schedule));
        when(bookingPassengerRepository.findByBooking_FlightSchedule_Id(20))
                .thenReturn(List.of());

        CreateBookingRequest.SeatRequest seatReq = new CreateBookingRequest.SeatRequest();
        seatReq.setSeatNo("  "); // blank seat number
        seatReq.setFname("NoSeat");
        seatReq.setLname("User");

        CreateBookingRequest req = new CreateBookingRequest();
        req.setScheduleId(20);
        req.setSeats(List.of(seatReq));

        ResponseEntity<?> response = controller.createBooking(req, session);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("Seat number is required");
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBookingShouldReturnConflictWhenDuplicateSeatsInSameRequest() {
        when(session.getAttribute("USER_ID")).thenReturn(1);
        User user = new User();
        user.setId(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        FlightSchedule schedule = new FlightSchedule();
        schedule.setId(30);
        when(flightScheduleRepository.findById(30)).thenReturn(Optional.of(schedule));
        // no existing bookings, conflict comes purely from duplicate seats in the request
        when(bookingPassengerRepository.findByBooking_FlightSchedule_Id(30))
                .thenReturn(List.of());

        CreateBookingRequest.SeatRequest seatReq1 = new CreateBookingRequest.SeatRequest();
        seatReq1.setSeatNo("20a");
        seatReq1.setFname("Alice");
        seatReq1.setLname("One");

        CreateBookingRequest.SeatRequest seatReq2 = new CreateBookingRequest.SeatRequest();
        seatReq2.setSeatNo("20A"); // same seat, different case
        seatReq2.setFname("Alice");
        seatReq2.setLname("Two");

        CreateBookingRequest req = new CreateBookingRequest();
        req.setScheduleId(30);
        req.setSeats(List.of(seatReq1, seatReq2));

        ResponseEntity<?> response = controller.createBooking(req, session);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isEqualTo("Duplicate seat in request: 20A");
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBookingShouldNormalizeSeatAndBaggageAndPassengerDob() {
        when(session.getAttribute("USER_ID")).thenReturn(1);
        User user = new User();
        user.setId(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        FlightSchedule schedule = new FlightSchedule();
        schedule.setId(40);
        // minimal graph to avoid NPEs when building BookingSummaryResponse
        Destination from = new Destination();
        from.setAirport("FROM");
        Destination to = new Destination();
        to.setAirport("TO");
        Route route = new Route();
        route.setFromDestination(from);
        route.setToDestination(to);
        Airline airline = new Airline();
        airline.setName("Test Airline");
        Flight flight = new Flight();
        flight.setFlightCode("TST100");
        flight.setAirline(airline);
        schedule.setRoute(route);
        schedule.setFlight(flight);
        schedule.setTravelDate(LocalDate.of(2030, 1, 1));
        schedule.setTravelTime(java.time.LocalTime.of(10, 0));
        when(flightScheduleRepository.findById(40)).thenReturn(Optional.of(schedule));
        when(bookingPassengerRepository.findByBooking_FlightSchedule_Id(40))
                .thenReturn(List.of());

        Booking persistedBooking = new Booking();
        persistedBooking.setId(100);
        persistedBooking.setUser(user);
        persistedBooking.setFlightSchedule(schedule);
        when(bookingRepository.save(any(Booking.class))).thenReturn(persistedBooking);

        // echo back the same Passenger / BookingPassenger instances that are passed in
        when(passengerRepository.save(any(Passenger.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, Passenger.class));
        when(bookingPassengerRepository.save(any(BookingPassenger.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, BookingPassenger.class));

        CreateBookingRequest.SeatRequest seatReq1 = new CreateBookingRequest.SeatRequest();
        seatReq1.setSeatNo("10a"); // will be uppercased
        seatReq1.setFname("John");
        seatReq1.setLname("Doe");
        seatReq1.setDob("1990-01-01");
        seatReq1.setPhone("1111111111");
        seatReq1.setEmail("john@example.com");
        seatReq1.setPassport("P111");
        seatReq1.setBaggageQuantity(-1); // will be normalized to 0

        CreateBookingRequest.SeatRequest seatReq2 = new CreateBookingRequest.SeatRequest();
        seatReq2.setSeatNo("11b"); // will be uppercased
        seatReq2.setFname("Jane");
        seatReq2.setLname("Roe");
        seatReq2.setDob("not-a-date"); // invalid, will be parsed as null
        seatReq2.setPhone("2222222222");
        seatReq2.setEmail("jane@example.com");
        seatReq2.setPassport("P222");
        seatReq2.setBaggageQuantity(null); // will be normalized to 0

        CreateBookingRequest req = new CreateBookingRequest();
        req.setScheduleId(40);
        req.setSeats(List.of(seatReq1, seatReq2));

        ResponseEntity<?> response = controller.createBooking(req, session);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // verify passengers persisted with correct DOB handling
        var passengerCaptor = org.mockito.ArgumentCaptor.forClass(Passenger.class);
        verify(passengerRepository, org.mockito.Mockito.times(2)).save(passengerCaptor.capture());
        List<Passenger> savedPassengers = passengerCaptor.getAllValues();
        assertThat(savedPassengers.get(0).getDob()).isEqualTo(LocalDate.of(1990, 1, 1));
        assertThat(savedPassengers.get(1).getDob()).isNull();

        // verify booking passengers persisted with normalized seat and baggage quantity
        var bookingPassengerCaptor = org.mockito.ArgumentCaptor.forClass(BookingPassenger.class);
        verify(bookingPassengerRepository, org.mockito.Mockito.times(2)).save(bookingPassengerCaptor.capture());
        List<BookingPassenger> savedBookingPassengers = bookingPassengerCaptor.getAllValues();

        assertThat(savedBookingPassengers.get(0).getSeatNo()).isEqualTo("10A");
        assertThat(savedBookingPassengers.get(1).getSeatNo()).isEqualTo("11B");
        assertThat(savedBookingPassengers.get(0).getBaggageQuantity()).isEqualTo(0);
        assertThat(savedBookingPassengers.get(1).getBaggageQuantity()).isEqualTo(0);
    }

    @Test
    void listMyBookingsShouldReturnUnauthorizedWhenNoSessionUser() {
        when(session.getAttribute("USER_ID")).thenReturn(null);

        ResponseEntity<?> response = controller.listMyBookings(session);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isEqualTo("Not logged in");
        verifyNoInteractions(bookingRepository);
    }

    @Test
    void getBookingShouldReturnForbiddenWhenUserIsNotOwner() {
        when(session.getAttribute("USER_ID")).thenReturn(2);

        User owner = new User();
        owner.setId(1);

        Booking booking = new Booking();
        booking.setId(5);
        booking.setUser(owner);

        when(bookingRepository.findById(5)).thenReturn(Optional.of(booking));

        ResponseEntity<?> response = controller.getBooking(5, session);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ── listMyBookings ────────────────────────────────────────────────────────

    @Test
    void listMyBookingsShouldReturnBookingSummariesForValidUser() {
        when(session.getAttribute("USER_ID")).thenReturn(1);
        FlightSchedule schedule = buildMinimalSchedule();
        Booking b = new Booking();
        b.setId(10); b.setStatus("CONFIRMED"); b.setPaymentStatus("SUCCESS");
        b.setFlightSchedule(schedule);
        b.setBookingPassengers(new java.util.HashSet<>());
        when(bookingRepository.findByUser_IdOrderByBookedTimeDesc(1)).thenReturn(List.of(b));

        ResponseEntity<?> response = controller.listMyBookings(session);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        List<com.safelight.dto.BookingSummaryResponse> body =
                (List<com.safelight.dto.BookingSummaryResponse>) response.getBody();
        assertThat(body).hasSize(1);
        assertThat(body.get(0).getBookingId()).isEqualTo(10);
        assertThat(body.get(0).getStatus()).isEqualTo("CONFIRMED");
    }

    @Test
    void listMyBookingsShouldReturnEmptyListWhenNoBookings() {
        when(session.getAttribute("USER_ID")).thenReturn(1);
        when(bookingRepository.findByUser_IdOrderByBookedTimeDesc(1)).thenReturn(List.of());

        ResponseEntity<?> response = controller.listMyBookings(session);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((List<?>) response.getBody()).isEmpty();
    }

    @Test
    void listMyBookingsShouldFilterNullSeatNumbers() {
        when(session.getAttribute("USER_ID")).thenReturn(1);
        FlightSchedule schedule = buildMinimalSchedule();
        BookingPassenger bpNull = new BookingPassenger(); bpNull.setSeatNo(null);
        BookingPassenger bpValid = new BookingPassenger(); bpValid.setSeatNo("3A");
        Booking b = new Booking();
        b.setId(20); b.setStatus("CONFIRMED"); b.setPaymentStatus("SUCCESS");
        b.setFlightSchedule(schedule);
        b.setBookingPassengers(new java.util.HashSet<>(List.of(bpNull, bpValid)));
        when(bookingRepository.findByUser_IdOrderByBookedTimeDesc(1)).thenReturn(List.of(b));

        ResponseEntity<?> response = controller.listMyBookings(session);

        @SuppressWarnings("unchecked")
        List<com.safelight.dto.BookingSummaryResponse> body =
                (List<com.safelight.dto.BookingSummaryResponse>) response.getBody();
        assertThat(body.get(0).getSeats()).containsExactly("3A");
    }

    // ── createBooking extra branches ──────────────────────────────────────────

    @Test
    void createBookingShouldReturnBadRequestWhenScheduleIdIsNull() {
        when(session.getAttribute("USER_ID")).thenReturn(1);
        User user = new User(); user.setId(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        CreateBookingRequest req = new CreateBookingRequest();
        req.setScheduleId(null);
        req.setSeats(List.of(new CreateBookingRequest.SeatRequest()));

        ResponseEntity<?> response = controller.createBooking(req, session);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("Missing scheduleId or seats");
    }

    @Test
    void createBookingShouldReturnBadRequestWhenScheduleNotFound() {
        when(session.getAttribute("USER_ID")).thenReturn(1);
        User user = new User(); user.setId(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        CreateBookingRequest.SeatRequest seat = new CreateBookingRequest.SeatRequest();
        seat.setSeatNo("1A");
        CreateBookingRequest req = new CreateBookingRequest();
        req.setScheduleId(999); req.setSeats(List.of(seat));
        when(flightScheduleRepository.findById(999)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.createBooking(req, session);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("Invalid schedule");
    }

    @Test
    void createBookingShouldReturnBadRequestWhenSeatNumberIsNull() {
        when(session.getAttribute("USER_ID")).thenReturn(1);
        User user = new User(); user.setId(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        FlightSchedule schedule = new FlightSchedule(); schedule.setId(50);
        when(flightScheduleRepository.findById(50)).thenReturn(Optional.of(schedule));
        when(bookingPassengerRepository.findByBooking_FlightSchedule_Id(50)).thenReturn(List.of());

        CreateBookingRequest.SeatRequest seat = new CreateBookingRequest.SeatRequest();
        seat.setSeatNo(null);
        CreateBookingRequest req = new CreateBookingRequest();
        req.setScheduleId(50); req.setSeats(List.of(seat));

        ResponseEntity<?> response = controller.createBooking(req, session);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("Seat number is required");
    }

    @Test
    void createBookingShouldSucceedWhenExistingBookingIsPending() {
        when(session.getAttribute("USER_ID")).thenReturn(1);
        User user = new User(); user.setId(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        FlightSchedule schedule = buildMinimalSchedule(); schedule.setId(60);
        when(flightScheduleRepository.findById(60)).thenReturn(Optional.of(schedule));

        Booking pending = new Booking(); pending.setStatus("PENDING"); pending.setPaymentStatus("PENDING");
        BookingPassenger existingBp = new BookingPassenger();
        existingBp.setSeatNo("5B"); existingBp.setBooking(pending);
        when(bookingPassengerRepository.findByBooking_FlightSchedule_Id(60)).thenReturn(List.of(existingBp));

        Booking saved = new Booking(); saved.setId(200); saved.setFlightSchedule(schedule);
        when(bookingRepository.save(any(Booking.class))).thenReturn(saved);
        when(passengerRepository.save(any(Passenger.class))).thenAnswer(inv -> inv.getArgument(0, Passenger.class));
        when(bookingPassengerRepository.save(any(BookingPassenger.class))).thenAnswer(inv -> inv.getArgument(0, BookingPassenger.class));

        CreateBookingRequest.SeatRequest seat = new CreateBookingRequest.SeatRequest();
        seat.setSeatNo("5B"); seat.setFname("A"); seat.setLname("B");
        CreateBookingRequest req = new CreateBookingRequest();
        req.setScheduleId(60); req.setSeats(List.of(seat));

        assertThat(controller.createBooking(req, session).getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void createBookingShouldSucceedWhenExistingBookingIsCancelled() {
        when(session.getAttribute("USER_ID")).thenReturn(1);
        User user = new User(); user.setId(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        FlightSchedule schedule = buildMinimalSchedule(); schedule.setId(61);
        when(flightScheduleRepository.findById(61)).thenReturn(Optional.of(schedule));

        Booking cancelled = new Booking(); cancelled.setStatus("CANCELLED"); cancelled.setPaymentStatus("SUCCESS");
        BookingPassenger existingBp = new BookingPassenger();
        existingBp.setSeatNo("6C"); existingBp.setBooking(cancelled);
        when(bookingPassengerRepository.findByBooking_FlightSchedule_Id(61)).thenReturn(List.of(existingBp));

        Booking saved = new Booking(); saved.setId(201); saved.setFlightSchedule(schedule);
        when(bookingRepository.save(any(Booking.class))).thenReturn(saved);
        when(passengerRepository.save(any(Passenger.class))).thenAnswer(inv -> inv.getArgument(0, Passenger.class));
        when(bookingPassengerRepository.save(any(BookingPassenger.class))).thenAnswer(inv -> inv.getArgument(0, BookingPassenger.class));

        CreateBookingRequest.SeatRequest seat = new CreateBookingRequest.SeatRequest();
        seat.setSeatNo("6C"); seat.setFname("A"); seat.setLname("B");
        CreateBookingRequest req = new CreateBookingRequest();
        req.setScheduleId(61); req.setSeats(List.of(seat));

        assertThat(controller.createBooking(req, session).getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void createBookingShouldTreatConfirmedNullPaymentStatusAsTaken() {
        when(session.getAttribute("USER_ID")).thenReturn(1);
        User user = new User(); user.setId(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        FlightSchedule schedule = new FlightSchedule(); schedule.setId(70);
        when(flightScheduleRepository.findById(70)).thenReturn(Optional.of(schedule));

        Booking existing = new Booking(); existing.setStatus("CONFIRMED"); existing.setPaymentStatus(null);
        BookingPassenger bp = new BookingPassenger(); bp.setSeatNo("7D"); bp.setBooking(existing);
        when(bookingPassengerRepository.findByBooking_FlightSchedule_Id(70)).thenReturn(List.of(bp));

        CreateBookingRequest.SeatRequest seat = new CreateBookingRequest.SeatRequest();
        seat.setSeatNo("7D");
        CreateBookingRequest req = new CreateBookingRequest();
        req.setScheduleId(70); req.setSeats(List.of(seat));

        ResponseEntity<?> response = controller.createBooking(req, session);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isEqualTo("Seat already booked: 7D");
    }

    @Test
    void createBookingShouldHandleNullDob() {
        when(session.getAttribute("USER_ID")).thenReturn(1);
        User user = new User(); user.setId(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        FlightSchedule schedule = buildMinimalSchedule(); schedule.setId(80);
        when(flightScheduleRepository.findById(80)).thenReturn(Optional.of(schedule));
        when(bookingPassengerRepository.findByBooking_FlightSchedule_Id(80)).thenReturn(List.of());

        Booking saved = new Booking(); saved.setId(300); saved.setFlightSchedule(schedule);
        when(bookingRepository.save(any(Booking.class))).thenReturn(saved);
        when(passengerRepository.save(any(Passenger.class))).thenAnswer(inv -> inv.getArgument(0, Passenger.class));
        when(bookingPassengerRepository.save(any(BookingPassenger.class))).thenAnswer(inv -> inv.getArgument(0, BookingPassenger.class));

        CreateBookingRequest.SeatRequest seat = new CreateBookingRequest.SeatRequest();
        seat.setSeatNo("1A"); seat.setFname("A"); seat.setLname("B"); seat.setDob(null);
        CreateBookingRequest req = new CreateBookingRequest();
        req.setScheduleId(80); req.setSeats(List.of(seat));

        controller.createBooking(req, session);

        var captor = org.mockito.ArgumentCaptor.forClass(Passenger.class);
        verify(passengerRepository).save(captor.capture());
        assertThat(captor.getValue().getDob()).isNull();
    }

    @Test
    void createBookingShouldHandleBlankDob() {
        when(session.getAttribute("USER_ID")).thenReturn(1);
        User user = new User(); user.setId(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        FlightSchedule schedule = buildMinimalSchedule(); schedule.setId(81);
        when(flightScheduleRepository.findById(81)).thenReturn(Optional.of(schedule));
        when(bookingPassengerRepository.findByBooking_FlightSchedule_Id(81)).thenReturn(List.of());

        Booking saved = new Booking(); saved.setId(301); saved.setFlightSchedule(schedule);
        when(bookingRepository.save(any(Booking.class))).thenReturn(saved);
        when(passengerRepository.save(any(Passenger.class))).thenAnswer(inv -> inv.getArgument(0, Passenger.class));
        when(bookingPassengerRepository.save(any(BookingPassenger.class))).thenAnswer(inv -> inv.getArgument(0, BookingPassenger.class));

        CreateBookingRequest.SeatRequest seat = new CreateBookingRequest.SeatRequest();
        seat.setSeatNo("1B"); seat.setFname("A"); seat.setLname("B"); seat.setDob("");
        CreateBookingRequest req = new CreateBookingRequest();
        req.setScheduleId(81); req.setSeats(List.of(seat));

        controller.createBooking(req, session);

        var captor = org.mockito.ArgumentCaptor.forClass(Passenger.class);
        verify(passengerRepository).save(captor.capture());
        assertThat(captor.getValue().getDob()).isNull();
    }

    @Test
    void createBookingShouldStorePositiveBaggageQuantityAsIs() {
        when(session.getAttribute("USER_ID")).thenReturn(1);
        User user = new User(); user.setId(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        FlightSchedule schedule = buildMinimalSchedule(); schedule.setId(82);
        when(flightScheduleRepository.findById(82)).thenReturn(Optional.of(schedule));
        when(bookingPassengerRepository.findByBooking_FlightSchedule_Id(82)).thenReturn(List.of());

        Booking saved = new Booking(); saved.setId(302); saved.setFlightSchedule(schedule);
        when(bookingRepository.save(any(Booking.class))).thenReturn(saved);
        when(passengerRepository.save(any(Passenger.class))).thenAnswer(inv -> inv.getArgument(0, Passenger.class));
        when(bookingPassengerRepository.save(any(BookingPassenger.class))).thenAnswer(inv -> inv.getArgument(0, BookingPassenger.class));

        CreateBookingRequest.SeatRequest seat = new CreateBookingRequest.SeatRequest();
        seat.setSeatNo("2A"); seat.setFname("A"); seat.setLname("B"); seat.setBaggageQuantity(3);
        CreateBookingRequest req = new CreateBookingRequest();
        req.setScheduleId(82); req.setSeats(List.of(seat));

        controller.createBooking(req, session);

        var captor = org.mockito.ArgumentCaptor.forClass(BookingPassenger.class);
        verify(bookingPassengerRepository).save(captor.capture());
        assertThat(captor.getValue().getBaggageQuantity()).isEqualTo(3);
    }

    // ── getBooking ────────────────────────────────────────────────────────────

    @Test
    void getBookingShouldReturnUnauthorizedWhenNoSession() {
        when(session.getAttribute("USER_ID")).thenReturn(null);
        assertThat(controller.getBooking(1, session).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getBookingShouldReturnNotFoundWhenBookingMissing() {
        when(session.getAttribute("USER_ID")).thenReturn(1);
        when(bookingRepository.findById(99)).thenReturn(Optional.empty());
        assertThat(controller.getBooking(99, session).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getBookingShouldReturnFullDtoWithPassengers() {
        when(session.getAttribute("USER_ID")).thenReturn(1);
        User owner = new User(); owner.setId(1);
        FlightSchedule schedule = buildMinimalSchedule();

        Passenger p = new Passenger();
        p.setFname("John"); p.setLname("Doe");
        p.setDob(LocalDate.of(1990, 1, 1));
        p.setPhone("123"); p.setEmail("j@test.com"); p.setPassport("P1");

        BookingPassenger bp = new BookingPassenger(); bp.setSeatNo("3A"); bp.setPassenger(p);

        Booking booking = new Booking();
        booking.setId(7); booking.setUser(owner);
        booking.setStatus("CONFIRMED"); booking.setPaymentStatus("SUCCESS");
        booking.setFlightSchedule(schedule);
        booking.setBookingPassengers(new java.util.HashSet<>(List.of(bp)));
        when(bookingRepository.findById(7)).thenReturn(Optional.of(booking));

        ResponseEntity<?> response = controller.getBooking(7, session);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        com.safelight.dto.BookingSummaryResponse dto =
                (com.safelight.dto.BookingSummaryResponse) response.getBody();
        assertThat(dto.getSeats()).containsExactly("3A");
        assertThat(dto.getPassengers()).hasSize(1);
        assertThat(dto.getPassengers().get(0).getFname()).isEqualTo("John");
    }

    @Test
    void getBookingShouldFilterNullSeatNoFromSeats() {
        when(session.getAttribute("USER_ID")).thenReturn(1);
        User owner = new User(); owner.setId(1);
        FlightSchedule schedule = buildMinimalSchedule();

        BookingPassenger bpNull = new BookingPassenger(); bpNull.setSeatNo(null);
        BookingPassenger bpValid = new BookingPassenger(); bpValid.setSeatNo("4B");

        Booking booking = new Booking();
        booking.setId(8); booking.setUser(owner); booking.setFlightSchedule(schedule);
        booking.setBookingPassengers(new java.util.HashSet<>(List.of(bpNull, bpValid)));
        when(bookingRepository.findById(8)).thenReturn(Optional.of(booking));

        com.safelight.dto.BookingSummaryResponse dto =
                (com.safelight.dto.BookingSummaryResponse) controller.getBooking(8, session).getBody();
        assertThat(dto.getSeats()).containsExactly("4B");
    }

    @Test
    void getBookingShouldHandleNullPassengerInBookingPassenger() {
        when(session.getAttribute("USER_ID")).thenReturn(1);
        User owner = new User(); owner.setId(1);
        FlightSchedule schedule = buildMinimalSchedule();

        BookingPassenger bp = new BookingPassenger(); bp.setSeatNo("5C"); bp.setPassenger(null);

        Booking booking = new Booking();
        booking.setId(9); booking.setUser(owner); booking.setFlightSchedule(schedule);
        booking.setBookingPassengers(new java.util.HashSet<>(List.of(bp)));
        when(bookingRepository.findById(9)).thenReturn(Optional.of(booking));

        ResponseEntity<?> response = controller.getBooking(9, session);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        com.safelight.dto.BookingSummaryResponse dto =
                (com.safelight.dto.BookingSummaryResponse) response.getBody();
        assertThat(dto.getPassengers().get(0).getFname()).isNull();
    }

    // ── recordPayment ─────────────────────────────────────────────────────────

    @Test
    void recordPaymentShouldReturnUnauthorizedWhenNoSession() {
        when(session.getAttribute("USER_ID")).thenReturn(null);
        assertThat(controller.recordPayment(1, session).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void recordPaymentShouldReturnNotFoundWhenBookingMissing() {
        when(session.getAttribute("USER_ID")).thenReturn(1);
        when(bookingRepository.findById(99)).thenReturn(Optional.empty());
        assertThat(controller.recordPayment(99, session).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void recordPaymentShouldReturnForbiddenWhenNotOwner() {
        when(session.getAttribute("USER_ID")).thenReturn(2);
        User owner = new User(); owner.setId(1);
        Booking booking = new Booking(); booking.setId(5); booking.setUser(owner);
        when(bookingRepository.findById(5)).thenReturn(Optional.of(booking));
        assertThat(controller.recordPayment(5, session).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void recordPaymentShouldConfirmBookingAndReturn200() {
        when(session.getAttribute("USER_ID")).thenReturn(1);
        User owner = new User(); owner.setId(1);
        Booking booking = new Booking();
        booking.setId(5); booking.setUser(owner);
        booking.setStatus("PENDING"); booking.setPaymentStatus("PENDING");
        when(bookingRepository.findById(5)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0, Booking.class));

        ResponseEntity<?> response = controller.recordPayment(5, session);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var captor = org.mockito.ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("CONFIRMED");
        assertThat(captor.getValue().getPaymentStatus()).isEqualTo("SUCCESS");
    }

    // ── downloadTicketPdf ─────────────────────────────────────────────────────

    @Test
    void downloadTicketPdfShouldReturnUnauthorizedWhenNoSession() {
        when(session.getAttribute("USER_ID")).thenReturn(null);
        assertThat(controller.downloadTicketPdf(1, session).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void downloadTicketPdfShouldReturnNotFoundWhenBookingMissing() {
        when(session.getAttribute("USER_ID")).thenReturn(1);
        when(bookingRepository.findById(99)).thenReturn(Optional.empty());
        assertThat(controller.downloadTicketPdf(99, session).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void downloadTicketPdfShouldReturnForbiddenWhenNotOwner() {
        when(session.getAttribute("USER_ID")).thenReturn(2);
        User owner = new User(); owner.setId(1);
        Booking booking = new Booking(); booking.setId(5); booking.setUser(owner);
        when(bookingRepository.findById(5)).thenReturn(Optional.of(booking));
        assertThat(controller.downloadTicketPdf(5, session).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void downloadTicketPdfShouldReturn500WhenPdfGenerationFails() throws Exception {
        when(session.getAttribute("USER_ID")).thenReturn(1);
        User owner = new User(); owner.setId(1);
        Booking booking = new Booking(); booking.setId(5); booking.setUser(owner);
        when(bookingRepository.findById(5)).thenReturn(Optional.of(booking));
        when(ticketPdfService.generateTicketPdf(booking)).thenThrow(new java.io.IOException("fail"));

        assertThat(controller.downloadTicketPdf(5, session).getStatusCode())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void downloadTicketPdfShouldReturnPdfBytesWithContentDispositionHeader() throws Exception {
        when(session.getAttribute("USER_ID")).thenReturn(1);
        User owner = new User(); owner.setId(1);
        Booking booking = new Booking(); booking.setId(5); booking.setUser(owner);
        when(bookingRepository.findById(5)).thenReturn(Optional.of(booking));
        byte[] pdfBytes = new byte[]{0x25, 0x50, 0x44, 0x46};
        when(ticketPdfService.generateTicketPdf(booking)).thenReturn(pdfBytes);

        ResponseEntity<?> response = controller.downloadTicketPdf(5, session);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst("Content-Disposition")).contains("ticket-booking-5.pdf");
        assertThat((byte[]) response.getBody()).isEqualTo(pdfBytes);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private FlightSchedule buildMinimalSchedule() {
        Destination from = new Destination(); from.setAirport("SYD");
        Destination to = new Destination(); to.setAirport("MEL");
        Route route = new Route(); route.setFromDestination(from); route.setToDestination(to);
        Airline airline = new Airline(); airline.setName("TestAir");
        Flight flight = new Flight(); flight.setFlightCode("T100"); flight.setAirline(airline);
        FlightSchedule schedule = new FlightSchedule();
        schedule.setId(1); schedule.setRoute(route); schedule.setFlight(flight);
        schedule.setTravelDate(LocalDate.of(2030, 1, 1));
        schedule.setTravelTime(java.time.LocalTime.of(10, 0));
        return schedule;
    }
}

