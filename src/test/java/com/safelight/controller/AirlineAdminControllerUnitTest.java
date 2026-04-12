package com.safelight.controller;

import com.safelight.dto.AirlineRequest;
import com.safelight.model.Airline;
import com.safelight.model.User;
import com.safelight.repository.AirlineRepository;
import com.safelight.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AirlineAdminControllerUnitTest {

    @Mock
    private AirlineRepository airlineRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AirlineAdminController controller;

    // AAC-U-01: admin email not found → 403
    @Test
    void createAirlineShouldReturnForbiddenWhenAdminEmailNotFound() {
        when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.createAirline("ghost@test.com", buildRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isEqualTo("Admin user not found");
        verify(airlineRepository, never()).save(any());
    }

    // AAC-U-02: user found but role is USER → 403
    @Test
    void createAirlineShouldReturnForbiddenWhenUserIsNotAdmin() {
        User user = buildUser("user@test.com", "USER");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        ResponseEntity<?> response = controller.createAirline("user@test.com", buildRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isEqualTo("User is not an admin");
        verify(airlineRepository, never()).save(any());
    }

    // AAC-U-03: role "user" lowercase → 403 (equalsIgnoreCase)
    @Test
    void createAirlineShouldReturnForbiddenWhenRoleIsLowercaseUser() {
        User user = buildUser("u@test.com", "user");
        when(userRepository.findByEmail("u@test.com")).thenReturn(Optional.of(user));

        ResponseEntity<?> response = controller.createAirline("u@test.com", buildRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isEqualTo("User is not an admin");
    }

    // AAC-U-04/05/06: happy path with exact "ADMIN" role
    @Test
    void createAirlineShouldCreateAndReturn201WhenAdminRoleIsExact() {
        User admin = buildUser("admin@test.com", "ADMIN");
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));

        Airline saved = new Airline();
        saved.setName("SafeAir");
        saved.setCountry("AU");
        when(airlineRepository.save(any(Airline.class))).thenReturn(saved);

        AirlineRequest req = buildRequest();
        ResponseEntity<?> response = controller.createAirline("admin@test.com", req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Airline body = (Airline) response.getBody();
        assertThat(body.getName()).isEqualTo("SafeAir");
        assertThat(body.getCountry()).isEqualTo("AU");

        ArgumentCaptor<Airline> captor = ArgumentCaptor.forClass(Airline.class);
        verify(airlineRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("SafeAir");
        assertThat(captor.getValue().getCountry()).isEqualTo("AU");
    }

    // AAC-U-07: role "Admin" mixed case → allowed (equalsIgnoreCase)
    @Test
    void createAirlineShouldAllowMixedCaseAdminRole() {
        User admin = buildUser("a@test.com", "Admin");
        when(userRepository.findByEmail("a@test.com")).thenReturn(Optional.of(admin));

        Airline saved = new Airline(); saved.setName("X"); saved.setCountry("US");
        when(airlineRepository.save(any(Airline.class))).thenReturn(saved);

        ResponseEntity<?> response = controller.createAirline("a@test.com", buildRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private AirlineRequest buildRequest() {
        AirlineRequest req = new AirlineRequest();
        req.setName("SafeAir");
        req.setCountry("AU");
        return req;
    }

    private User buildUser(String email, String role) {
        User u = new User();
        u.setEmail(email);
        u.setRole(role);
        return u;
    }
}
