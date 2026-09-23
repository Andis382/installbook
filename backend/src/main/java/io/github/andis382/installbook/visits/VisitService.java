package io.github.andis382.installbook.visits;

import io.github.andis382.installbook.common.ApiException;
import io.github.andis382.installbook.reminders.ReminderRepository;
import io.github.andis382.installbook.units.Unit;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Recording work done on a unit, and moving its service cycle on when that work was the service. */
@Service
public class VisitService {

    private final ServiceVisitRepository visits;
    private final ReminderRepository reminders;

    public VisitService(ServiceVisitRepository visits, ReminderRepository reminders) {
        this.visits = visits;
        this.reminders = reminders;
    }

    public record VisitRequest(
        @NotNull LocalDate visitedOn,
        @NotNull ServiceKind kind,
        @PositiveOrZero @Max(10_000_000) Integer priceCents,
        @Size(max = 1000) String parts,
        @Size(max = 2000) String notes) {}

    @Transactional
    public ServiceVisit record(Unit unit, VisitRequest req, Long userId, LocalDate today) {
        if (!unit.isActive()) {
            throw ApiException.conflict("unit.not_active");
        }
        if (req.visitedOn().isAfter(today)) {
            throw ApiException.field("visitedOn", "unit.date_future");
        }
        LocalDate visitedOn = req.visitedOn().isBefore(unit.getInstalledOn()) ? unit.getInstalledOn() : req.visitedOn();
        ServiceVisit visit = visits.save(new ServiceVisit(unit.getOrganizationId(), unit.getId(), visitedOn, req.kind(),
            req.priceCents(), blankToNull(req.parts()), blankToNull(req.notes()), userId));
        if (req.kind().restartsCycle()) {
            LocalDate cycle = unit.getNextServiceDue();
            unit.serviced(visitedOn);
            if (!cycle.equals(unit.getNextServiceDue())) {
                reminders.findByUnitIdAndDueOn(unit.getId(), cycle).ifPresent(r -> r.serviced());
            }
        }
        return visit;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
