package com.safelight.controller;

import com.safelight.dto.LoginRequest;
import com.safelight.dto.SignupRequest;
import com.safelight.dto.UserResponse;
import com.safelight.model.User;
import com.safelight.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpSession session;

    @InjectMocks
    private AuthController authController;

    @Test
    void signupShouldReturnConflictWhenEmailAlreadyRegistered() {
        SignupRequest request = buildSignupRequest();
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(buildUser(1, request.getEmail(), "secret")));

        ResponseEntity<?> response = authController.signup(request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Email is already registered", response.getBody());
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(User.class));
    }

    @Test
    void signupShouldCreateUserWhenEmailIsNew() {
        SignupRequest request = buildSignupRequest();
        User savedUser = buildUser(12, request.getEmail(), request.getPassword());
        savedUser.setFname(request.getFname());
        savedUser.setLname(request.getLname());
        savedUser.setPhone(request.getPhone());
        savedUser.setRole("USER");

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class))).thenReturn(savedUser);

        ResponseEntity<?> response = authController.signup(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        UserResponse body = assertInstanceOf(UserResponse.class, response.getBody());
        assertEquals(12, body.getId());
        assertEquals(request.getEmail(), body.getEmail());
        assertEquals("USER", body.getRole());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User toSave = userCaptor.getValue();
        assertEquals(request.getFname(), toSave.getFname());
        assertEquals(request.getLname(), toSave.getLname());
        assertEquals(request.getEmail(), toSave.getEmail());
        assertEquals(request.getPassword(), toSave.getPassword());
        assertEquals(request.getPhone(), toSave.getPhone());
        assertEquals(request.getDob(), toSave.getDob());
        assertEquals(request.getCountry(), toSave.getCountry());
        assertEquals("USER", toSave.getRole());
    }

    @Test
    void loginShouldReturnUnauthorizedWhenUserNotFound() {
        LoginRequest request = new LoginRequest();
        request.setEmail("missing@example.com");
        request.setPassword("secret");
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

        ResponseEntity<?> response = authController.login(request, session);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Invalid credentials", response.getBody());
        verify(session, never()).setAttribute(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void loginShouldReturnUnauthorizedWhenPasswordIsWrong() {
        LoginRequest request = new LoginRequest();
        request.setEmail("john@example.com");
        request.setPassword("wrong");
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(buildUser(3, request.getEmail(), "correct")));

        ResponseEntity<?> response = authController.login(request, session);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Invalid credentials", response.getBody());
        verify(session, never()).setAttribute(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void loginShouldSetSessionAndReturnUserWhenCredentialsAreValid() {
        LoginRequest request = new LoginRequest();
        request.setEmail("john@example.com");
        request.setPassword("correct");
        User user = buildUser(7, request.getEmail(), "correct");
        user.setFname("John");
        user.setLname("Doe");
        user.setPhone("1234567890");
        user.setRole("USER");
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));

        ResponseEntity<?> response = authController.login(request, session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        UserResponse body = assertInstanceOf(UserResponse.class, response.getBody());
        assertEquals(7, body.getId());
        assertEquals("john@example.com", body.getEmail());
        verify(session).setAttribute("USER_ID", 7);
    }

    @Test
    void currentUserShouldReturnUnauthorizedWhenNoSessionUserId() {
        when(session.getAttribute("USER_ID")).thenReturn(null);

        ResponseEntity<?> response = authController.currentUser(session);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Not logged in", response.getBody());
    }

    @Test
    void currentUserShouldReturnUnauthorizedWhenSessionUserIdIsNotInteger() {
        when(session.getAttribute("USER_ID")).thenReturn("7");

        ResponseEntity<?> response = authController.currentUser(session);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Not logged in", response.getBody());
    }

    @Test
    void currentUserShouldReturnUnauthorizedWhenUserNotFound() {
        when(session.getAttribute("USER_ID")).thenReturn(404);
        when(userRepository.findById(404)).thenReturn(Optional.empty());

        ResponseEntity<?> response = authController.currentUser(session);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Not logged in", response.getBody());
    }

    @Test
    void currentUserShouldReturnUserWhenSessionAndUserAreValid() {
        User user = buildUser(11, "active@example.com", "secret");
        user.setFname("Active");
        user.setLname("User");
        user.setPhone("5551234");
        user.setRole("USER");
        when(session.getAttribute("USER_ID")).thenReturn(11);
        when(userRepository.findById(11)).thenReturn(Optional.of(user));

        ResponseEntity<?> response = authController.currentUser(session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        UserResponse body = assertInstanceOf(UserResponse.class, response.getBody());
        assertEquals(11, body.getId());
        assertEquals("active@example.com", body.getEmail());
        assertEquals("USER", body.getRole());
        assertNull(user.getCountry());
    }

    @Test
    void logoutShouldInvalidateSessionAndReturnNoContent() {
        ResponseEntity<Void> response = authController.logout(session);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(session).invalidate();
    }

    private SignupRequest buildSignupRequest() {
        SignupRequest request = new SignupRequest();
        request.setFname("Jane");
        request.setLname("Doe");
        request.setEmail("jane@example.com");
        request.setPassword("secret");
        request.setPhone("1234567890");
        request.setDob(LocalDate.of(1995, 3, 10));
        request.setCountry("USA");
        return request;
    }

    // AUTH-U-04: signup with null DOB → toResponse dob branch returns null
    @Test
    void signupWithNullDobShouldReturnNullDobInResponse() {
        SignupRequest request = buildSignupRequest();
        request.setDob(null);
        User savedUser = buildUser(20, request.getEmail(), request.getPassword());
        savedUser.setRole("USER");
        savedUser.setDob(null);

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class))).thenReturn(savedUser);

        ResponseEntity<?> response = authController.signup(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        UserResponse body = assertInstanceOf(UserResponse.class, response.getBody());
        assertNull(body.getDob());
    }

    // AUTH-U-13: updateProfile with no session → 401
    @Test
    void updateProfileShouldReturnUnauthorizedWhenNoSession() {
        when(session.getAttribute("USER_ID")).thenReturn(null);

        com.safelight.dto.UpdateProfileRequest req = new com.safelight.dto.UpdateProfileRequest();
        req.setFname("New");

        ResponseEntity<?> response = authController.updateProfile(req, session);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Not logged in", response.getBody());
    }

    // AUTH-U-14: updateProfile with valid session but user not in DB → 401
    @Test
    void updateProfileShouldReturnUnauthorizedWhenUserNotFound() {
        when(session.getAttribute("USER_ID")).thenReturn(5);
        when(userRepository.findById(5)).thenReturn(Optional.empty());

        com.safelight.dto.UpdateProfileRequest req = new com.safelight.dto.UpdateProfileRequest();
        req.setFname("New");

        ResponseEntity<?> response = authController.updateProfile(req, session);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Not logged in", response.getBody());
    }

    // AUTH-U-15: updateProfile all fields non-null → all updated
    @Test
    void updateProfileShouldUpdateAllFieldsWhenAllNonNull() {
        User existing = buildUser(10, "user@test.com", "pass");
        existing.setFname("Old");
        existing.setLname("Name");
        existing.setPhone("000");
        existing.setCountry("UK");
        existing.setDob(LocalDate.of(1980, 1, 1));

        when(session.getAttribute("USER_ID")).thenReturn(10);
        when(userRepository.findById(10)).thenReturn(Optional.of(existing));
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class)))
                .thenAnswer(inv -> inv.getArgument(0, User.class));

        com.safelight.dto.UpdateProfileRequest req = new com.safelight.dto.UpdateProfileRequest();
        req.setFname("New");
        req.setLname("Last");
        req.setPhone("999");
        req.setCountry("AU");
        req.setDob("1990-06-15");

        ResponseEntity<?> response = authController.updateProfile(req, session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        UserResponse body = assertInstanceOf(UserResponse.class, response.getBody());
        assertEquals("New", body.getFname());
        assertEquals("Last", body.getLname());
        assertEquals("999", body.getPhone());
        assertEquals("AU", body.getCountry());
        assertEquals("1990-06-15", body.getDob());
    }

    // AUTH-U-16/17/18/19: null fields are NOT applied
    @Test
    void updateProfileShouldNotOverwriteFieldsWhenRequestFieldsAreNull() {
        User existing = buildUser(11, "u@test.com", "pass");
        existing.setFname("Keep");
        existing.setLname("This");
        existing.setPhone("111");
        existing.setCountry("US");
        existing.setDob(LocalDate.of(1985, 5, 5));

        when(session.getAttribute("USER_ID")).thenReturn(11);
        when(userRepository.findById(11)).thenReturn(Optional.of(existing));
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class)))
                .thenAnswer(inv -> inv.getArgument(0, User.class));

        com.safelight.dto.UpdateProfileRequest req = new com.safelight.dto.UpdateProfileRequest();
        // all fields null — nothing should change

        ResponseEntity<?> response = authController.updateProfile(req, session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertEquals("Keep", saved.getFname());
        assertEquals("This", saved.getLname());
        assertEquals("111", saved.getPhone());
        assertEquals("US", saved.getCountry());
        assertEquals(LocalDate.of(1985, 5, 5), saved.getDob());
    }

    // AUTH-U-21: dob invalid format → dob unchanged
    @Test
    void updateProfileShouldLeavedobUnchangedWhenDobIsInvalidFormat() {
        User existing = buildUser(12, "u@test.com", "pass");
        existing.setDob(LocalDate.of(1990, 1, 1));

        when(session.getAttribute("USER_ID")).thenReturn(12);
        when(userRepository.findById(12)).thenReturn(Optional.of(existing));
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class)))
                .thenAnswer(inv -> inv.getArgument(0, User.class));

        com.safelight.dto.UpdateProfileRequest req = new com.safelight.dto.UpdateProfileRequest();
        req.setDob("not-a-date");

        authController.updateProfile(req, session);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(LocalDate.of(1990, 1, 1), captor.getValue().getDob());
    }

    // AUTH-U-22: dob blank → user.dob set to null
    @Test
    void updateProfileShouldSetDobToNullWhenDobIsBlank() {
        User existing = buildUser(13, "u@test.com", "pass");
        existing.setDob(LocalDate.of(1990, 1, 1));

        when(session.getAttribute("USER_ID")).thenReturn(13);
        when(userRepository.findById(13)).thenReturn(Optional.of(existing));
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class)))
                .thenAnswer(inv -> inv.getArgument(0, User.class));

        com.safelight.dto.UpdateProfileRequest req = new com.safelight.dto.UpdateProfileRequest();
        req.setDob("   ");

        authController.updateProfile(req, session);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertNull(captor.getValue().getDob());
    }

    // AUTH-U-23: dob null → neither branch taken, dob unchanged
    @Test
    void updateProfileShouldLeavedobUnchangedWhenDobIsNull() {
        User existing = buildUser(14, "u@test.com", "pass");
        existing.setDob(LocalDate.of(1992, 3, 3));

        when(session.getAttribute("USER_ID")).thenReturn(14);
        when(userRepository.findById(14)).thenReturn(Optional.of(existing));
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class)))
                .thenAnswer(inv -> inv.getArgument(0, User.class));

        com.safelight.dto.UpdateProfileRequest req = new com.safelight.dto.UpdateProfileRequest();
        req.setDob(null);

        authController.updateProfile(req, session);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(LocalDate.of(1992, 3, 3), captor.getValue().getDob());
    }

    private User buildUser(int id, String email, String password) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setPassword(password);
        return user;
    }
}
