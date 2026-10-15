package com.kloudspot.support.auth;

import com.kloudspot.support.common.ApiException;
import com.kloudspot.support.security.JwtService;
import com.kloudspot.support.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users; this.encoder = encoder; this.jwt = jwt;
    }
    @Transactional(readOnly = true)
    public AuthDtos.LoginResponse login(AuthDtos.LoginRequest req) {
        var user = users.findByOrganization_IdAndEmailIgnoreCase(req.organizationId(), req.email())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid organization, email, or password"));
        if (!encoder.matches(req.password(), user.getPasswordHash()))
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid organization, email, or password");
        return new AuthDtos.LoginResponse(jwt.generate(user), "Bearer", jwt.getExpirationMs()/1000,
                new AuthDtos.UserSummary(user.getId(), user.getOrganizationId(), user.getName(), user.getEmail(), user.getRole().name()));
    }
}
