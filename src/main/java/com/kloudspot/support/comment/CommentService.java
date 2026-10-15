package com.kloudspot.support.comment;

import com.kloudspot.support.common.*;
import com.kloudspot.support.security.AuthenticatedUser;
import com.kloudspot.support.ticket.TicketRepository;
import com.kloudspot.support.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class CommentService {
    private final CommentRepository comments; private final TicketRepository tickets; private final UserRepository users;
    public CommentService(CommentRepository comments,TicketRepository tickets,UserRepository users){this.comments=comments;this.tickets=tickets;this.users=users;}
    @Transactional
    public CommentDtos.Response add(Long ticketId,CommentDtos.CreateRequest req,AuthenticatedUser actor){
        var ticket=accessibleTicket(ticketId,actor);
        if(req.internal() && actor.role()==Role.CUSTOMER) throw new ApiException(HttpStatus.FORBIDDEN,"Customers cannot add internal notes");
        var author=users.findById(actor.userId()).orElseThrow(()->new ApiException(HttpStatus.UNAUTHORIZED,"User no longer exists"));
        var saved=comments.save(new TicketComment(ticket.getId(),actor.organizationId(),actor.userId(),author.getName(),req.content().trim(),req.internal()));
        return dto(saved);
    }
    @Transactional(readOnly=true)
    public List<CommentDtos.Response> list(Long ticketId,AuthenticatedUser actor){
        accessibleTicket(ticketId,actor);
        return comments.findByTicketIdAndOrganizationIdOrderByCreatedAtAsc(ticketId,actor.organizationId()).stream()
                .filter(c->actor.role()!=Role.CUSTOMER || !c.isInternal()).map(this::dto).toList();
    }
    private com.kloudspot.support.ticket.Ticket accessibleTicket(Long id,AuthenticatedUser u){
        if(u.role()==Role.CUSTOMER) return tickets.findByIdAndOrganization_IdAndRequester_Id(id,u.organizationId(),u.userId())
                .orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Ticket not found"));
        return tickets.findByIdAndOrganization_Id(id,u.organizationId()).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Ticket not found"));
    }
    private CommentDtos.Response dto(TicketComment c){return new CommentDtos.Response(c.getId(),c.getTicketId(),c.getAuthorId(),c.getAuthorName(),c.getContent(),c.isInternal(),c.getCreatedAt());}
}
