package io.github.andis382.installbook.reminders;

import io.github.andis382.installbook.auth.Organization;
import io.github.andis382.installbook.auth.OrganizationRepository;
import io.github.andis382.installbook.settings.InstallerSettings;
import io.github.andis382.installbook.settings.Installers;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Once per local day, from 08:00 in the installer's own time zone: the reminder run, and on
 * Mondays the digest. Checked often rather than fired once, so a restart at 07:59 or a
 * server in another zone still gets it right, and a missed morning is caught up later that day.
 */
@Service
public class DailyRun {

    static final LocalTime START = LocalTime.of(8, 0);

    private final OrganizationRepository organizations;
    private final Installers installers;
    private final ReminderService reminders;
    private final DigestService digest;
    private final Clock clock;

    public DailyRun(OrganizationRepository organizations, Installers installers, ReminderService reminders,
                    DigestService digest, Clock clock) {
        this.organizations = organizations;
        this.installers = installers;
        this.reminders = reminders;
        this.digest = digest;
        this.clock = clock;
    }

    @Transactional
    public void runIfDue(Long organizationId) {
        Organization org = organizations.findById(organizationId).orElseThrow();
        ZonedDateTime now = ZonedDateTime.now(clock.withZone(Installers.zone(org)));
        if (now.toLocalTime().isBefore(START)) {
            return;
        }
        LocalDate today = now.toLocalDate();
        InstallerSettings settings = installers.settings(organizationId);
        if (!today.equals(settings.getLastReminderRunOn())) {
            reminders.run(organizationId, today);
            settings.setLastReminderRunOn(today);
        }
        if (today.getDayOfWeek() == DayOfWeek.MONDAY && !today.equals(settings.getLastDigestOn())) {
            digest.send(org, today);
        }
    }
}
