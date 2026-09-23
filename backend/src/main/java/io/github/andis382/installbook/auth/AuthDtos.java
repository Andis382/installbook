package io.github.andis382.installbook.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class AuthDtos {

    private AuthDtos() {}

    public record RegisterRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Email @Size(max = 190) String email,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotBlank @Size(max = 120) String organizationName,
        @Pattern(regexp = "en|sq") String locale) {}

    public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password) {}

    public record JoinRequest(
        @NotBlank String token,
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Email @Size(max = 190) String email,
        @NotBlank @Size(min = 8, max = 100) String password,
        @Pattern(regexp = "en|sq") String locale) {}

    public record UpdateProfileRequest(
        @NotBlank @Size(max = 120) String name,
        @Pattern(regexp = "en|sq") String locale) {}

    public record UserView(Long id, String name, String email, String role, String locale) {
        public static UserView of(User u) {
            return new UserView(u.getId(), u.getName(), u.getEmail(), u.getRole().name(), u.getLocale());
        }
    }

    public record OrganizationView(Long id, String name, String phone, String country, String locale,
                                   String timezone, String currency) {
        public static OrganizationView of(Organization o) {
            return new OrganizationView(o.getId(), o.getName(), o.getPhone(), o.getCountry(), o.getLocale(),
                o.getTimezone(), o.getCurrency());
        }
    }

    public record MeResponse(UserView user, OrganizationView organization, boolean demo) {}

    public record InvitationInfo(String organizationName, String role, String name, Instant expiresAt) {}
}
