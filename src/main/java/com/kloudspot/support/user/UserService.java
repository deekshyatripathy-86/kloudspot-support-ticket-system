package com.kloudspot.support.user;

import com.kloudspot.support.common.ApiException;
import com.kloudspot.support.common.Role;
import com.kloudspot.support.organization.OrganizationRepository;
import com.kloudspot.support.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class UserService {
    private final UserRepository users;
    private final OrganizationRepository organizations;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository users, OrganizationRepository organizations, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.organizations = organizations;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserDtos.Response> listAgents(AuthenticatedUser actor) {
        requireStaff(actor);
        return users.findAllByOrganization_IdAndRoleOrderByNameAsc(actor.organizationId(), Role.AGENT)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<UserDtos.Response> listOrganizationUsers(AuthenticatedUser actor) {
        requireAdmin(actor);
        return users.findAllByOrganization_IdOrderByNameAsc(actor.organizationId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public UserDtos.Response create(UserDtos.CreateRequest request, AuthenticatedUser actor) {
        requireAdmin(actor);
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Password must not exceed 72 UTF-8 bytes");
        }
        if (users.existsByOrganization_IdAndEmailIgnoreCase(actor.organizationId(), request.email().trim())) {
            throw new ApiException(HttpStatus.CONFLICT, "A user with this email already exists in the organization");
        }
        var organization = organizations.findById(actor.organizationId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Organization no longer exists"));
        var user = users.save(new AppUser(organization, request.name().trim(), request.email().trim(),
                passwordEncoder.encode(request.password()), request.role()));
        return toResponse(user);
    }

    private void requireStaff(AuthenticatedUser actor) {
        if (actor.role() == Role.CUSTOMER) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only organization staff can list agents");
        }
    }

    private void requireAdmin(AuthenticatedUser actor) {
        if (actor.role() != Role.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only organization admins can manage users");
        }
    }

    private UserDtos.Response toResponse(AppUser user) {
        return new UserDtos.Response(user.getId(), user.getOrganizationId(), user.getName(),
                user.getEmail(), user.getRole(), user.getCreatedAt());
    }
}
