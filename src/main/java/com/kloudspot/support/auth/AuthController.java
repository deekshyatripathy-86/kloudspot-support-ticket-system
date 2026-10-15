package com.kloudspot.support.auth;

import com.kloudspot.support.security.PrincipalUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService service;
    public AuthController(AuthService service) { this.service = service; }
    @PostMapping("/login")
    public AuthDtos.LoginResponse login(@Valid @RequestBody AuthDtos.LoginRequest req) { return service.login(req); }
    @GetMapping("/me")
    public AuthDtos.UserSummary me() {
        var u = PrincipalUtil.current();
        return new AuthDtos.UserSummary(u.userId(), u.organizationId(), u.name(), u.email(), u.role().name());
    }
}
