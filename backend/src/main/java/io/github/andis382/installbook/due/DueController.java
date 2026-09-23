package io.github.andis382.installbook.due;

import io.github.andis382.installbook.auth.CurrentUser;
import io.github.andis382.installbook.auth.Organization;
import io.github.andis382.installbook.auth.Role;
import io.github.andis382.installbook.common.ApiException;
import io.github.andis382.installbook.messaging.MessagesController.MessageView;
import io.github.andis382.installbook.reminders.DigestService;
import io.github.andis382.installbook.reminders.ReminderService;
import io.github.andis382.installbook.reminders.ReminderService.RunResult;
import io.github.andis382.installbook.settings.Installers;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DueController {

    private final DueService due;
    private final ReminderService reminders;
    private final DigestService digest;
    private final Installers installers;
    private final CurrentUser currentUser;

    public DueController(DueService due, ReminderService reminders, DigestService digest, Installers installers,
                         CurrentUser currentUser) {
        this.due = due;
        this.reminders = reminders;
        this.digest = digest;
        this.installers = installers;
        this.currentUser = currentUser;
    }

    @GetMapping("/api/due")
    public DueService.DueView view(@RequestParam(required = false) String month) {
        return due.view(currentUser.organization(), parse(month));
    }

    /** "Send due reminders now": the same run as the 08:00 job, for this installer, right away. */
    @PostMapping("/api/reminders/run")
    public RunResult runReminders() {
        currentUser.requireRole(Role.OWNER);
        Organization org = currentUser.organization();
        return reminders.run(org.getId(), installers.today(org));
    }

    /** The Monday digest on demand, so the installer can see what it looks like. */
    @PostMapping("/api/reminders/digest")
    public MessageView sendDigest() {
        currentUser.requireRole(Role.OWNER);
        Organization org = currentUser.organization();
        return digest.send(org, installers.today(org)).map(MessageView::of)
            .orElseThrow(() -> ApiException.conflict("digest.no_phone"));
    }

    private static YearMonth parse(String month) {
        if (month == null || month.isBlank()) {
            return null;
        }
        try {
            return YearMonth.parse(month);
        } catch (DateTimeParseException e) {
            throw ApiException.badRequest("error.bad_request");
        }
    }
}
