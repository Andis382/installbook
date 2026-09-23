package io.github.andis382.installbook.bookings;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRequestRepository extends JpaRepository<BookingRequest, Long> {

    @Query("""
        select b from BookingRequest b join fetch b.unit u join fetch u.customer
        where b.organizationId = :org and b.status in :statuses
        order by b.createdAt desc
        """)
    List<BookingRequest> findWithUnit(@Param("org") Long organizationId,
                                      @Param("statuses") Collection<BookingRequest.Status> statuses);

    @Query("""
        select b from BookingRequest b join fetch b.unit u join fetch u.customer
        where b.id = :id and b.organizationId = :org
        """)
    Optional<BookingRequest> findInOrganization(@Param("id") Long id, @Param("org") Long organizationId);

    List<BookingRequest> findByUnitIdOrderByCreatedAtDesc(Long unitId);

    List<BookingRequest> findByUnitIdInAndStatusIn(Collection<Long> unitIds, Collection<BookingRequest.Status> statuses);

    Optional<BookingRequest> findFirstByUnitIdAndStatusInOrderByCreatedAtDesc(Long unitId,
                                                                              Collection<BookingRequest.Status> statuses);

    long countByOrganizationIdAndStatus(Long organizationId, BookingRequest.Status status);
}
