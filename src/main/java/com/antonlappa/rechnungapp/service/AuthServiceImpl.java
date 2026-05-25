package com.antonlappa.rechnungapp.service;

import com.antonlappa.rechnungapp.exception.BusinessRuleException;
import com.antonlappa.rechnungapp.controller.dto.auth.AuthResponseDto;
import com.antonlappa.rechnungapp.controller.dto.auth.LoginRequestDto;
import com.antonlappa.rechnungapp.controller.dto.auth.RegisterRequestDto;
import com.antonlappa.rechnungapp.config.JwtService;
import com.antonlappa.rechnungapp.repository.entity.UserEntity;
import com.antonlappa.rechnungapp.repository.UserRepository;
import com.antonlappa.rechnungapp.repository.entity.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import com.antonlappa.rechnungapp.mapper.AuthMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for user registration and login.
 * <p>
 * Registration creates a new {@link UserEntity} with a BCrypt-hashed
 * password and returns a JWT.
 * Login delegates credential verification to Spring Security's
 * {@link AuthenticationManager} and returns a fresh JWT on success.
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final AuthMapper authMapper;

    /**
     * Registers a new user account.
     *
     * @throws BusinessRuleException if the email is already taken.
     */
    @Transactional
    public AuthResponseDto register(RegisterRequestDto request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessRuleException("Email is already registered");
        }

        UserEntity user = UserEntity.builder()
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.USER)
                .build();

        userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);

        return authMapper.toDto(user, token);
    }

    /**
     * Authenticates an existing user and returns a JWT.
     *
     * @throws org.springframework.security.core.AuthenticationException
     *         if credentials are invalid.
     */
    public AuthResponseDto login(LoginRequestDto request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        UserEntity user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);

        return authMapper.toDto(user, token);
    }


}
