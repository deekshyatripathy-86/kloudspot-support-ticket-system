package com.kloudspot.support.organization;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "organizations")
public class Organization {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 150)
    private String name;
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Organization() {}
    public Organization(String name) { this.name = name; }
    public Long getId() { return id; }
    public String getName() { return name; }
    public Instant getCreatedAt() { return createdAt; }
    public void setName(String name) { this.name = name; }
}
