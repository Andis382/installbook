package io.github.andis382.installbook.settings;

import io.github.andis382.installbook.auth.CurrentUser;
import io.github.andis382.installbook.auth.Role;
import io.github.andis382.installbook.common.ApiException;
import io.github.andis382.installbook.common.Phones;
import io.github.andis382.installbook.config.AppProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The "Installer" card in Settings. Everyone reads the defaults (the install form uses them); the owner edits. */
@RestController
@RequestMapping("/api/settings/installer")
public class InstallerSettingsController {

    private final Installers installers;
    private final CurrentUser currentUser;
    private final AppProperties props;

    public InstallerSettingsController(Installers installers, CurrentUser currentUser, AppProperties props) {
        this.installers = installers;
        this.currentUser = currentUser;
        this.props = props;
    }

    public record SettingsView(int defaultWarrantyMonths, int defaultServiceIntervalMonths, int reminderLeadDays,
                               int typicalServicePriceCents, String publicPhone) {
        static SettingsView of(InstallerSettings s) {
            return new SettingsView(s.getDefaultWarrantyMonths(), s.getDefaultServiceIntervalMonths(), s.getReminderLeadDays(),
                s.getTypicalServicePriceCents(), s.getPublicPhone());
        }
    }

    public record UpdateSettingsRequest(
        @NotNull @Min(1) @Max(120) Integer defaultWarrantyMonths,
        @NotNull @Min(1) @Max(60) Integer defaultServiceIntervalMonths,
        @NotNull @Min(0) @Max(90) Integer reminderLeadDays,
        @NotNull @Min(0) @Max(1_000_000) Integer typicalServicePriceCents,
        @Size(max = 40) String publicPhone) {}

    @GetMapping
    @Transactional
    public SettingsView get() {
        return SettingsView.of(installers.settings(currentUser.organizationId()));
    }

    @PutMapping
    @Transactional
    public SettingsView update(@Valid @RequestBody UpdateSettingsRequest req) {
        currentUser.requireRole(Role.OWNER);
        String phone = Phones.normalize(req.publicPhone(), props.getDefaultCountryCode());
        if (phone != null && !Phones.isPlausible(phone)) {
            throw ApiException.field("publicPhone", "customer.phone_invalid");
        }
        InstallerSettings s = installers.settings(currentUser.organizationId());
        s.setDefaultWarrantyMonths(req.defaultWarrantyMonths());
        s.setDefaultServiceIntervalMonths(req.defaultServiceIntervalMonths());
        s.setReminderLeadDays(req.reminderLeadDays());
        s.setTypicalServicePriceCents(req.typicalServicePriceCents());
        s.setPublicPhone(phone);
        return SettingsView.of(s);
    }
}
