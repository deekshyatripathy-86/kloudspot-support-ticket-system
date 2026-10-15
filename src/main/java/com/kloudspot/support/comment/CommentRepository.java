package com.kloudspot.support.comment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface CommentRepository extends JpaRepository<TicketComment,Long> {
    List<TicketComment> findByTicketIdAndOrganizationIdOrderByCreatedAtAsc(Long ticketId,Long organizationId);
}
