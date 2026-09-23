package io.github.andis382.installbook.files;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** A photo or document on disk, owned by one organisation. */
@Entity
@Table(name = "stored_files")
public class StoredFile {

    @Id
    private String id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "original_name")
    private String originalName;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "storage_path", nullable = false)
    private String storagePath;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected StoredFile() {}

    public StoredFile(String id, Long organizationId, String originalName, String contentType, long sizeBytes, String storagePath) {
        this.id = id;
        this.organizationId = organizationId;
        this.originalName = originalName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.storagePath = storagePath;
    }

    public boolean isImage() {
        return contentType.startsWith("image/");
    }

    public String getId() { return id; }
    public Long getOrganizationId() { return organizationId; }
    public String getOriginalName() { return originalName; }
    public String getContentType() { return contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public String getStoragePath() { return storagePath; }
    public Instant getCreatedAt() { return createdAt; }
}
