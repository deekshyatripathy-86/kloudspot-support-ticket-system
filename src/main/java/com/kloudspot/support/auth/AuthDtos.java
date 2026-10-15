package com.kloudspot.support.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public final class AuthDtos {
    private AuthDtos() {}
    public record LoginRequest(@NotNull @Positive Long organizationId,
                               @NotBlank @Email String email,
                               @NotBlank String password) {}
    public record UserSummary(Long id, Long organizationId, String name, String email, String role) {}
    public record LoginResponse(String accessToken, String tokenType, long expiresIn, UserSummary user) {}
}
