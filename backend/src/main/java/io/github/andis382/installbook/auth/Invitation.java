package io.github.andis382.installbook.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** A join link for a colleague, driver, carer or relative. Single use, expires. */
@Entity
@Table(name = "invitations")
public class Invitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(nullable = false, unique = true)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /** Who it is for, so the owner recognises it in the list ("Arben, driver"). */
    private String name;

    @Column(name = "invited_by", nullable = false)
    private Long invitedBy;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "accepted_user_id")
    private Long acceptedUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Invitation() {}

    public Invitation(Long organizationId, String token, Role role, String name, Long invitedBy, Instant expiresAt) {
        this.organizationId = organizationId;
        this.token = token;
        this.role = role;
        this.name = name;
        this.invitedBy = invitedBy;
        this.expiresAt = expiresAt;
    }

    public boolean isUsable(Instant now) {
        return acceptedAt == null && expiresAt.isAfter(now);
    }

    public void accept(Long userId, Instant now) {
        this.acceptedAt = now;
        this.acceptedUserId = userId;
    }

    public Long getId() { return id; }
    public Long getOrganizationId() { return organizationId; }
    public String getToken() { return token; }
    public Role getRole() { return role; }
    public String getName() { return name; }
    public Long getInvitedBy() { return invitedBy; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getAcceptedAt() { return acceptedAt; }
    public Long getAcceptedUserId() { return acceptedUserId; }
    public Instant getCreatedAt() { return createdAt; }
}
