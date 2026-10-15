package com.kloudspot.support.ticket;

import com.kloudspot.support.common.TicketPriority;
import com.kloudspot.support.common.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket,Long>, JpaSpecificationExecutor<Ticket> {
    Optional<Ticket> findByIdAndOrganization_Id(Long id, Long organizationId);
    Optional<Ticket> findByIdAndOrganization_IdAndRequester_Id(Long id, Long organizationId, Long requesterId);
}
