package io.github.andis382.installbook.units;

import io.github.andis382.installbook.customers.Customer;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/**
 * The register's search and filters as one JPA specification. Free text matches the serial,
 * brand, model, address, customer name and phone; the service filter is relative to today and
 * the installer's reminder lead time, exactly like the status pills on screen.
 */
public record UnitSearch(String text, UnitType type, Warranty warranty, Service service, Status status) {

    public enum Warranty { ACTIVE, EXPIRED }

    public enum Service { OVERDUE, DUE, OK }

    public enum Status { ACTIVE, REMOVED, ALL }

    public Specification<Unit> toSpecification(Long organizationId, LocalDate today, int leadDays) {
        return (root, query, cb) -> {
            List<Predicate> where = new ArrayList<>();
            where.add(cb.equal(root.get("organizationId"), organizationId));
            Status s = status == null ? Status.ACTIVE : status;
            if (s != Status.ALL) {
                where.add(cb.equal(root.get("status"), s == Status.ACTIVE ? UnitStatus.ACTIVE : UnitStatus.REMOVED));
            }
            if (type != null) {
                where.add(cb.equal(root.get("type"), type));
            }
            if (warranty == Warranty.ACTIVE) {
                where.add(cb.greaterThanOrEqualTo(root.get("warrantyUntil"), today));
            } else if (warranty == Warranty.EXPIRED) {
                where.add(cb.lessThan(root.get("warrantyUntil"), today));
            }
            if (service != null) {
                where.add(cb.equal(root.get("status"), UnitStatus.ACTIVE));
                switch (service) {
                    case OVERDUE -> where.add(cb.lessThan(root.get("nextServiceDue"), today));
                    case DUE -> where.add(cb.between(root.get("nextServiceDue"), today, today.plusDays(leadDays)));
                    case OK -> where.add(cb.greaterThan(root.get("nextServiceDue"), today.plusDays(leadDays)));
                }
            }
            String q = text == null ? "" : text.trim().toLowerCase();
            if (!q.isEmpty()) {
                Join<Unit, Customer> customer = root.join("customer", JoinType.INNER);
                String like = "%" + q.replace("%", "").replace("_", "") + "%";
                List<Predicate> any = new ArrayList<>(List.of(
                    cb.like(cb.lower(root.get("serialNumber")), like),
                    cb.like(cb.lower(root.get("brand")), like),
                    cb.like(cb.lower(root.get("model")), like),
                    cb.like(cb.lower(root.get("address")), like),
                    cb.like(cb.lower(customer.get("name")), like)));
                String digits = q.replaceAll("\\D", "");
                if (digits.length() >= 3) {
                    // people type "069 123..." while we store "35569123..."
                    String local = digits.startsWith("0") ? digits.substring(1) : digits;
                    any.add(cb.like(customer.get("phone"), "%" + local + "%"));
                }
                where.add(cb.or(any.toArray(Predicate[]::new)));
            }
            return cb.and(where.toArray(Predicate[]::new));
        };
    }
}
