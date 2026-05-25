package com.antonlappa.rechnungapp.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO returned by the /auth/me endpoint.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeResponse {

    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private boolean companyProfileComplete;
}
