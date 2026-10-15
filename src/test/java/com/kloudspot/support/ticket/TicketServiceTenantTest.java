package com.kloudspot.support.ticket;

import com.kloudspot.support.common.*;
import com.kloudspot.support.organization.Organization;
import com.kloudspot.support.organization.OrganizationRepository;
import com.kloudspot.support.security.AuthenticatedUser;
import com.kloudspot.support.user.AppUser;
import com.kloudspot.support.user.UserRepository;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceTenantTest {
    @Mock TicketRepository tickets;
    @Mock UserRepository users;
    @Mock OrganizationRepository organizations;
    @Mock TicketHistoryRepository history;
    @InjectMocks TicketService service;

    @Test
    void crossOrganizationTicketIsNotFound() {
        when(tickets.findByIdAndOrganization_Id(42L, 2L)).thenReturn(Optional.empty());
        AuthenticatedUser actor = new AuthenticatedUser(7L,2L,"agent@b.example",Role.AGENT,"Agent B");
        assertThrows(ApiException.class, () -> service.get(42L,actor));
        verify(tickets).findByIdAndOrganization_Id(42L,2L);
    }

    @Test
    void customerLookupIsScopedToTheirOwnTickets() {
        when(tickets.findByIdAndOrganization_IdAndRequester_Id(42L,2L,7L)).thenReturn(Optional.empty());
        AuthenticatedUser actor = new AuthenticatedUser(7L,2L,"customer@b.example",Role.CUSTOMER,"Customer B");
        assertThrows(ApiException.class, () -> service.get(42L,actor));
        verify(tickets).findByIdAndOrganization_IdAndRequester_Id(42L,2L,7L);
        verify(tickets, never()).findByIdAndOrganization_Id(anyLong(),anyLong());
    }

    @Test
    void assignmentAcceptsMatchingOrganizationIdsOutsideLongCache() {
        Organization organization = new Organization("Org");
        ReflectionTestUtils.setField(organization, "id", 1000L);
        AppUser requester = new AppUser(organization, "Requester", "requester@example.com", "hash", Role.CUSTOMER);
        AppUser agent = new AppUser(organization, "Agent", "agent@example.com", "hash", Role.AGENT);
        ReflectionTestUtils.setField(agent, "id", 9L);
        Ticket ticket = new Ticket(organization, "Subject", "Description", TicketPriority.MEDIUM, requester);
        ReflectionTestUtils.setField(ticket, "id", 42L);
        when(tickets.findByIdAndOrganization_Id(42L, 1000L)).thenReturn(Optional.of(ticket));
        when(users.findById(9L)).thenReturn(Optional.of(agent));
        when(tickets.save(ticket)).thenReturn(ticket);

        AuthenticatedUser actor = new AuthenticatedUser(7L, Long.valueOf(1000L), "admin@example.com", Role.ADMIN, "Admin");
        TicketDtos.TicketResponse response = service.assign(42L, new TicketDtos.AssignRequest(9L), actor);

        assertEquals(9L, response.assignedAgentId());
        assertEquals(agent, ticket.getAssignedAgent());
        verify(history).save(argThat(entry -> "ASSIGNED_AGENT".equals(entry.getFieldName())
                && "9".equals(entry.getNewValue())));
    }

    @Test
    void assignmentRejectsAgentFromAnotherOrganization() {
        Organization organization = new Organization("Org A");
        Organization otherOrganization = new Organization("Org B");
        ReflectionTestUtils.setField(organization, "id", 2L);
        ReflectionTestUtils.setField(otherOrganization, "id", 3L);
        AppUser requester = new AppUser(organization, "Requester", "requester@example.com", "hash", Role.CUSTOMER);
        AppUser agent = new AppUser(otherOrganization, "Agent", "agent@example.com", "hash", Role.AGENT);
        Ticket ticket = new Ticket(organization, "Subject", "Description", TicketPriority.MEDIUM, requester);
        ReflectionTestUtils.setField(ticket, "id", 42L);
        when(tickets.findByIdAndOrganization_Id(42L, 2L)).thenReturn(Optional.of(ticket));
        when(users.findById(9L)).thenReturn(Optional.of(agent));
        AuthenticatedUser actor = new AuthenticatedUser(7L, 2L, "admin@example.com", Role.ADMIN, "Admin");

        assertThrows(ApiException.class,
                () -> service.assign(42L, new TicketDtos.AssignRequest(9L), actor));
        assertNull(ticket.getAssignedAgent());
        verify(history, never()).save(any());
    }

    @Test
    void statusChangePersistsHistoryAndTimestamp() {
        Organization organization = new Organization("Org");
        ReflectionTestUtils.setField(organization, "id", 2L);
        AppUser requester = new AppUser(organization, "Requester", "requester@example.com", "hash", Role.CUSTOMER);
        Ticket ticket = new Ticket(organization, "Subject", "Description", TicketPriority.MEDIUM, requester);
        ReflectionTestUtils.setField(ticket, "id", 42L);
        when(tickets.findByIdAndOrganization_Id(42L, 2L)).thenReturn(Optional.of(ticket));
        when(tickets.save(ticket)).thenReturn(ticket);
        AuthenticatedUser actor = new AuthenticatedUser(8L, 2L, "agent@example.com", Role.AGENT, "Agent");

        TicketDtos.TicketResponse response = service.changeStatus(
                42L, new TicketDtos.StatusRequest(TicketStatus.IN_PROGRESS), actor);

        assertEquals(TicketStatus.IN_PROGRESS, response.status());
        assertNotNull(response.updatedAt());
        verify(history).save(argThat(entry -> "STATUS".equals(entry.getFieldName())
                && "OPEN".equals(entry.getOldValue())
                && "IN_PROGRESS".equals(entry.getNewValue())));
    }

    @Test
    void customerCannotChangeTicketStatus() {
        AuthenticatedUser actor = new AuthenticatedUser(7L, 2L, "customer@example.com", Role.CUSTOMER, "Customer");

        ApiException error = assertThrows(ApiException.class, () -> service.changeStatus(
                42L, new TicketDtos.StatusRequest(TicketStatus.CLOSED), actor));

        assertEquals(403, error.getStatus().value());
        verifyNoInteractions(tickets, history);
    }

    @Test
    void invalidPaginationIsRejectedBeforeQueryingTickets() {
        AuthenticatedUser actor = new AuthenticatedUser(7L, 2L, "agent@example.com", Role.AGENT, "Agent");

        assertThrows(ApiException.class, () -> service.list(
                actor, null, null, null, null, -1, 10, "createdAt", "DESC"));
        verifyNoInteractions(tickets);
    }
}
