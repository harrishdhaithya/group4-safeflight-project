package com.safelight.controller;

import com.safelight.dto.LoginRequest;
import com.safelight.dto.SignupRequest;
import com.safelight.dto.UpdateProfileRequest;
import com.safelight.dto.UserResponse;
import com.safelight.model.User;
import com.safelight.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String SESSION_USER_ID = "USER_ID";
    private static final String NOT_LOGGED_IN = "Not logged in";

    private final UserRepository userRepository;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest request) {
        Optional<User> existing = userRepository.findByEmail(request.getEmail());
        if (existing.isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email is already registered");
        }

        User user = new User();
        user.setFname(request.getFname());
        user.setLname(request.getLname());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setPhone(request.getPhone());
        user.setDob(request.getDob());
        user.setCountry(request.getCountry());
        user.setRole("USER");

        User saved = userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpSession session) {
        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }

        User user = userOpt.get();
        if (!request.getPassword().equals(user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }

        session.setAttribute(SESSION_USER_ID, user.getId());
        return ResponseEntity.ok(toResponse(user));
    }

    @GetMapping("/me")
    public ResponseEntity<?> currentUser(HttpSession session) {
        Object idAttr = session.getAttribute(SESSION_USER_ID);
        if (!(idAttr instanceof Integer)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(NOT_LOGGED_IN);
        }

        Integer userId = (Integer) idAttr;
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(NOT_LOGGED_IN);
        }

        return ResponseEntity.ok(toResponse(userOpt.get()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateProfile(@RequestBody UpdateProfileRequest request, HttpSession session) {
        Object idAttr = session.getAttribute(SESSION_USER_ID);
        if (!(idAttr instanceof Integer)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(NOT_LOGGED_IN);
        }
        Integer userId = (Integer) idAttr;
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(NOT_LOGGED_IN);
        }
        User user = userOpt.get();
        if (request.getFname() != null) user.setFname(request.getFname());
        if (request.getLname() != null) user.setLname(request.getLname());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getCountry() != null) user.setCountry(request.getCountry());
        if (request.getDob() != null && !request.getDob().isBlank()) {
            try {
                user.setDob(LocalDate.parse(request.getDob()));
            } catch (Exception _) {
                // leave dob unchanged on parse error
            }
        } else if (request.getDob() != null && request.getDob().isBlank()) {
            user.setDob(null);
        }
        user = userRepository.save(user);
        return ResponseEntity.ok(toResponse(user));
    }

    private UserResponse toResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setFname(user.getFname());
        response.setLname(user.getLname());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setDob(user.getDob() != null ? user.getDob().toString() : null);
        response.setCountry(user.getCountry());
        response.setRole(user.getRole());
        return response;
    }
}

