package io.github.andis382.installbook.settings;

import io.github.andis382.installbook.auth.Organization;
import io.github.andis382.installbook.common.Phones;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The installer's settings (created with defaults on first use) and the business facts
 * customers see: the name on the card and the number they call.
 */
@Service
public class Installers {

    private final InstallerSettingsRepository settings;
    private final Clock clock;

    public Installers(InstallerSettingsRepository settings, Clock clock) {
        this.settings = settings;
        this.clock = clock;
    }

    @Transactional
    public InstallerSettings settings(Long organizationId) {
        return settings.findById(organizationId).orElseGet(() -> settings.save(new InstallerSettings(organizationId)));
    }

    /** The number printed on cards and in messages: the public phone, else the business phone. */
    public String publicPhone(Organization org, InstallerSettings s) {
        return s.getPublicPhone() != null ? s.getPublicPhone() : org.getPhone();
    }

    public String publicPhoneDisplay(Organization org, InstallerSettings s) {
        return Phones.display(publicPhone(org, s));
    }

    /** "Today" is the installer's calendar day, not the server's. */
    public LocalDate today(Organization org) {
        return LocalDate.now(clock.withZone(zone(org)));
    }

    public static ZoneId zone(Organization org) {
        try {
            return ZoneId.of(org.getTimezone());
        } catch (RuntimeException e) {
            return ZoneId.of("Europe/Tirane");
        }
    }
}
