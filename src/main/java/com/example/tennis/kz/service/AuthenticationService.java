package com.example.tennis.kz.service;

import com.example.tennis.kz.exception.BadRequestException; // Import your custom exception
import com.example.tennis.kz.model.Role;
import com.example.tennis.kz.model.User;
import com.example.tennis.kz.model.UserInfo;
import com.example.tennis.kz.model.request.SignInRequest;
import com.example.tennis.kz.model.request.SignUpRequest;
import com.example.tennis.kz.model.response.JwtAuthenticationResponse;
import com.example.tennis.kz.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
// Consider importing specific authentication exceptions if you plan to catch them here,
// otherwise, they will be handled by Spring Security defaults or a generic handler.
// import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.NoSuchElementException; // Keep this if userService.findUserByEmail can throw it

@Service
@RequiredArgsConstructor
@Transactional // Added @Transactional here as it was in your prompt, assumed it's class-level
public class AuthenticationService {
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenRepository refreshTokenRepository;

    public JwtAuthenticationResponse signup(SignUpRequest request) {
        // Validate if email already exists
        if (userService.existsByEmail(request.getEmail())) {
            // Using BadRequestException for a clear client error (400)
            throw new BadRequestException("Пользователь с таким email уже существует.");
        }

        // Validate request parameters (example, can be more extensive or use @Valid)
        if (request.getFirstName() == null || request.getFirstName().trim().isEmpty()) {
            throw new BadRequestException("Имя не может быть пустым.");
        }
        if (request.getLastName() == null || request.getLastName().trim().isEmpty()) {
            throw new BadRequestException("Фамилия не может быть пустой.");
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) { // Though existsByEmail implies it's not empty
            throw new BadRequestException("Email не может быть пустым.");
        }
        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            throw new BadRequestException("Пароль не может быть пустым.");
        }
        // Add more password policy checks if needed, e.g., length, complexity
         if (request.getPassword().length() < 8) {
         throw new BadRequestException("Пароль должен содержать не менее 8 символов.");
         }
        if (request.getRole() == null) {
            // Assuming Role.USER is a default or you require it to be specified
            throw new BadRequestException("Роль пользователя должна быть указана.");
        }


        var user = User.builder()
                .userInfo(UserInfo.builder()
                        .firstName(request.getFirstName())
                        .lastName(request.getLastName())
                        .build())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole()) // Use the role from the request
                .createdAt(LocalDateTime.now())
                .enabled(true) // Defaulting to true, confirmation flow might change this
                .build();

        userService.saveUser(user); // This might throw NonUniqueObjectException from your UserService, ensure GlobalExceptionHandler covers it or saveUser handles it.

        return JwtAuthenticationResponse.builder()
                .accessToken(jwtService.generateAccessToken(user))
                .refreshToken(jwtService.generateRefreshToken(user).getRefreshToken())
                .build();
    }

    public JwtAuthenticationResponse signin(SignInRequest request) {
        // Basic validation for SignInRequest
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new BadRequestException("Email не может быть пустым для входа.");
        }
        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            throw new BadRequestException("Пароль не может быть пустым для входа.");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        } catch (org.springframework.security.core.AuthenticationException e) {
            // Intercept Spring Security's AuthenticationException (like BadCredentialsException)
            // to return a response consistent with BadRequestException.
            // Otherwise, Spring Security might return a 401 or 403 with a different format.
            throw new BadRequestException("Неверный email или пароль.");
        }

        // If authentication is successful, user is guaranteed to be found by email normally.
        // userService.findUserByEmail already throws NoSuchElementException if not found,
        // which GlobalExceptionHandler maps to 404. This is acceptable.
        // If it throws here after successful authentication, it implies an inconsistency.
        var user = userService.findUserByEmail(request.getEmail());

        // It's good practice to invalidate old refresh tokens or manage them.
        // Assuming one active refresh token per user.
        refreshTokenRepository.deleteById(user.getId()); // Changed to delete by user ID for clarity

        return JwtAuthenticationResponse.builder()
                .accessToken(jwtService.generateAccessToken(user))
                .refreshToken(jwtService.generateRefreshToken(user).getRefreshToken())
                .build();
    }

    public JwtAuthenticationResponse refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new BadRequestException("Refresh token не может быть пустым.");
        }

        User user = refreshTokenRepository.findByRefreshToken(refreshToken)
                .map(rt -> {
                    // Optional: Check token expiry here if not handled by JwtService on validation
                    // if (rt.getExpiryDate().isBefore(LocalDateTime.now())) {
                    // refreshTokenRepository.delete(rt);
                    // throw new BadRequestException("Refresh token истек.");
                    // }
                    return rt.getUser();
                })
                .orElseThrow(() -> new BadRequestException("Некорректный или истекший refresh token.")); // Changed from NoSuchElementException

        // Delete the used refresh token to prevent reuse (common practice for security)
        refreshTokenRepository.deleteByRefreshToken(refreshToken);

        return JwtAuthenticationResponse.builder()
                .accessToken(jwtService.generateAccessToken(user))
                .refreshToken(jwtService.generateRefreshToken(user).getRefreshToken()) // Generate a new refresh token
                .build();
    }
}