package com.antonlappa.rechnungapp.repository;

import com.antonlappa.rechnungapp.repository.entity.CustomerEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link CustomerEntity} entities.
 */
@Repository
public interface CustomerRepository extends JpaRepository<CustomerEntity, UUID> {

    /**
     * Returns all customers belonging to a specific user,
     * ordered by name ascending.
     *
     * @param userId the user's UUID
     */
    List<CustomerEntity> findAllByUserIdOrderByNameAsc(UUID userId);

    /**
     * Finds a single customer by its ID and owning user ID.
     * Used to enforce data scoping — a user can only access their own customers.
     *
     * @param id     the customer's UUID
     * @param userId the owning user's UUID
     */
    Optional<CustomerEntity> findByIdAndUserId(UUID id, UUID userId);
}
