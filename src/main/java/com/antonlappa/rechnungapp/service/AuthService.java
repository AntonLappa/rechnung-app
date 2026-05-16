package com.antonlappa.rechnungapp.service;

import com.antonlappa.rechnungapp.controller.dto.AuthResponse;
import com.antonlappa.rechnungapp.controller.dto.LoginRequest;
import com.antonlappa.rechnungapp.controller.dto.RegisterRequest;
import com.antonlappa.rechnungapp.config.JwtService;
import com.antonlappa.rechnungapp.repository.entity.User;
import com.antonlappa.rechnungapp.repository.UserRepository;
import com.antonlappa.rechnungapp.repository.entity.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for user registration and login.
 * <p>
 * Registration creates a new {@link User} with a BCrypt-hashed
 * password and returns a JWT.
 * Login delegates credential verification to Spring Security's
 * {@link AuthenticationManager} and returns a fresh JWT on success.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    /**
     * Registers a new user account.
     *
     * @throws IllegalStateException if the email is already taken.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalStateException("Email is already registered");
        }

        User user = User.builder()
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.USER)
                .build();

        userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);

        return buildAuthResponse(user, token);
    }

    /**
     * Authenticates an existing user and returns a JWT.
     *
     * @throws org.springframework.security.core.AuthenticationException
     *         if credentials are invalid.
     */
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalStateException("User not found"));

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);

        return buildAuthResponse(user, token);
    }

    // ── Private helpers ──────────────────────────────────────────────

    private AuthResponse buildAuthResponse(User user, String token) {
        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .build();
    }
}
