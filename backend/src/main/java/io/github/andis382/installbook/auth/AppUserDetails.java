package io.github.andis382.installbook.auth;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** The signed-in person as Spring Security sees it; stored in the session, so keep it small. */
public class AppUserDetails implements UserDetails, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long userId;
    private final Long organizationId;
    private final String email;
    private final String passwordHash;
    private final String role;

    public AppUserDetails(User user) {
        this.userId = user.getId();
        this.organizationId = user.getOrganizationId();
        this.email = user.getEmail();
        this.passwordHash = user.getPasswordHash();
        this.role = user.getRole().name();
    }

    public Long getUserId() { return userId; }
    public Long getOrganizationId() { return organizationId; }
    public String getRole() { return role; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() { return passwordHash; }

    @Override
    public String getUsername() { return email; }
}
