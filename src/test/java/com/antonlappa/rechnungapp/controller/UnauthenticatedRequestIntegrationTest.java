package com.antonlappa.rechnungapp.controller;

import com.antonlappa.rechnungapp.config.JwtProperties;
import com.antonlappa.rechnungapp.config.JwtService;
import com.antonlappa.rechnungapp.repository.UserRepository;
import com.antonlappa.rechnungapp.repository.entity.UserEntity;
import com.antonlappa.rechnungapp.repository.entity.UserRole;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the 401 handling of unauthenticated requests.
 * <p>
 * Uses the real PostgreSQL database (via Docker Compose) and the full
 * security filter chain.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UnauthenticatedRequestIntegrationTest {

    private static final String PROTECTED_URL = "/api/v1/customers";
    private static final long ONE_HOUR_MS = 3_600_000L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private UserRepository userRepository;

    private String email;

    @BeforeEach
    void setUp() {
        UserEntity user = UserEntity.builder()
                .email("unauth-" + UUID.randomUUID() + "@example.com")
                .passwordHash("$2a$10$encodedPasswordHash")
                .firstName("Test")
                .lastName("User")
                .role(UserRole.USER)
                .build();
        userRepository.save(user);
        email = user.getEmail();
    }

    private String buildExpiredToken() {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date(now - 2 * ONE_HOUR_MS))
                .expiration(new Date(now - ONE_HOUR_MS))
                .signWith(Keys.hmacShaKeyFor(HexFormat.of().parseHex(jwtProperties.getSecret())))
                .compact();
    }

    private void expectUnauthorizedJson(ResultActions result) throws Exception {
        result.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").isString())
                .andExpect(jsonPath("$.timestamp").isString());
    }

    @Test
    @DisplayName("returns 401 JSON when no token is sent")
    void missingToken() throws Exception {
        expectUnauthorizedJson(mockMvc.perform(get(PROTECTED_URL)));
    }

    @Test
    @DisplayName("returns 401 JSON when the token is expired")
    void expiredToken() throws Exception {
        expectUnauthorizedJson(mockMvc.perform(get(PROTECTED_URL)
                .header("Authorization", "Bearer " + buildExpiredToken())));
    }

    @Test
    @DisplayName("returns 401 JSON when the token is malformed")
    void malformedToken() throws Exception {
        expectUnauthorizedJson(mockMvc.perform(get(PROTECTED_URL)
                .header("Authorization", "Bearer not-a-jwt")));
    }

    @Test
    @DisplayName("returns 401 JSON when the token signature is invalid")
    void tamperedToken() throws Exception {
        String token = jwtService.generateToken(new User(email, "x", List.of()));
        String tampered = token.substring(0, token.length() - 4) + "AAAA";

        expectUnauthorizedJson(mockMvc.perform(get(PROTECTED_URL)
                .header("Authorization", "Bearer " + tampered)));
    }

    @Test
    @DisplayName("accepts a valid token on a protected endpoint")
    void validToken() throws Exception {
        String token = jwtService.generateToken(new User(email, "x", List.of()));

        mockMvc.perform(get(PROTECTED_URL).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("leaves public endpoints reachable without a token")
    void publicEndpointUnaffected() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
