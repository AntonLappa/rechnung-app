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
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests ensuring {@code logoPath} cannot be set by clients via
 * {@code POST/PUT /api/v1/company-profile}. Only the logo upload/delete
 * endpoints may change it.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CompanyProfileLogoPathInputIntegrationTest {

    private static final String PROFILE_URL = "/api/v1/company-profile";

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
    private String foreignKey;

    @BeforeEach
    void setUp() {
        UserEntity user = UserEntity.builder()
                .email("logo-input-" + UUID.randomUUID() + "@example.com")
                .passwordHash("$2a$10$encodedPasswordHash")
                .firstName("Test")
                .lastName("User")
                .role(UserRole.USER)
                .build();
        userRepository.save(user);
        userId = user.getId();
        foreignKey = "logos/" + UUID.randomUUID() + "/logo.png";

        bearerToken = "Bearer " + jwtService.generateToken(
                new User(user.getEmail(), user.getPasswordHash(), List.of()));
    }

    private String profileJsonWithLogoPath(String companyName) {
        return """
                {
                  "companyName": "%s",
                  "ownerName": "Max Mustermann",
                  "address": "Musterstraße 1\\n12345 Berlin",
                  "smallBusiness": false,
                  "logoPath": "%s"
                }
                """.formatted(companyName, foreignKey);
    }

    private CompanyProfileEntity storedProfile() {
        return companyProfileRepository.findByUserId(userId).orElseThrow();
    }

    @Test
    @DisplayName("POST ignores logoPath in the request body")
    void createIgnoresLogoPath() throws Exception {
        mockMvc.perform(post(PROFILE_URL)
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileJsonWithLogoPath("Testfirma GmbH")))
                .andExpect(status().isCreated());

        assertNull(storedProfile().getLogoPath());
        verifyNoInteractions(storageService);
    }

    @Test
    @DisplayName("PUT ignores logoPath in the request body and preserves the existing logo")
    void updateIgnoresLogoPathAndPreservesExistingLogo() throws Exception {
        String ownKey = "logos/" + userId + "/logo.png";
        companyProfileRepository.save(CompanyProfileEntity.builder()
                .user(userRepository.getReferenceById(userId))
                .companyName("Testfirma GmbH")
                .ownerName("Max Mustermann")
                .address("Musterstraße 1\n12345 Berlin")
                .smallBusiness(false)
                .logoPath(ownKey)
                .build());

        mockMvc.perform(put(PROFILE_URL)
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileJsonWithLogoPath("Neue Firma GmbH")))
                .andExpect(status().isOk());

        CompanyProfileEntity profile = storedProfile();
        assertEquals("Neue Firma GmbH", profile.getCompanyName());
        assertEquals(ownKey, profile.getLogoPath());
        verifyNoInteractions(storageService);
    }
}
