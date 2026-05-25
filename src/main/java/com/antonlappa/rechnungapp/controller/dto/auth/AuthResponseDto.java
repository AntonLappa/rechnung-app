package com.antonlappa.rechnungapp.controller.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO returned after successful registration or login.
 * <p>
 * Contains the JWT token the client must send as a
 * {@code Bearer} token in subsequent requests.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponseDto {

    private String token;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
}
