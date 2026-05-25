package com.antonlappa.rechnungapp.service;

import com.antonlappa.rechnungapp.controller.dto.auth.AuthResponseDto;
import com.antonlappa.rechnungapp.controller.dto.auth.LoginRequestDto;
import com.antonlappa.rechnungapp.controller.dto.auth.RegisterRequestDto;

public interface AuthService {

    AuthResponseDto register(RegisterRequestDto request);

    AuthResponseDto login(LoginRequestDto request);
}
