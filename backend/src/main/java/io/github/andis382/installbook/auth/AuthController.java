package io.github.andis382.installbook.auth;

import io.github.andis382.installbook.auth.AuthDtos.InvitationInfo;
import io.github.andis382.installbook.auth.AuthDtos.JoinRequest;
import io.github.andis382.installbook.auth.AuthDtos.LoginRequest;
import io.github.andis382.installbook.auth.AuthDtos.MeResponse;
import io.github.andis382.installbook.auth.AuthDtos.OrganizationView;
import io.github.andis382.installbook.auth.AuthDtos.RegisterRequest;
import io.github.andis382.installbook.auth.AuthDtos.UpdateProfileRequest;
import io.github.andis382.installbook.auth.AuthDtos.UserView;
import io.github.andis382.installbook.common.ApiException;
import io.github.andis382.installbook.config.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Clock;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final SessionLogin sessionLogin;
    private final UserRepository users;
    private final OrganizationRepository organizations;
    private final InvitationRepository invitations;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUser currentUser;
    private final AppProperties props;
    private final Clock clock;

    public AuthController(AuthenticationManager authenticationManager, SessionLogin sessionLogin, UserRepository users,
                          OrganizationRepository organizations, InvitationRepository invitations,
                          PasswordEncoder passwordEncoder, CurrentUser currentUser, AppProperties props, Clock clock) {
        this.authenticationManager = authenticationManager;
        this.sessionLogin = sessionLogin;
        this.users = users;
        this.organizations = organizations;
        this.invitations = invitations;
        this.passwordEncoder = passwordEncoder;
        this.currentUser = currentUser;
        this.props = props;
        this.clock = clock;
    }

    /** Called once when the app boots so the XSRF-TOKEN cookie exists before the first POST. */
    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf(CsrfToken token) {
        token.getToken();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public MeResponse register(@Valid @RequestBody RegisterRequest req, HttpServletRequest request, HttpServletResponse response) {
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw ApiException.field("email", "auth.email_taken");
        }
        String locale = req.locale() == null ? props.getDefaultLocale() : req.locale();
        Organization org = new Organization(req.organizationName().trim());
        org.setLocale(locale);
        org.setTimezone(props.getDefaultTimezone());
        organizations.save(org);
        User user = new User(org.getId(), req.name().trim(), email, passwordEncoder.encode(req.password()), Role.OWNER);
        user.setLocale(locale);
        users.save(user);
        sessionLogin.login(user, request, response);
        return me(user, org);
    }

    @PostMapping("/login")
    @Transactional
    public MeResponse login(@Valid @RequestBody LoginRequest req, HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken.unauthenticated(req.email().trim().toLowerCase(), req.password()));
        sessionLogin.start(auth, request, response);
        User user = users.findById(((AppUserDetails) auth.getPrincipal()).getUserId()).orElseThrow();
        user.setLastLoginAt(clock.instant());
        return me(user, organizations.findById(user.getOrganizationId()).orElseThrow());
    }

    @GetMapping("/me")
    public MeResponse me() {
        return me(currentUser.user(), currentUser.organization());
    }

    @PutMapping("/me")
    @Transactional
    public MeResponse updateMe(@Valid @RequestBody UpdateProfileRequest req) {
        User user = currentUser.user();
        user.setName(req.name().trim());
        if (req.locale() != null) {
            user.setLocale(req.locale());
        }
        return me(user, currentUser.organization());
    }

    @GetMapping("/invitations/{token}")
    public InvitationInfo invitation(@PathVariable String token) {
        Invitation inv = invitations.findByToken(token)
            .filter(i -> i.isUsable(clock.instant()))
            .orElseThrow(() -> new ApiException(HttpStatus.GONE, "auth.invitation_invalid"));
        Organization org = organizations.findById(inv.getOrganizationId()).orElseThrow();
        return new InvitationInfo(org.getName(), inv.getRole().name(), inv.getName(), inv.getExpiresAt());
    }

    @PostMapping("/join")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public MeResponse join(@Valid @RequestBody JoinRequest req, HttpServletRequest request, HttpServletResponse response) {
        Invitation inv = invitations.findByToken(req.token())
            .filter(i -> i.isUsable(clock.instant()))
            .orElseThrow(() -> new ApiException(HttpStatus.GONE, "auth.invitation_invalid"));
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw ApiException.field("email", "auth.email_taken");
        }
        Organization org = organizations.findById(inv.getOrganizationId()).orElseThrow();
        User user = new User(org.getId(), req.name().trim(), email, passwordEncoder.encode(req.password()), inv.getRole());
        user.setLocale(req.locale() == null ? org.getLocale() : req.locale());
        users.save(user);
        inv.accept(user.getId(), clock.instant());
        sessionLogin.login(user, request, response);
        return me(user, org);
    }

    private MeResponse me(User user, Organization org) {
        return new MeResponse(UserView.of(user), OrganizationView.of(org), props.isDemo());
    }
}
