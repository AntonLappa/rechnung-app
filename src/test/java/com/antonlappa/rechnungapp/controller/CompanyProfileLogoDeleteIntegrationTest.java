package com.antonlappa.rechnungapp.controller;

import com.antonlappa.rechnungapp.config.JwtService;
import com.antonlappa.rechnungapp.repository.CompanyProfileRepository;
import com.antonlappa.rechnungapp.repository.UserRepository;
import com.antonlappa.rechnungapp.repository.entity.CompanyProfileEntity;
import com.antonlappa.rechnungapp.repository.entity.UserEntity;
import com.antonlappa.rechnungapp.repository.entity.UserRole;
import com.antonlappa.rechnungapp.service.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@code DELETE /api/v1/company-profile/logo}.
 * <p>
 * Uses the real PostgreSQL database (via Docker Compose) and the full
 * security filter chain with a real JWT. S3 is replaced by a mock.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CompanyProfileLogoDeleteIntegrationTest {

    private static final String LOGO_URL = "/api/v1/company-profile/logo";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyProfileRepository companyProfileRepository;

    @MockitoBean
    private StorageService storageService;

    private UUID userId;
    private String bearerToken;
    private String logoKey;

    @BeforeEach
    void setUp() {
        UserEntity user = UserEntity.builder()
                .email("logo-delete-" + UUID.randomUUID() + "@example.com")
                .passwordHash("$2a$10$encodedPasswordHash")
                .firstName("Test")
                .lastName("User")
                .role(UserRole.USER)
                .build();
        userRepository.save(user);
        userId = user.getId();
        logoKey = "logos/" + userId + "/logo.png";

        bearerToken = "Bearer " + jwtService.generateToken(
                new User(user.getEmail(), user.getPasswordHash(), List.of()));
    }

    private void givenProfileWithLogo(String logoPath) {
        companyProfileRepository.save(CompanyProfileEntity.builder()
                .user(userRepository.getReferenceById(userId))
                .companyName("Testfirma GmbH")
                .ownerName("Max Mustermann")
                .address("Musterstraße 1\n12345 Berlin")
                .smallBusiness(false)
                .logoPath(logoPath)
                .build());
    }

    private String storedLogoPath() {
        return companyProfileRepository.findByUserId(userId).orElseThrow().getLogoPath();
    }

    @Test
    @DisplayName("returns 204, clears logoPath and deletes the S3 object")
    void deletesExistingLogo() throws Exception {
        givenProfileWithLogo(logoKey);

        mockMvc.perform(delete(LOGO_URL).header("Authorization", bearerToken))
                .andExpect(status().isNoContent());

        assertNull(storedLogoPath());
        verify(storageService).delete(logoKey);
    }

    @Test
    @DisplayName("returns 204 without touching S3 when no logo is set")
    void noLogoIsIdempotent() throws Exception {
        givenProfileWithLogo(null);

        mockMvc.perform(delete(LOGO_URL).header("Authorization", bearerToken))
                .andExpect(status().isNoContent());

        assertNull(storedLogoPath());
        verify(storageService, never()).delete(anyString());
    }

    @Test
    @DisplayName("returns 204 and clears logoPath even when the S3 delete fails")
    void s3FailureStillSucceeds() throws Exception {
        givenProfileWithLogo(logoKey);
        doThrow(new RuntimeException("S3 unavailable")).when(storageService).delete(logoKey);

        mockMvc.perform(delete(LOGO_URL).header("Authorization", bearerToken))
                .andExpect(status().isNoContent());

        assertNull(storedLogoPath());
    }

    @Test
    @DisplayName("rejects unauthenticated requests and leaves the logo untouched")
    void rejectsUnauthenticated() throws Exception {
        givenProfileWithLogo(logoKey);

        mockMvc.perform(delete(LOGO_URL))
                .andExpect(status().isForbidden());

        assertEquals(logoKey, storedLogoPath());
        verifyNoInteractions(storageService);
    }
}
