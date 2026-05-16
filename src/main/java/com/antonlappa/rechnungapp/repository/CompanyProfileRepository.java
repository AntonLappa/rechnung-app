package com.antonlappa.rechnungapp.repository;

import com.antonlappa.rechnungapp.repository.entity.CompanyProfile;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link CompanyProfile} entities.
 */
@Repository
public interface CompanyProfileRepository extends JpaRepository<CompanyProfile, UUID> {

    /**
     * Finds the company profile belonging to a specific user.
     *
     * @param userId the user's UUID
     * @return the profile, if it exists
     */
    Optional<CompanyProfile> findByUserId(UUID userId);

    /**
     * Checks whether a given user already has a company profile.
     *
     * @param userId the user's UUID
     * @return true if a profile exists
     */
    boolean existsByUserId(UUID userId);
}
