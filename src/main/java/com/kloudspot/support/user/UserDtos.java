package com.kloudspot.support.user;

import com.kloudspot.support.common.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class UserDtos {
    private UserDtos() {}

    public record CreateRequest(
            @NotBlank @Size(max = 150) String name,
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotNull Role role) {}

    public record Response(Long id, Long organizationId, String name, String email, Role role, Instant createdAt) {}
}
