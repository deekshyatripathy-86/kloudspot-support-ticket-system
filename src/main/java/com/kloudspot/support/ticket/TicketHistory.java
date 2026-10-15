package com.kloudspot.support.ticket;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="ticket_history", indexes=@Index(name="idx_history_org_ticket", columnList="organization_id,ticket_id"))
public class TicketHistory {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="ticket_id", nullable=false) private Long ticketId;
    @Column(name="organization_id", nullable=false) private Long organizationId;
    @Column(name="changed_by", nullable=false) private Long changedBy;
    @Column(name="field_name", nullable=false, length=50) private String fieldName;
    @Column(name="old_value", length=255) private String oldValue;
    @Column(name="new_value", length=255) private String newValue;
    @Column(nullable=false, updatable=false) private Instant changedAt=Instant.now();
    protected TicketHistory() {}
    public TicketHistory(Long ticketId, Long orgId, Long changedBy, String field, String oldVal, String newVal) {
        this.ticketId=ticketId; this.organizationId=orgId; this.changedBy=changedBy; this.fieldName=field; this.oldValue=oldVal; this.newValue=newVal;
    }
    public Long getId(){return id;} public Long getTicketId(){return ticketId;} public Long getOrganizationId(){return organizationId;}
    public Long getChangedBy(){return changedBy;} public String getFieldName(){return fieldName;}
    public String getOldValue(){return oldValue;} public String getNewValue(){return newValue;} public Instant getChangedAt(){return changedAt;}
}
