package com.antonlappa.rechnungapp.repository.entity;

import com.antonlappa.rechnungapp.repository.entity.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity representing a company profile.
 * <p>
 * Each authenticated user owns exactly one company profile that stores
 * business information used on invoices (seller data, tax info, banking).
 */
@Entity
@Table(name = "company_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserEntity user;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "owner_name", nullable = false)
    private String ownerName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String address;

    @Column(name = "tax_number", length = 50)
    private String taxNumber;

    @Column(name = "vat_id", length = 50)
    private String vatId;

    @Column(name = "registration_number", length = 50)
    private String registrationNumber;

    @Column(name = "registration_court", length = 100)
    private String registrationCourt;

    @Column(length = 34)
    private String iban;

    @Column(length = 11)
    private String bic;

    @Column
    private String email;

    @Column(length = 50)
    private String phone;

    @Column(name = "logo_path", length = 500)
    private String logoPath;

    @Column(name = "small_business", nullable = false)
    private boolean smallBusiness;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ── Lifecycle callbacks ──────────────────────────────────────────

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
