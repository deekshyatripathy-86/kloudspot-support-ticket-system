package com.kloudspot.support.ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface TicketHistoryRepository extends JpaRepository<TicketHistory,Long> {
    List<TicketHistory> findByTicketIdAndOrganizationIdOrderByChangedAtAsc(Long ticketId, Long organizationId);
}
