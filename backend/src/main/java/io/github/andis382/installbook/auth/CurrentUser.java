package io.github.andis382.installbook.auth;

import io.github.andis382.installbook.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Who is calling. Every query for tenant data goes through {@link #organizationId()}. */
@Component
public class CurrentUser {

    private final UserRepository users;
    private final OrganizationRepository organizations;

    public CurrentUser(UserRepository users, OrganizationRepository organizations) {
        this.users = users;
        this.organizations = organizations;
    }

    public AppUserDetails details() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppUserDetails details)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "error.unauthenticated");
        }
        return details;
    }

    public Long id() {
        return details().getUserId();
    }

    public Long organizationId() {
        return details().getOrganizationId();
    }

    public User user() {
        return users.findById(id()).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "error.unauthenticated"));
    }

    public Organization organization() {
        return organizations.findById(organizationId()).orElseThrow(ApiException::notFound);
    }

    public boolean hasRole(Role role) {
        return role.name().equals(details().getRole());
    }

    public void requireRole(Role... roles) {
        String current = details().getRole();
        for (Role role : roles) {
            if (role.name().equals(current)) {
                return;
            }
        }
        throw ApiException.forbidden();
    }
}
