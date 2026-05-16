package com.antonlappa.rechnungapp.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO returned after successful registration or login.
 * <p>
 * Contains the JWT token the client must send as a
 * {@code Bearer} token in subsequent requests.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String token;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
}
