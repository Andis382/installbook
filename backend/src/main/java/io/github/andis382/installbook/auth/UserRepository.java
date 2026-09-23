package io.github.andis382.installbook.auth;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<User> findByOrganizationIdOrderByNameAsc(Long organizationId);

    Optional<User> findByIdAndOrganizationId(Long id, Long organizationId);
}
