package com.antonlappa.rechnungapp.mapper;

import com.antonlappa.rechnungapp.controller.dto.auth.AuthResponseDto;
import com.antonlappa.rechnungapp.repository.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class AuthMapper {

    public AuthResponseDto toDto(UserEntity user, String token) {
        return AuthResponseDto.builder()
                .token(token)
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .build();
    }
}
