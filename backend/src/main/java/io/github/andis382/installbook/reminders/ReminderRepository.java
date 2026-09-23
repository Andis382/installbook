package io.github.andis382.installbook.reminders;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    Optional<Reminder> findByUnitIdAndDueOn(Long unitId, LocalDate dueOn);

    List<Reminder> findByUnitIdOrderByCreatedAtDesc(Long unitId);

    List<Reminder> findByUnitIdIn(Collection<Long> unitIds);

    /** Reminders that reached a customer's phone since a moment, newest first (to match a "1" reply). */
    @Query("""
        select r from Reminder r, Unit u
        where u.id = r.unitId and u.customer.id = :customerId and r.sentAt is not null and r.sentAt >= :since
        order by r.sentAt desc
        """)
    List<Reminder> findSentToCustomer(@Param("customerId") Long customerId, @Param("since") Instant since, Limit limit);

    long countByOrganizationIdAndSentAtGreaterThanEqual(Long organizationId, Instant since);

    long countByOrganizationIdAndSentAtGreaterThanEqualAndOutcome(Long organizationId, Instant since, Reminder.Outcome outcome);
}
