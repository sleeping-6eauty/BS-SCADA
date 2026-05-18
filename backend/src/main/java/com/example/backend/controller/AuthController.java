package com.example.backend.controller;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.domain.Equipment;
import com.example.backend.domain.User;
import com.example.backend.dto.ApiResponse;
import com.example.backend.dto.AuthRequest;
import com.example.backend.dto.JwtResponse;
import com.example.backend.dto.SignupRequest;
import com.example.backend.dto.UserResponse;
import com.example.backend.security.JwtUtils;
import com.example.backend.security.UserDetailsImpl;
import com.example.backend.service.UserService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserService userService;

    @Autowired
    public AuthController(AuthenticationManager authenticationManager, JwtUtils jwtUtils, UserService userService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@Validated @RequestBody AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String token = jwtUtils.generateJwtToken(userDetails);
        return ResponseEntity.ok(new JwtResponse(token, userDetails.getId(), userDetails.getUsername(), userDetails.getAuthorities().stream()
                .map(Object::toString).collect(Collectors.joining(","))));
    }

    @PostMapping("/signup")
    public ResponseEntity<UserResponse> signup(@Validated @RequestBody SignupRequest request) {
        User user = userService.signup(request);
        UserResponse response = new UserResponse(user.getUserId(), user.getName(), user.getEmail(), user.getRole(), user.getStatus());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        User user = userService.findByEmail(principal.getName());
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        UserResponse response = new UserResponse(user.getUserId(), user.getName(), user.getEmail(), user.getRole(), user.getStatus());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me/equipments")
    public ResponseEntity<ApiResponse<List<Equipment>>> myEquipments(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userService.findByEmail(principal.getName());
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        List<Equipment> equipments = userService.listAssignedEquipments(user.getUserId());
        return ResponseEntity.ok(new ApiResponse<>(true, "My assigned equipments retrieved", equipments));
    }
}
