package io.github.andis382.installbook.auth;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    Optional<Invitation> findByToken(String token);

    List<Invitation> findByOrganizationIdAndAcceptedAtIsNullOrderByCreatedAtDesc(Long organizationId);

    Optional<Invitation> findByIdAndOrganizationId(Long id, Long organizationId);
}
