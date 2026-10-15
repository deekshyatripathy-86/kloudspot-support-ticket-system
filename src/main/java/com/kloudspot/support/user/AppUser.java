package com.kloudspot.support.user;

import com.kloudspot.support.common.Role;
import com.kloudspot.support.organization.Organization;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "app_users",
       uniqueConstraints = @UniqueConstraint(name = "uk_user_org_email", columnNames = {"organization_id", "email"}))
public class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
    @Column(nullable = false, length = 150)
    private String name;
    @Column(nullable = false, length = 255)
    private String email;
    @Column(nullable = false, length = 255)
    private String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private Role role;
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected AppUser() {}
    public AppUser(Organization organization, String name, String email, String passwordHash, Role role) {
        this.organization = organization; this.name = name; this.email = email.toLowerCase(); this.passwordHash = passwordHash; this.role = role;
    }
    public Long getId() { return id; }
    public Organization getOrganization() { return organization; }
    public Long getOrganizationId() { return organization.getId(); }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
    public Instant getCreatedAt() { return createdAt; }
}
