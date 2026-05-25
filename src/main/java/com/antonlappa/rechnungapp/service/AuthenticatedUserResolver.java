package com.antonlappa.rechnungapp.service;

import com.antonlappa.rechnungapp.repository.UserRepository;
import com.antonlappa.rechnungapp.repository.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Helper component to resolve the authenticated user's ID from the security context.
 */
@Component
@RequiredArgsConstructor
public class AuthenticatedUserResolver {

    private final UserRepository userRepository;

    /**
     * Resolves the authenticated user's UUID from the security principal.
     * The principal's username is the user's email.
     *
     * @param userDetails the authenticated user details
     * @return the user's UUID
     * @throws IllegalStateException if the user is not found in the database
     */
    public UUID resolveUserId(UserDetails userDetails) {
        UserEntity user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found in database"));
        return user.getId();
    }
}
