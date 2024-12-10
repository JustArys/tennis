package com.example.tennis.kz.controller;

import com.example.tennis.kz.model.response.JwtAuthenticationResponse;
import com.example.tennis.kz.model.request.*;
import com.example.tennis.kz.service.AuthenticationService;
import com.example.tennis.kz.service.CookieService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    private final AuthenticationService authenticationService;
    private final CookieService cookieService;
    @PostMapping("/signup")
    public ResponseEntity<JwtAuthenticationResponse> signup(HttpServletResponse response,
                                                            @RequestBody SignUpRequest request) {
        JwtAuthenticationResponse jwtAuthenticationResponse = authenticationService.signup(request);
        cookieService.setAuthenticationCookies(response, jwtAuthenticationResponse);
        return ResponseEntity.ok(jwtAuthenticationResponse);
    }

    @PostMapping("/signin")
    public ResponseEntity<JwtAuthenticationResponse> signin(HttpServletResponse response,
                                                            @RequestBody SignInRequest request) {
        JwtAuthenticationResponse jwtAuthenticationResponse = authenticationService.signin(request);
        cookieService.setAuthenticationCookies(response, jwtAuthenticationResponse);
        return ResponseEntity.ok(jwtAuthenticationResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<JwtAuthenticationResponse> refresh(HttpServletResponse response,
                                                             @CookieValue("refreshToken") String refreshToken) {
        JwtAuthenticationResponse jwtAuthenticationResponse = authenticationService.refresh(refreshToken);
        cookieService.setAuthenticationCookies(response, jwtAuthenticationResponse);
        System.out.println(refreshToken);
        return ResponseEntity.ok(jwtAuthenticationResponse);
    }
}