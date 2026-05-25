package com.antonlappa.rechnungapp.service;

import com.antonlappa.rechnungapp.controller.dto.MeResponse;
import com.antonlappa.rechnungapp.controller.dto.auth.AuthResponseDto;
import com.antonlappa.rechnungapp.controller.dto.auth.LoginRequestDto;
import com.antonlappa.rechnungapp.controller.dto.auth.RegisterRequestDto;

import java.util.UUID;

/**
 * Service interface for authentication operations.
 */
public interface AuthService {

    AuthResponseDto register(RegisterRequestDto request);

    AuthResponseDto login(LoginRequestDto request);

    MeResponse getMe(UUID userId);
}
