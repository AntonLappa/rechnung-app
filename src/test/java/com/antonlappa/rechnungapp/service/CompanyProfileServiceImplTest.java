package com.antonlappa.rechnungapp.service;

import com.antonlappa.rechnungapp.exception.ResourceNotFoundException;
import com.antonlappa.rechnungapp.mapper.CompanyProfileMapper;
import com.antonlappa.rechnungapp.repository.CompanyProfileRepository;
import com.antonlappa.rechnungapp.repository.UserRepository;
import com.antonlappa.rechnungapp.repository.entity.CompanyProfileEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CompanyProfileServiceImpl} logo deletion.
 */
@ExtendWith(MockitoExtension.class)
class CompanyProfileServiceImplTest {

    @Mock
    private CompanyProfileRepository companyProfileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CompanyProfileMapper companyProfileMapper;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private CompanyProfileServiceImpl companyProfileService;

    private UUID userId;
    private String logoKey;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        logoKey = "logos/" + userId + "/logo.png";
    }

    private CompanyProfileEntity givenProfileWithLogo(String logoPath) {
        CompanyProfileEntity profile = CompanyProfileEntity.builder()
                .companyName("Testfirma GmbH")
                .logoPath(logoPath)
                .build();
        when(companyProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        return profile;
    }

    @Nested
    @DisplayName("deleteLogo")
    class DeleteLogo {

        @Test
        @DisplayName("clears logoPath, saves, then deletes the S3 object by its key")
        void deletesExistingLogo() {
            CompanyProfileEntity profile = givenProfileWithLogo(logoKey);

            companyProfileService.deleteLogo(userId);

            assertNull(profile.getLogoPath());
            InOrder inOrder = inOrder(companyProfileRepository, storageService);
            inOrder.verify(companyProfileRepository).save(profile);
            inOrder.verify(storageService).delete(logoKey);
        }

        @Test
        @DisplayName("does nothing when no logo is set")
        void noLogoIsNoOp() {
            givenProfileWithLogo(null);

            companyProfileService.deleteLogo(userId);

            verify(companyProfileRepository, never()).save(any());
            verifyNoInteractions(storageService);
        }

        @Test
        @DisplayName("treats a blank logoPath as no logo")
        void blankLogoIsNoOp() {
            givenProfileWithLogo("  ");

            companyProfileService.deleteLogo(userId);

            verify(companyProfileRepository, never()).save(any());
            verifyNoInteractions(storageService);
        }

        @Test
        @DisplayName("succeeds and keeps logoPath null when S3 delete fails")
        void s3FailureDoesNotFailRequest() {
            CompanyProfileEntity profile = givenProfileWithLogo(logoKey);
            doThrow(new RuntimeException("S3 unavailable")).when(storageService).delete(logoKey);

            assertDoesNotThrow(() -> companyProfileService.deleteLogo(userId));

            assertNull(profile.getLogoPath());
            verify(companyProfileRepository).save(profile);
        }

        @Test
        @DisplayName("clears logoPath but skips S3 delete for a key outside the user's prefix")
        void foreignKeyIsNotDeletedFromS3() {
            CompanyProfileEntity profile = givenProfileWithLogo("logos/" + UUID.randomUUID() + "/logo.png");

            companyProfileService.deleteLogo(userId);

            assertNull(profile.getLogoPath());
            verify(companyProfileRepository).save(profile);
            verify(storageService, never()).delete(anyString());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when the user has no profile")
        void missingProfile() {
            when(companyProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> companyProfileService.deleteLogo(userId));
            verifyNoInteractions(storageService);
        }
    }
}
