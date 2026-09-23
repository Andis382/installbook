package io.github.andis382.installbook.files;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoredFileRepository extends JpaRepository<StoredFile, String> {

    Optional<StoredFile> findByIdAndOrganizationId(String id, Long organizationId);
}
