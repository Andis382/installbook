package io.github.andis382.installbook.messaging;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboundMessageRepository extends JpaRepository<OutboundMessage, Long> {

    List<OutboundMessage> findByOrganizationIdOrderByCreatedAtDesc(Long organizationId, Pageable page);

    List<OutboundMessage> findByOrganizationIdAndRelatedTypeAndRelatedIdOrderByCreatedAtDesc(Long organizationId,
                                                                                            String relatedType, Long relatedId);

    Optional<OutboundMessage> findByProviderMessageId(String providerMessageId);

    /** The last thing we sent to a phone tells us which organisation a reply belongs to. */
    Optional<OutboundMessage> findTopByRecipientOrderByCreatedAtDesc(String recipient);

    List<OutboundMessage> findByRecipientAndCreatedAtGreaterThanEqualOrderByCreatedAtAsc(String recipient, Instant since);

    long countByOrganizationIdAndCreatedAtGreaterThanEqual(Long organizationId, Instant since);

    List<OutboundMessage> findByOrganizationIdAndRecipientOrderByCreatedAtDesc(Long organizationId, String recipient,
                                                                               Pageable page);
}
