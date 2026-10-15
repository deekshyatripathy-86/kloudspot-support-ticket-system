package com.kloudspot.support.user;

import com.kloudspot.support.common.ApiException;
import com.kloudspot.support.common.Role;
import com.kloudspot.support.organization.Organization;
import com.kloudspot.support.organization.OrganizationRepository;
import com.kloudspot.support.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock UserRepository users;
    @Mock OrganizationRepository organizations;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks UserService service;

    @Test
    void agentDirectoryIsScopedToTheAuthenticatedOrganization() {
        AuthenticatedUser actor = new AuthenticatedUser(7L, 200L, "agent@example.com", Role.AGENT, "Agent");
        when(users.findAllByOrganization_IdAndRoleOrderByNameAsc(200L, Role.AGENT)).thenReturn(List.of());

        assertTrue(service.listAgents(actor).isEmpty());
        verify(users).findAllByOrganization_IdAndRoleOrderByNameAsc(200L, Role.AGENT);
    }

    @Test
    void customerCannotEnumerateAgents() {
        AuthenticatedUser actor = new AuthenticatedUser(7L, 200L, "customer@example.com", Role.CUSTOMER, "Customer");

        ApiException error = assertThrows(ApiException.class, () -> service.listAgents(actor));

        assertEquals(403, error.getStatus().value());
        verifyNoInteractions(users);
    }

    @Test
    void onlyAdminsCanCreateOrganizationUsers() {
        AuthenticatedUser actor = new AuthenticatedUser(7L, 200L, "agent@example.com", Role.AGENT, "Agent");
        UserDtos.CreateRequest request = new UserDtos.CreateRequest(
                "New Agent", "new@example.com", "password123", Role.AGENT);

        ApiException error = assertThrows(ApiException.class, () -> service.create(request, actor));

        assertEquals(403, error.getStatus().value());
        verifyNoInteractions(users, organizations, passwordEncoder);
    }

    @Test
    void creatingUserUsesTheAdminsOrganizationAndHashesPassword() {
        AuthenticatedUser actor = new AuthenticatedUser(7L, 200L, "admin@example.com", Role.ADMIN, "Admin");
        UserDtos.CreateRequest request = new UserDtos.CreateRequest(
                "New Agent", "new@example.com", "password123", Role.AGENT);
        Organization organization = new Organization("Org");
        ReflectionTestUtils.setField(organization, "id", 200L);
        when(users.existsByOrganization_IdAndEmailIgnoreCase(200L, "new@example.com")).thenReturn(false);
        when(organizations.findById(200L)).thenReturn(Optional.of(organization));
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        when(users.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserDtos.Response response = service.create(request, actor);

        assertEquals(200L, response.organizationId());
        assertEquals(Role.AGENT, response.role());
        verify(users).save(argThat(user -> user.getOrganization() == organization
                && "hashed-password".equals(user.getPasswordHash())
                && "new@example.com".equals(user.getEmail())));
    }

    @Test
    void duplicateEmailIsRejectedWithinOrganization() {
        AuthenticatedUser actor = new AuthenticatedUser(7L, 200L, "admin@example.com", Role.ADMIN, "Admin");
        UserDtos.CreateRequest request = new UserDtos.CreateRequest(
                "New Agent", "new@example.com", "password123", Role.AGENT);
        when(users.existsByOrganization_IdAndEmailIgnoreCase(200L, "new@example.com")).thenReturn(true);

        ApiException error = assertThrows(ApiException.class, () -> service.create(request, actor));

        assertEquals(409, error.getStatus().value());
        verifyNoInteractions(organizations, passwordEncoder);
    }

    @Test
    void passwordExceedingBcryptByteLimitIsRejected() {
        AuthenticatedUser actor = new AuthenticatedUser(7L, 200L, "admin@example.com", Role.ADMIN, "Admin");
        UserDtos.CreateRequest request = new UserDtos.CreateRequest(
                "New Agent", "new@example.com", "😀".repeat(19), Role.AGENT);

        ApiException error = assertThrows(ApiException.class, () -> service.create(request, actor));

        assertEquals(400, error.getStatus().value());
        verifyNoInteractions(users, organizations, passwordEncoder);
    }
}
