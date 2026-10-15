package com.kloudspot.support.ticket;

import com.kloudspot.support.common.TicketPriority;
import com.kloudspot.support.common.TicketStatus;
import com.kloudspot.support.organization.Organization;
import com.kloudspot.support.user.AppUser;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="tickets", indexes = {
 @Index(name="idx_ticket_org_status", columnList="organization_id,status"),
 @Index(name="idx_ticket_org_priority", columnList="organization_id,priority"),
 @Index(name="idx_ticket_org_agent", columnList="organization_id,assigned_agent_id"),
 @Index(name="idx_ticket_org_created", columnList="organization_id,created_at")
})
public class Ticket {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(name="organization_id", nullable=false) private Organization organization;
    @Column(nullable=false, length=255) private String subject;
    @Column(nullable=false, columnDefinition="TEXT") private String description;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private TicketPriority priority;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private TicketStatus status = TicketStatus.OPEN;
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(name="requester_id", nullable=false) private AppUser requester;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="assigned_agent_id") private AppUser assignedAgent;
    @Column(nullable=false, updatable=false) private Instant createdAt = Instant.now();
    @Column(nullable=false) private Instant updatedAt = Instant.now();
    private Instant closedAt;

    protected Ticket() {}
    public Ticket(Organization organization, String subject, String description, TicketPriority priority, AppUser requester) {
        this.organization=organization; this.subject=subject; this.description=description; this.priority=priority; this.requester=requester;
    }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
    public Long getId(){return id;} public Organization getOrganization(){return organization;}
    public Long getOrganizationId(){return organization.getId();} public String getSubject(){return subject;}
    public String getDescription(){return description;} public TicketPriority getPriority(){return priority;}
    public TicketStatus getStatus(){return status;} public AppUser getRequester(){return requester;}
    public AppUser getAssignedAgent(){return assignedAgent;} public Instant getCreatedAt(){return createdAt;}
    public Instant getUpdatedAt(){return updatedAt;} public Instant getClosedAt(){return closedAt;}
    public void setSubject(String v){subject=v; touch();} public void setDescription(String v){description=v; touch();}
    public void setPriority(TicketPriority v){priority=v; touch();}
    public void setStatus(TicketStatus v){status=v; if(v==TicketStatus.CLOSED) closedAt=Instant.now(); else closedAt=null; touch();}
    public void setAssignedAgent(AppUser v){assignedAgent=v; touch();}
    private void touch(){updatedAt=Instant.now();}
}
