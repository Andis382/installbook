package io.github.andis382.installbook.reminders;

import io.github.andis382.installbook.auth.Organization;
import io.github.andis382.installbook.auth.Role;
import io.github.andis382.installbook.auth.User;
import io.github.andis382.installbook.auth.UserRepository;
import io.github.andis382.installbook.bookings.BookingRequest;
import io.github.andis382.installbook.bookings.BookingRequestRepository;
import io.github.andis382.installbook.common.Texts;
import io.github.andis382.installbook.config.AppProperties;
import io.github.andis382.installbook.messaging.Messenger;
import io.github.andis382.installbook.messaging.Messenger.Outgoing;
import io.github.andis382.installbook.messaging.OutboundMessage;
import io.github.andis382.installbook.notify.MessageTexts;
import io.github.andis382.installbook.settings.Installers;
import io.github.andis382.installbook.units.Unit;
import io.github.andis382.installbook.units.UnitRepository;
import io.github.andis382.installbook.units.UnitStatus;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The installer's Monday message: what is overdue, what is due this month, who asked for a visit. */
@Service
public class DigestService {

    private static final int MAX_LINES = 6;

    private final UnitRepository units;
    private final BookingRequestRepository bookings;
    private final UserRepository users;
    private final Messenger messenger;
    private final MessageTexts texts;
    private final Texts plain;
    private final Installers installers;
    private final AppProperties props;

    public DigestService(UnitRepository units, BookingRequestRepository bookings, UserRepository users, Messenger messenger,
                         MessageTexts texts, Texts plain, Installers installers, AppProperties props) {
        this.units = units;
        this.bookings = bookings;
        this.users = users;
        this.messenger = messenger;
        this.texts = texts;
        this.plain = plain;
        this.installers = installers;
        this.props = props;
    }

    /** Sends to the business phone; empty when the installer has not set one. */
    @Transactional
    public Optional<OutboundMessage> send(Organization org, LocalDate today) {
        installers.settings(org.getId()).setLastDigestOn(today);
        if (org.getPhone() == null || org.getPhone().isBlank()) {
            return Optional.empty();
        }
        String locale = org.getLocale();
        List<Unit> overdue = units.findByOrganizationIdAndStatusAndNextServiceDueLessThanEqualOrderByNextServiceDueAsc(
            org.getId(), UnitStatus.ACTIVE, today.minusDays(1));
        List<Unit> dueThisMonth = units.findByOrganizationIdAndStatusAndNextServiceDueBetweenOrderByNextServiceDueAsc(
            org.getId(), UnitStatus.ACTIVE, today, today.withDayOfMonth(today.lengthOfMonth()));
        long newBookings = bookings.countByOrganizationIdAndStatus(org.getId(), BookingRequest.Status.NEW);

        List<String> lines = new ArrayList<>();
        overdue.stream().limit(MAX_LINES).forEach(u -> lines.add(line(u, "digest.overdue_since", locale)));
        dueThisMonth.stream().limit(MAX_LINES - lines.size()).forEach(u -> lines.add(line(u, "digest.due_on", locale)));

        Map<String, String> params = new LinkedHashMap<>();
        params.put("name", ownerFirstName(org));
        params.put("business", org.getName());
        params.put("overdue", String.valueOf(overdue.size()));
        params.put("dueMonth", String.valueOf(dueThisMonth.size()));
        params.put("newBookings", String.valueOf(newBookings));
        params.put("list", lines.isEmpty() ? plain.in(locale, "digest.nothing") : String.join("\n", lines));
        return Optional.of(messenger.send(new Outgoing(org.getId(), org.getPhone(), org.getName(), "installer_digest",
            locale, params, props.link("/due"), "DIGEST", null)));
    }

    private String line(Unit unit, String key, String locale) {
        String when = plain.in(locale, key).replace("{date}", texts.day(unit.getNextServiceDue(), locale));
        return "- " + unit.getCustomer().getName() + ", " + texts.unit(unit, locale) + " (" + when + ")";
    }

    private String ownerFirstName(Organization org) {
        return users.findByOrganizationIdOrderByNameAsc(org.getId()).stream()
            .filter(u -> u.getRole() == Role.OWNER)
            .map(User::getName)
            .map(n -> n.trim().split("\\s+")[0])
            .findFirst()
            .orElse(org.getName());
    }
}
