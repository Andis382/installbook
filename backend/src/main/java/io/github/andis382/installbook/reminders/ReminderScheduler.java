package io.github.andis382.installbook.reminders;

import io.github.andis382.installbook.auth.Organization;
import io.github.andis382.installbook.auth.OrganizationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Wakes every ten minutes and lets each installer's {@link DailyRun} decide whether it is time. */
@Component
@ConditionalOnProperty(name = "app.jobs.enabled", havingValue = "true", matchIfMissing = true)
public class ReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);

    private final OrganizationRepository organizations;
    private final DailyRun dailyRun;

    public ReminderScheduler(OrganizationRepository organizations, DailyRun dailyRun) {
        this.organizations = organizations;
        this.dailyRun = dailyRun;
    }

    @Scheduled(cron = "0 */10 * * * *")
    public void tick() {
        for (Organization org : organizations.findAll()) {
            try {
                dailyRun.runIfDue(org.getId());
            } catch (RuntimeException e) {
                log.warn("Daily run failed for organisation {}", org.getId(), e);
            }
        }
    }
}
