package com.kloudspot.support.comment;

import com.kloudspot.support.common.Role;
import com.kloudspot.support.common.TicketPriority;
import com.kloudspot.support.organization.Organization;
import com.kloudspot.support.security.AuthenticatedUser;
import com.kloudspot.support.ticket.Ticket;
import com.kloudspot.support.ticket.TicketRepository;
import com.kloudspot.support.user.AppUser;
import com.kloudspot.support.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {
    @Mock CommentRepository comments;
    @Mock TicketRepository tickets;
    @Mock UserRepository users;
    @InjectMocks CommentService service;

    @Test
    void customerCommentListDoesNotIncludeInternalNotes() {
        Organization organization = new Organization("Org");
        ReflectionTestUtils.setField(organization, "id", 20L);
        AppUser customer = new AppUser(organization, "Customer", "customer@example.com", "hash", Role.CUSTOMER);
        ReflectionTestUtils.setField(customer, "id", 7L);
        Ticket ticket = new Ticket(organization, "Subject", "Details", TicketPriority.MEDIUM, customer);
        ReflectionTestUtils.setField(ticket, "id", 42L);
        when(tickets.findByIdAndOrganization_IdAndRequester_Id(42L, 20L, 7L)).thenReturn(Optional.of(ticket));
        when(comments.findByTicketIdAndOrganizationIdOrderByCreatedAtAsc(42L, 20L)).thenReturn(List.of(
                new TicketComment(42L, 20L, 7L, "Customer", "Public response", false),
                new TicketComment(42L, 20L, 9L, "Agent", "Private staff note", true)));
        AuthenticatedUser actor = new AuthenticatedUser(7L, 20L, "customer@example.com", Role.CUSTOMER, "Customer");

        List<CommentDtos.Response> response = service.list(42L, actor);

        assertEquals(1, response.size());
        assertEquals("Public response", response.get(0).content());
        assertFalse(response.get(0).internal());
    }

    @Test
    void customerCannotCreateInternalNotes() {
        Organization organization = new Organization("Org");
        ReflectionTestUtils.setField(organization, "id", 20L);
        AppUser customer = new AppUser(organization, "Customer", "customer@example.com", "hash", Role.CUSTOMER);
        ReflectionTestUtils.setField(customer, "id", 7L);
        Ticket ticket = new Ticket(organization, "Subject", "Details", TicketPriority.MEDIUM, customer);
        ReflectionTestUtils.setField(ticket, "id", 42L);
        when(tickets.findByIdAndOrganization_IdAndRequester_Id(42L, 20L, 7L)).thenReturn(Optional.of(ticket));
        AuthenticatedUser actor = new AuthenticatedUser(7L, 20L, "customer@example.com", Role.CUSTOMER, "Customer");

        var error = assertThrows(com.kloudspot.support.common.ApiException.class,
                () -> service.add(42L, new CommentDtos.CreateRequest("Private", true), actor));

        assertEquals(403, error.getStatus().value());
        verifyNoInteractions(users, comments);
    }
}
