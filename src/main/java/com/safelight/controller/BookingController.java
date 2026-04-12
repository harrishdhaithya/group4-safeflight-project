package com.safelight.controller;

import com.safelight.dto.BookingSummaryResponse;
import com.safelight.dto.CreateBookingRequest;
import com.safelight.dto.PassengerBookingDto;
import com.safelight.model.*;
import com.safelight.repository.*;
import com.safelight.service.TicketPdfService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private static final String SESSION_USER_ID = "USER_ID";
    private static final String NOT_LOGGED_IN = "Not logged in";

    private final UserRepository userRepository;
    private final FlightScheduleRepository flightScheduleRepository;
    private final BookingRepository bookingRepository;
    private final PassengerRepository passengerRepository;
    private final BookingPassengerRepository bookingPassengerRepository;
    private final TicketPdfService ticketPdfService;

    public BookingController(UserRepository userRepository,
                             FlightScheduleRepository flightScheduleRepository,
                             BookingRepository bookingRepository,
                             PassengerRepository passengerRepository,
                             BookingPassengerRepository bookingPassengerRepository,
                             TicketPdfService ticketPdfService) {
        this.userRepository = userRepository;
        this.flightScheduleRepository = flightScheduleRepository;
        this.bookingRepository = bookingRepository;
        this.passengerRepository = passengerRepository;
        this.bookingPassengerRepository = bookingPassengerRepository;
        this.ticketPdfService = ticketPdfService;
    }

    @GetMapping("/me")
    public ResponseEntity<?> listMyBookings(HttpSession session) {
        Object idAttr = session.getAttribute(SESSION_USER_ID);
        if (!(idAttr instanceof Integer)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(NOT_LOGGED_IN);
        }
        Integer userId = (Integer) idAttr;

        List<Booking> bookings = bookingRepository.findByUser_IdOrderByBookedTimeDesc(userId);
        List<BookingSummaryResponse> result = bookings.stream().map(b -> {
            BookingSummaryResponse dto = new BookingSummaryResponse();
            dto.setBookingId(b.getId());
            dto.setStatus(b.getStatus());
            dto.setPaymentStatus(b.getPaymentStatus());
            FlightSchedule s = b.getFlightSchedule();
            dto.setFlightCode(s.getFlight().getFlightCode());
            dto.setAirlineName(s.getFlight().getAirline().getName());
            dto.setFromAirport(s.getRoute().getFromDestination().getAirport());
            dto.setToAirport(s.getRoute().getToDestination().getAirport());
            dto.setTravelDate(s.getTravelDate());
            dto.setTravelTime(s.getTravelTime());
            List<String> seats = b.getBookingPassengers().stream()
                    .map(BookingPassenger::getSeatNo)
                    .filter(Objects::nonNull)
                    .sorted()
                    .toList();
            dto.setSeats(seats);
            return dto;
        }).toList();

        return ResponseEntity.ok(result);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> createBooking(@RequestBody CreateBookingRequest request, HttpSession session) {
        Object idAttr = session.getAttribute(SESSION_USER_ID);
        if (!(idAttr instanceof Integer)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(NOT_LOGGED_IN);
        }
        Integer userId = (Integer) idAttr;

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(NOT_LOGGED_IN);
        }

        if (request == null || request.getScheduleId() == null ||
                request.getSeats() == null || request.getSeats().isEmpty()) {
            return ResponseEntity.badRequest().body("Missing scheduleId or seats");
        }

        Optional<FlightSchedule> scheduleOpt = flightScheduleRepository.findById(request.getScheduleId());
        if (scheduleOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid schedule");
        }
        FlightSchedule schedule = scheduleOpt.get();

        // Gather already booked seats for this schedule (only confirmed + paid bookings)
        List<BookingPassenger> existing = bookingPassengerRepository
                .findByBooking_FlightSchedule_Id(schedule.getId());
        Set<String> takenSeats = existing.stream()
                .filter(bp -> {
                    Booking b = bp.getBooking();
                    return b.getStatus() != null &&
                           b.getStatus().equalsIgnoreCase("CONFIRMED") &&
                           (b.getPaymentStatus() == null || b.getPaymentStatus().equalsIgnoreCase("SUCCESS"));
                })
                .map(BookingPassenger::getSeatNo)
                .filter(Objects::nonNull)
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

        // Check requested seats for duplicates and conflicts
        Set<String> requestedSeats = new HashSet<>();
        for (CreateBookingRequest.SeatRequest seat : request.getSeats()) {
            if (seat.getSeatNo() == null || seat.getSeatNo().isBlank()) {
                return ResponseEntity.badRequest().body("Seat number is required");
            }
            String normalized = seat.getSeatNo().toUpperCase(Locale.ROOT);
            if (!requestedSeats.add(normalized)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Duplicate seat in request: " + normalized);
            }
            if (takenSeats.contains(normalized)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Seat already booked: " + normalized);
            }
        }

        Booking booking = new Booking();
        booking.setUser(userOpt.get());
        booking.setFlightSchedule(schedule);
        booking.setStatus("CONFIRMED");
        booking.setPaymentStatus("SUCCESS");
        booking.setBookedTime(LocalDateTime.now());
        booking = bookingRepository.save(booking);

        List<String> bookedSeatList = new ArrayList<>();

        for (CreateBookingRequest.SeatRequest seat : request.getSeats()) {
            Passenger passenger = new Passenger();
            passenger.setFname(seat.getFname());
            passenger.setLname(seat.getLname());
            if (seat.getDob() != null && !seat.getDob().isBlank()) {
                try {
                    passenger.setDob(LocalDate.parse(seat.getDob()));
                } catch (Exception _) {
                    passenger.setDob(null);
                }
            }
            passenger.setPhone(seat.getPhone());
            passenger.setEmail(seat.getEmail());
            passenger.setPassport(seat.getPassport());
            passenger = passengerRepository.save(passenger);

            BookingPassenger bp = new BookingPassenger();
            bp.setBooking(booking);
            bp.setPassenger(passenger);
            bp.setSeatNo(seat.getSeatNo().toUpperCase(Locale.ROOT));
            bp.setSeatType(null);
            Integer baggageQty = seat.getBaggageQuantity();
            if (baggageQty == null || baggageQty < 0) {
                baggageQty = 0;
            }
            bp.setBaggageQuantity(baggageQty);
            bookingPassengerRepository.save(bp);

            bookedSeatList.add(bp.getSeatNo());
        }

        List<PassengerBookingDto> passengerDtos = new ArrayList<>();
        for (CreateBookingRequest.SeatRequest seat : request.getSeats()) {
            PassengerBookingDto pd = new PassengerBookingDto();
            pd.setSeatNo(seat.getSeatNo().toUpperCase(Locale.ROOT));
            pd.setFname(seat.getFname());
            pd.setLname(seat.getLname());
            pd.setPhone(seat.getPhone());
            pd.setEmail(seat.getEmail());
            pd.setPassport(seat.getPassport());
            if (seat.getDob() != null && !seat.getDob().isBlank()) {
                try {
                    pd.setDob(LocalDate.parse(seat.getDob()));
                } catch (Exception _) {
                    pd.setDob(null);
                }
            }
            passengerDtos.add(pd);
        }

        BookingSummaryResponse summary = new BookingSummaryResponse();
        summary.setBookingId(booking.getId());
        summary.setStatus(booking.getStatus());
        summary.setFlightCode(schedule.getFlight().getFlightCode());
        summary.setAirlineName(schedule.getFlight().getAirline().getName());
        summary.setFromAirport(schedule.getRoute().getFromDestination().getAirport());
        summary.setToAirport(schedule.getRoute().getToDestination().getAirport());
        summary.setTravelDate(schedule.getTravelDate());
        summary.setTravelTime(schedule.getTravelTime());
        summary.setSeats(bookedSeatList);
        summary.setPassengers(passengerDtos);
        summary.setPaymentStatus(booking.getPaymentStatus());

        return ResponseEntity.status(HttpStatus.CREATED).body(summary);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getBooking(@PathVariable Integer id, HttpSession session) {
        Object idAttr = session.getAttribute(SESSION_USER_ID);
        if (!(idAttr instanceof Integer)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(NOT_LOGGED_IN);
        }
        Integer userId = (Integer) idAttr;

        Optional<Booking> bookingOpt = bookingRepository.findById(id);
        if (bookingOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Booking booking = bookingOpt.get();
        if (!booking.getUser().getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        FlightSchedule s = booking.getFlightSchedule();
        List<String> seats = booking.getBookingPassengers().stream()
                .map(BookingPassenger::getSeatNo)
                .filter(Objects::nonNull)
                .sorted()
                .toList();
        List<PassengerBookingDto> passengerDtos = booking.getBookingPassengers().stream()
                .sorted(Comparator.comparing(bp -> bp.getSeatNo() != null ? bp.getSeatNo() : ""))
                .map(bp -> {
                    PassengerBookingDto pd = new PassengerBookingDto();
                    pd.setSeatNo(bp.getSeatNo());
                    if (bp.getPassenger() != null) {
                        Passenger p = bp.getPassenger();
                        pd.setFname(p.getFname());
                        pd.setLname(p.getLname());
                        pd.setDob(p.getDob());
                        pd.setPhone(p.getPhone());
                        pd.setEmail(p.getEmail());
                        pd.setPassport(p.getPassport());
                    }
                    return pd;
                })
                .toList();

        BookingSummaryResponse dto = new BookingSummaryResponse();
        dto.setBookingId(booking.getId());
        dto.setStatus(booking.getStatus());
        dto.setPaymentStatus(booking.getPaymentStatus());
        dto.setFlightCode(s.getFlight().getFlightCode());
        dto.setAirlineName(s.getFlight().getAirline().getName());
        dto.setFromAirport(s.getRoute().getFromDestination().getAirport());
        dto.setToAirport(s.getRoute().getToDestination().getAirport());
        dto.setTravelDate(s.getTravelDate());
        dto.setTravelTime(s.getTravelTime());
        dto.setSeats(seats);
        dto.setPassengers(passengerDtos);

        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<?> recordPayment(@PathVariable Integer id, HttpSession session) {
        Object idAttr = session.getAttribute(SESSION_USER_ID);
        if (!(idAttr instanceof Integer)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(NOT_LOGGED_IN);
        }
        Integer userId = (Integer) idAttr;

        Optional<Booking> bookingOpt = bookingRepository.findById(id);
        if (bookingOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Booking booking = bookingOpt.get();
        if (!booking.getUser().getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        booking.setPaymentStatus("SUCCESS");
        booking.setStatus("CONFIRMED");
        bookingRepository.save(booking);

        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/{id}/ticket/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<?> downloadTicketPdf(@PathVariable Integer id, HttpSession session) {
        Object idAttr = session.getAttribute(SESSION_USER_ID);
        if (!(idAttr instanceof Integer)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(NOT_LOGGED_IN);
        }
        Integer userId = (Integer) idAttr;

        Optional<Booking> bookingOpt = bookingRepository.findById(id);
        if (bookingOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Booking booking = bookingOpt.get();
        if (!booking.getUser().getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            byte[] pdf = ticketPdfService.generateTicketPdf(booking);
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"ticket-booking-" + id + ".pdf\"");
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(pdf.length)
                    .body(pdf);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

