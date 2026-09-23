package io.github.andis382.installbook.customers;

import io.github.andis382.installbook.units.UnitStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByIdAndOrganizationId(Long id, Long organizationId);

    Optional<Customer> findByOrganizationIdAndPhone(Long organizationId, String phone);

    /** Customers with the number of units still in service and their most recent install. */
    @Query("""
        select new io.github.andis382.installbook.customers.CustomerRow(
            c.id, c.name, c.phone, c.locale, c.whatsappOptIn, count(u.id), max(u.installedOn))
        from Customer c left join Unit u on u.customer = c and u.status = :active
        where c.organizationId = :org and (lower(c.name) like :text or c.phone like :digits)
        group by c.id, c.name, c.phone, c.locale, c.whatsappOptIn
        order by lower(c.name)
        """)
    List<CustomerRow> search(@Param("org") Long organizationId, @Param("text") String text, @Param("digits") String digits,
                             @Param("active") UnitStatus active, Pageable page);
}
