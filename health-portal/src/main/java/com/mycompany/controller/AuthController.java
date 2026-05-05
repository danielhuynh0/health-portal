package com.mycompany.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.mycompany.DTO.UserDTO;
import com.mycompany.models.AppUser;
import com.mycompany.models.TokenResponse;
import com.mycompany.repo.AppUserRepository;
import com.mycompany.security.JwtUtil;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final JwtUtil jwtUtil;
    private final AppUserRepository userRepo;

    @Value("${jwt.expiration:3600000}")
    private long expirationMs;

    public AuthController(JwtUtil jwtUtil, AppUserRepository userRepo) {
        this.jwtUtil = jwtUtil;
        this.userRepo = userRepo;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody UserDTO userDTO) {
        if (userDTO == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body cannot be null");
        }

        String username = userDTO.getUsername();
        String password = userDTO.getPassword();

        if (username == null || username.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username is required");
        }
        if (password == null || password.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");
        }

        Optional<AppUser> userOpt = userRepo.findByUsername(username);

        if (userOpt.isEmpty() || !userOpt.get().getPassword().equals(password)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        String token = jwtUtil.generateToken(username);
        long expiresIn = expirationMs / 1000;
        return ResponseEntity.ok(new TokenResponse(token, expiresIn));
    }
}
