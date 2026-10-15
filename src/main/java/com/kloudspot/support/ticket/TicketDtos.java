package com.kloudspot.support.ticket;

import com.kloudspot.support.common.TicketPriority;
import com.kloudspot.support.common.TicketStatus;
import jakarta.validation.constraints.*;
import java.time.Instant;

public final class TicketDtos {
    private TicketDtos() {}
    public record CreateRequest(@NotBlank @Size(max=255) String subject,
                                @NotBlank @Size(max=10000) String description,
                                @NotNull TicketPriority priority) {}
    public record UpdateRequest(@Size(min=1,max=255) String subject,
                                @Size(min=1,max=10000) String description,
                                TicketPriority priority) {}
    public record AssignRequest(@NotNull @Positive Long agentId) {}
    public record StatusRequest(@NotNull TicketStatus status) {}
    public record TicketResponse(Long id, String subject, String description, TicketPriority priority,
                                 TicketStatus status, Long requesterId, String requesterName,
                                 Long assignedAgentId, String assignedAgentName, Instant createdAt,
                                 Instant updatedAt, Instant closedAt) {}
    public record HistoryResponse(Long id, String fieldName, String oldValue, String newValue,
                                  Long changedBy, Instant changedAt) {}
}
