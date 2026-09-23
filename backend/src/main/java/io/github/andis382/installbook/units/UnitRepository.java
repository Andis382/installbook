package io.github.andis382.installbook.units;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UnitRepository extends JpaRepository<Unit, Long>, JpaSpecificationExecutor<Unit> {

    @EntityGraph(attributePaths = "customer")
    Optional<Unit> findByIdAndOrganizationId(Long id, Long organizationId);

    @EntityGraph(attributePaths = "customer")
    Optional<Unit> findByCardToken(String cardToken);

    @Override
    @EntityGraph(attributePaths = "customer")
    Page<Unit> findAll(Specification<Unit> spec, Pageable pageable);

    @EntityGraph(attributePaths = "customer")
    List<Unit> findByCustomerIdOrderByInstalledOnDesc(Long customerId);

    /** Units in service whose next service falls on or before a day, soonest first. */
    @EntityGraph(attributePaths = "customer")
    List<Unit> findByOrganizationIdAndStatusAndNextServiceDueLessThanEqualOrderByNextServiceDueAsc(
        Long organizationId, UnitStatus status, LocalDate until);

    @EntityGraph(attributePaths = "customer")
    List<Unit> findByOrganizationIdAndStatusAndNextServiceDueBetweenOrderByNextServiceDueAsc(
        Long organizationId, UnitStatus status, LocalDate from, LocalDate to);

    long countByOrganizationIdAndStatusAndNextServiceDueBetween(Long organizationId, UnitStatus status, LocalDate from,
                                                                LocalDate to);

    long countByOrganizationIdAndStatusAndNextServiceDueBefore(Long organizationId, UnitStatus status, LocalDate before);

    long countByOrganizationIdAndInstalledOnBetween(Long organizationId, LocalDate from, LocalDate to);

    long countByOrganizationIdAndStatus(Long organizationId, UnitStatus status);

    List<Unit> findByOrganizationIdAndInstalledOnGreaterThanEqual(Long organizationId, LocalDate from);

    @Query("""
        select u.id from Unit u
        where u.organizationId = :org and u.status = :status and upper(u.serialNumber) = upper(:serial)
        """)
    List<Long> findIdsBySerial(@Param("org") Long organizationId, @Param("status") UnitStatus status,
                               @Param("serial") String serial);

    /** Brands this installer fits, most used first, to suggest while typing. */
    @Query("""
        select u.brand from Unit u where u.organizationId = :org
        group by u.brand order by count(u) desc, u.brand
        """)
    List<String> brandsByUse(@Param("org") Long organizationId);
}
