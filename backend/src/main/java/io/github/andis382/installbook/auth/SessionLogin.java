package io.github.andis382.installbook.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Clock;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

/** Puts an authenticated user into the HTTP session (after login, sign-up or joining). */
@Component
public class SessionLogin {

    private final SecurityContextRepository repository;
    private final UserRepository users;
    private final Clock clock;

    public SessionLogin(SecurityContextRepository repository, UserRepository users, Clock clock) {
        this.repository = repository;
        this.users = users;
        this.clock = clock;
    }

    public void login(User user, HttpServletRequest request, HttpServletResponse response) {
        AppUserDetails details = new AppUserDetails(user);
        Authentication auth = UsernamePasswordAuthenticationToken.authenticated(details, null, details.getAuthorities());
        start(auth, request, response);
        user.setLastLoginAt(clock.instant());
        users.save(user);
    }

    public void start(Authentication auth, HttpServletRequest request, HttpServletResponse response) {
        request.getSession(true);
        request.changeSessionId();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        repository.saveContext(context, request, response);
    }
}
