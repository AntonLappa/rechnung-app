package com.antonlappa.rechnungapp.repository;

import com.antonlappa.rechnungapp.repository.entity.InvoiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link InvoiceEntity} entities.
 */
@Repository
public interface InvoiceRepository extends JpaRepository<InvoiceEntity, UUID> {

    /**
     * Returns all invoices belonging to a specific user,
     * ordered by creation date descending (newest first).
     */
    List<InvoiceEntity> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Finds a single invoice by its ID and owning user ID.
     * Used to enforce data scoping.
     */
    Optional<InvoiceEntity> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Counts how many FINAL invoices a user has in a given year.
     * Used to generate sequential invoice numbers (YYYY-NNNN).
     */
    @Query("SELECT COUNT(i) FROM InvoiceEntity i WHERE i.user.id = :userId " +
           "AND i.status = com.antonlappa.rechnungapp.repository.entity.InvoiceStatus.FINAL " +
           "AND YEAR(i.createdAt) = :year")
    long countFinalInvoicesForUserInYear(@Param("userId") UUID userId, @Param("year") int year);
}
