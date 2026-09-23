package io.github.andis382.installbook.files;

import io.github.andis382.installbook.common.ApiException;
import io.github.andis382.installbook.config.AppProperties;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Keeps uploads on local disk under app.storage-dir, one folder per month. */
@Service
public class FileStorage {

    public static final Set<String> IMAGES = Set.of("image/jpeg", "image/png", "image/webp", "image/heic", "image/heif");
    public static final Set<String> DOCUMENTS = Set.of("application/pdf");
    public static final Set<String> AUDIO = Set.of("audio/webm", "audio/ogg", "audio/mpeg", "audio/mp4", "audio/wav", "audio/x-m4a", "audio/aac");

    private static final Map<String, String> EXTENSIONS = Map.ofEntries(
        Map.entry("image/jpeg", "jpg"), Map.entry("image/png", "png"), Map.entry("image/webp", "webp"),
        Map.entry("image/heic", "heic"), Map.entry("image/heif", "heif"), Map.entry("application/pdf", "pdf"),
        Map.entry("audio/webm", "webm"), Map.entry("audio/ogg", "ogg"), Map.entry("audio/mpeg", "mp3"),
        Map.entry("audio/mp4", "m4a"), Map.entry("audio/wav", "wav"), Map.entry("audio/x-m4a", "m4a"), Map.entry("audio/aac", "aac"));

    private final StoredFileRepository files;
    private final Path root;

    public FileStorage(StoredFileRepository files, AppProperties props) {
        this.files = files;
        this.root = Path.of(props.getStorageDir()).toAbsolutePath().normalize();
    }

    @Transactional
    public StoredFile store(Long organizationId, MultipartFile upload, Set<String> allowedTypes) {
        if (upload == null || upload.isEmpty()) {
            throw ApiException.field("file", "file.missing");
        }
        String type = baseType(upload.getContentType());
        if (!allowedTypes.contains(type)) {
            throw ApiException.field("file", "file.type_not_allowed");
        }
        try {
            return store(organizationId, upload.getBytes(), type, upload.getOriginalFilename());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Transactional
    public StoredFile store(Long organizationId, byte[] bytes, String contentType, String originalName) {
        String id = UUID.randomUUID().toString();
        LocalDate today = LocalDate.now();
        String relative = "%d/%02d/%s.%s".formatted(today.getYear(), today.getMonthValue(), id,
            EXTENSIONS.getOrDefault(baseType(contentType), "bin"));
        Path target = root.resolve(relative).normalize();
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, bytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return files.save(new StoredFile(id, organizationId, originalName, baseType(contentType), bytes.length, relative));
    }

    public Resource load(StoredFile file) {
        Path path = root.resolve(file.getStoragePath()).normalize();
        if (!path.startsWith(root) || !Files.exists(path)) {
            throw ApiException.notFound();
        }
        return new FileSystemResource(path);
    }

    public byte[] bytes(StoredFile file) {
        try {
            return Files.readAllBytes(root.resolve(file.getStoragePath()).normalize());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String baseType(String contentType) {
        if (contentType == null) {
            return "application/octet-stream";
        }
        int semicolon = contentType.indexOf(';');
        return (semicolon >= 0 ? contentType.substring(0, semicolon) : contentType).trim().toLowerCase();
    }
}
