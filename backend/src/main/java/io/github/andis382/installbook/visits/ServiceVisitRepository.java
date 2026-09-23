package io.github.andis382.installbook.visits;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceVisitRepository extends JpaRepository<ServiceVisit, Long> {

    List<ServiceVisit> findByUnitIdOrderByVisitedOnDescIdDesc(Long unitId);
}
