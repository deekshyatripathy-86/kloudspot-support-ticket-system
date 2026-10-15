package com.kloudspot.support.ticket;

import com.kloudspot.support.common.*;
import com.kloudspot.support.organization.OrganizationRepository;
import com.kloudspot.support.security.AuthenticatedUser;
import com.kloudspot.support.user.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.criteria.Predicate;
import java.util.*;

@Service
public class TicketService {
    private final TicketRepository tickets;
    private final UserRepository users;
    private final OrganizationRepository organizations;
    private final TicketHistoryRepository history;
    public TicketService(TicketRepository tickets, UserRepository users, OrganizationRepository organizations, TicketHistoryRepository history) {
        this.tickets=tickets; this.users=users; this.organizations=organizations; this.history=history;
    }
    @Transactional
    public TicketDtos.TicketResponse create(TicketDtos.CreateRequest req, AuthenticatedUser actor) {
        var requester = users.findById(actor.userId()).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED,"User no longer exists"));
        var org = organizations.findById(actor.organizationId()).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED,"Organization no longer exists"));
        Ticket t = tickets.save(new Ticket(org, req.subject().trim(), req.description().trim(), req.priority(), requester));
        return toResponse(t);
    }
    @Transactional(readOnly=true)
    public Page<TicketDtos.TicketResponse> list(AuthenticatedUser actor, TicketStatus status, TicketPriority priority,
                                                Long assignedAgentId, String search, int page, int size, String sortBy, String sortDir) {
        if(page < 0 || size < 1 || size > 100) throw new ApiException(HttpStatus.BAD_REQUEST,"page must be >= 0 and size must be between 1 and 100");
        Set<String> allowed = Set.of("createdAt","updatedAt","priority","status","subject","id");
        if(!allowed.contains(sortBy)) throw new ApiException(HttpStatus.BAD_REQUEST,"Unsupported sortBy");
        Sort.Direction direction;
        try { direction=Sort.Direction.fromString(sortDir); } catch(Exception ex) { throw new ApiException(HttpStatus.BAD_REQUEST,"sortDir must be ASC or DESC"); }
        Specification<Ticket> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.equal(root.get("organization").get("id"), actor.organizationId()));
            if(actor.role()==Role.CUSTOMER) p.add(cb.equal(root.get("requester").get("id"), actor.userId()));
            if(status!=null) p.add(cb.equal(root.get("status"),status));
            if(priority!=null) p.add(cb.equal(root.get("priority"),priority));
            if(assignedAgentId!=null) p.add(cb.equal(root.get("assignedAgent").get("id"),assignedAgentId));
            if(search!=null && !search.isBlank()) p.add(cb.or(cb.like(cb.lower(root.get("subject")),"%"+search.toLowerCase()+"%"),
                    cb.like(cb.lower(root.get("description")),"%"+search.toLowerCase()+"%")));
            return cb.and(p.toArray(new Predicate[0]));
        };
        return tickets.findAll(spec, PageRequest.of(page,size,Sort.by(direction,sortBy))).map(this::toResponse);
    }
    @Transactional(readOnly=true)
    public TicketDtos.TicketResponse get(Long id, AuthenticatedUser actor) { return toResponse(findAccessible(id,actor)); }
    @Transactional
    public TicketDtos.TicketResponse update(Long id, TicketDtos.UpdateRequest req, AuthenticatedUser actor) {
        requireStaff(actor);
        Ticket t=findAccessible(id,actor);
        if(req.subject()!=null) {
            if(req.subject().isBlank()) throw new ApiException(HttpStatus.BAD_REQUEST,"subject must not be blank");
            t.setSubject(req.subject().trim());
        }
        if(req.description()!=null) {
            if(req.description().isBlank()) throw new ApiException(HttpStatus.BAD_REQUEST,"description must not be blank");
            t.setDescription(req.description().trim());
        }
        if(req.priority()!=null) t.setPriority(req.priority());
        return toResponse(tickets.save(t));
    }
    @Transactional
    public TicketDtos.TicketResponse assign(Long id, TicketDtos.AssignRequest req, AuthenticatedUser actor) {
        requireAdminOrAgent(actor);
        Ticket t=findAccessible(id,actor);
        AppUser newAgent=users.findById(req.agentId()).orElseThrow(()->new ApiException(HttpStatus.BAD_REQUEST,"Agent not found"));
        if(!Objects.equals(newAgent.getOrganizationId(),actor.organizationId())) throw new ApiException(HttpStatus.BAD_REQUEST,"Agent must belong to the same organization");
        if(newAgent.getRole()!=Role.AGENT) throw new ApiException(HttpStatus.BAD_REQUEST,"Assigned user must have AGENT role");
        Long oldId=t.getAssignedAgent()==null?null:t.getAssignedAgent().getId();
        if(Objects.equals(oldId,newAgent.getId())) return toResponse(t);
        t.setAssignedAgent(newAgent);
        tickets.save(t);
        history.save(new TicketHistory(t.getId(),actor.organizationId(),actor.userId(),"ASSIGNED_AGENT",
                oldId==null?null:oldId.toString(),newAgent.getId().toString()));
        return toResponse(t);
    }
    @Transactional
    public TicketDtos.TicketResponse changeStatus(Long id, TicketDtos.StatusRequest req, AuthenticatedUser actor) {
        requireAdminOrAgent(actor);
        Ticket t=findAccessible(id,actor);
        TicketStatus old=t.getStatus();
        if(old==req.status()) return toResponse(t);
        if(old==TicketStatus.CLOSED) throw new ApiException(HttpStatus.CONFLICT,"Closed tickets cannot be changed");
        t.setStatus(req.status());
        tickets.save(t);
        history.save(new TicketHistory(t.getId(),actor.organizationId(),actor.userId(),"STATUS",old.name(),req.status().name()));
        return toResponse(t);
    }
    @Transactional
    public TicketDtos.TicketResponse close(Long id, AuthenticatedUser actor) {
        return changeStatus(id,new TicketDtos.StatusRequest(TicketStatus.CLOSED),actor);
    }
    @Transactional(readOnly=true)
    public List<TicketDtos.HistoryResponse> getHistory(Long id, AuthenticatedUser actor) {
        findAccessible(id,actor);
        return history.findByTicketIdAndOrganizationIdOrderByChangedAtAsc(id,actor.organizationId()).stream()
                .map(h -> new TicketDtos.HistoryResponse(h.getId(),h.getFieldName(),h.getOldValue(),h.getNewValue(),h.getChangedBy(),h.getChangedAt())).toList();
    }
    private Ticket findAccessible(Long id, AuthenticatedUser actor) {
        if(actor.role()==Role.CUSTOMER)
            return tickets.findByIdAndOrganization_IdAndRequester_Id(id,actor.organizationId(),actor.userId())
                    .orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Ticket not found"));
        return tickets.findByIdAndOrganization_Id(id,actor.organizationId())
                .orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Ticket not found"));
    }
    private void requireStaff(AuthenticatedUser u) {
        if(u.role()==Role.CUSTOMER) throw new ApiException(HttpStatus.FORBIDDEN,"Customers cannot update ticket fields");
    }
    private void requireAdminOrAgent(AuthenticatedUser u) {
        if(u.role()==Role.CUSTOMER) throw new ApiException(HttpStatus.FORBIDDEN,"Only support staff can perform this action");
    }
    private TicketDtos.TicketResponse toResponse(Ticket t) {
        var req=t.getRequester(); var agent=t.getAssignedAgent();
        return new TicketDtos.TicketResponse(t.getId(),t.getSubject(),t.getDescription(),t.getPriority(),t.getStatus(),
                req.getId(),req.getName(),agent==null?null:agent.getId(),agent==null?null:agent.getName(),
                t.getCreatedAt(),t.getUpdatedAt(),t.getClosedAt());
    }
}
