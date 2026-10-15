package com.kloudspot.support.comment;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="ticket_comments", indexes=@Index(name="idx_comment_org_ticket",columnList="organization_id,ticket_id"))
public class TicketComment {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="ticket_id",nullable=false) private Long ticketId;
    @Column(name="organization_id",nullable=false) private Long organizationId;
    @Column(name="author_id",nullable=false) private Long authorId;
    @Column(name="author_name",nullable=false) private String authorName;
    @Column(nullable=false,columnDefinition="TEXT") private String content;
    @Column(nullable=false) private boolean internal;
    @Column(nullable=false,updatable=false) private Instant createdAt=Instant.now();
    protected TicketComment(){}
    public TicketComment(Long ticketId,Long organizationId,Long authorId,String authorName,String content,boolean internal){
        this.ticketId=ticketId;this.organizationId=organizationId;this.authorId=authorId;this.authorName=authorName;this.content=content;this.internal=internal;
    }
    public Long getId(){return id;} public Long getTicketId(){return ticketId;} public Long getOrganizationId(){return organizationId;}
    public Long getAuthorId(){return authorId;} public String getAuthorName(){return authorName;} public String getContent(){return content;}
    public boolean isInternal(){return internal;} public Instant getCreatedAt(){return createdAt;}
}
