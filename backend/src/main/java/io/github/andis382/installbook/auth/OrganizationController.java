package io.github.andis382.installbook.auth;

import io.github.andis382.installbook.auth.AuthDtos.OrganizationView;
import io.github.andis382.installbook.common.Phones;
import io.github.andis382.installbook.config.AppProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.ZoneId;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organization")
public class OrganizationController {

    private final CurrentUser currentUser;
    private final AppProperties props;

    public OrganizationController(CurrentUser currentUser, AppProperties props) {
        this.currentUser = currentUser;
        this.props = props;
    }

    public record UpdateOrganizationRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 40) String phone,
        @Pattern(regexp = "en|sq") String locale,
        @Size(max = 64) String timezone,
        @Pattern(regexp = "[A-Z]{3}") String currency) {}

    @GetMapping
    public OrganizationView get() {
        return OrganizationView.of(currentUser.organization());
    }

    @PutMapping
    @Transactional
    public OrganizationView update(@Valid @RequestBody UpdateOrganizationRequest req) {
        currentUser.requireRole(Role.OWNER);
        Organization org = currentUser.organization();
        org.setName(req.name().trim());
        org.setPhone(Phones.normalize(req.phone(), props.getDefaultCountryCode()));
        if (req.locale() != null) {
            org.setLocale(req.locale());
        }
        if (req.timezone() != null && ZoneId.getAvailableZoneIds().contains(req.timezone())) {
            org.setTimezone(req.timezone());
        }
        if (req.currency() != null) {
            org.setCurrency(req.currency());
        }
        return OrganizationView.of(org);
    }
}
