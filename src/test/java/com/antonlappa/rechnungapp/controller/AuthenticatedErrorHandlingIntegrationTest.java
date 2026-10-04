package com.antonlappa.rechnungapp.controller;

import com.antonlappa.rechnungapp.config.JwtService;
import com.antonlappa.rechnungapp.exception.ResourceNotFoundException;
import com.antonlappa.rechnungapp.repository.UserRepository;
import com.antonlappa.rechnungapp.repository.entity.UserEntity;
import com.antonlappa.rechnungapp.repository.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for errors raised after successful JWT authentication.
 * <p>
 * Uses a test-only controller (imported explicitly, not component-scanned)
 * and the full security filter chain with a real JWT.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(AuthenticatedErrorHandlingIntegrationTest.TestErrorController.class)
class AuthenticatedErrorHandlingIntegrationTest {

    private static final String DENIED_URL = "/test-only/denied";
    private static final String NOT_FOUND_URL = "/test-only/not-found";

    @RestController
    static class TestErrorController {

        @PreAuthorize("denyAll()")
        @GetMapping(DENIED_URL)
        String denied() {
            return "unreachable";
        }

        @GetMapping(NOT_FOUND_URL)
        String notFound() {
            throw new ResourceNotFoundException("Test resource not found");
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    private String bearerToken;

    @BeforeEach
    void setUp() {
        UserEntity user = UserEntity.builder()
                .email("auth-errors-" + UUID.randomUUID() + "@example.com")
                .passwordHash("$2a$10$encodedPasswordHash")
                .firstName("Test")
                .lastName("User")
                .role(UserRole.USER)
                .build();
        userRepository.save(user);

        bearerToken = "Bearer " + jwtService.generateToken(
                new User(user.getEmail(), user.getPasswordHash(), List.of()));
    }

    @Test
    @DisplayName("returns 403 JSON when an authenticated user is denied by @PreAuthorize")
    void accessDenied() throws Exception {
        mockMvc.perform(get(DENIED_URL).header("Authorization", bearerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Access denied"))
                .andExpect(jsonPath("$.timestamp").isString());
    }

    @Test
    @DisplayName("handles a controller exception after authentication by its normal handler")
    void controllerExceptionKeepsItsHandler() throws Exception {
        mockMvc.perform(get(NOT_FOUND_URL).header("Authorization", bearerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Test resource not found"));
    }
}
