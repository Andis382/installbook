package io.github.andis382.installbook.messaging;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InboundMessageRepository extends JpaRepository<InboundMessage, Long> {

    boolean existsByProviderMessageId(String providerMessageId);

    List<InboundMessage> findByOrganizationIdOrderByReceivedAtDesc(Long organizationId, Pageable page);

    List<InboundMessage> findByOrganizationIdAndRelatedTypeAndRelatedIdOrderByReceivedAtDesc(Long organizationId,
                                                                                             String relatedType, Long relatedId);

    List<InboundMessage> findByOrganizationIdAndFromPhoneOrderByReceivedAtDesc(Long organizationId, String fromPhone,
                                                                               Pageable page);
}
