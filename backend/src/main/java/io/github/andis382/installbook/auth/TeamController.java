package io.github.andis382.installbook.auth;

import io.github.andis382.installbook.common.ApiException;
import io.github.andis382.installbook.common.Tokens;
import io.github.andis382.installbook.config.AppProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** People in the organisation and the join links waiting to be used. */
@RestController
@RequestMapping("/api/team")
public class TeamController {

    private static final Duration INVITATION_TTL = Duration.ofDays(14);

    private final UserRepository users;
    private final InvitationRepository invitations;
    private final CurrentUser currentUser;
    private final AppProperties props;
    private final Clock clock;

    public TeamController(UserRepository users, InvitationRepository invitations, CurrentUser currentUser,
                          AppProperties props, Clock clock) {
        this.users = users;
        this.invitations = invitations;
        this.currentUser = currentUser;
        this.props = props;
        this.clock = clock;
    }

    public record Member(Long id, String name, String email, String role, Instant lastLoginAt, boolean you) {}

    public record PendingInvitation(Long id, String name, String role, String url, Instant expiresAt) {}

    public record TeamView(List<Member> members, List<PendingInvitation> invitations) {}

    public record InviteRequest(@NotNull Role role, @Size(max = 120) String name) {}

    @GetMapping
    public TeamView team() {
        Long orgId = currentUser.organizationId();
        Long me = currentUser.id();
        Instant now = clock.instant();
        List<Member> members = users.findByOrganizationIdOrderByNameAsc(orgId).stream()
            .map(u -> new Member(u.getId(), u.getName(), u.getEmail(), u.getRole().name(), u.getLastLoginAt(), u.getId().equals(me)))
            .toList();
        List<PendingInvitation> pending = invitations.findByOrganizationIdAndAcceptedAtIsNullOrderByCreatedAtDesc(orgId).stream()
            .filter(i -> i.isUsable(now))
            .map(this::view)
            .toList();
        return new TeamView(members, pending);
    }

    @PostMapping("/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public PendingInvitation invite(@Valid @RequestBody InviteRequest req) {
        currentUser.requireRole(Role.OWNER);
        if (req.role() != Role.TECHNICIAN) {
            throw ApiException.field("role", "team.role_not_invitable");
        }
        Invitation inv = new Invitation(currentUser.organizationId(), Tokens.urlToken(), req.role(),
            req.name() == null ? null : req.name().trim(), currentUser.id(), clock.instant().plus(INVITATION_TTL));
        invitations.save(inv);
        return view(inv);
    }

    @DeleteMapping("/invitations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void revoke(@PathVariable Long id) {
        currentUser.requireRole(Role.OWNER);
        Invitation inv = invitations.findByIdAndOrganizationId(id, currentUser.organizationId()).orElseThrow(ApiException::notFound);
        invitations.delete(inv);
    }

    @DeleteMapping("/members/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void remove(@PathVariable Long id) {
        currentUser.requireRole(Role.OWNER);
        if (id.equals(currentUser.id())) {
            throw ApiException.conflict("team.cannot_remove_self");
        }
        User user = users.findByIdAndOrganizationId(id, currentUser.organizationId()).orElseThrow(ApiException::notFound);
        users.delete(user);
    }

    private PendingInvitation view(Invitation i) {
        return new PendingInvitation(i.getId(), i.getName(), i.getRole().name(), props.link("/join/" + i.getToken()), i.getExpiresAt());
    }
}
