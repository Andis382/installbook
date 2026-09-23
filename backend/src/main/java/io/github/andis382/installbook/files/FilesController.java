package io.github.andis382.installbook.files;

import io.github.andis382.installbook.auth.CurrentUser;
import io.github.andis382.installbook.common.ApiException;
import io.github.andis382.installbook.common.SignedUrls;
import java.time.Duration;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FilesController {

    private final StoredFileRepository files;
    private final FileStorage storage;
    private final CurrentUser currentUser;
    private final SignedUrls signedUrls;

    public FilesController(StoredFileRepository files, FileStorage storage, CurrentUser currentUser, SignedUrls signedUrls) {
        this.files = files;
        this.storage = storage;
        this.currentUser = currentUser;
        this.signedUrls = signedUrls;
    }

    @GetMapping("/api/files/{id}")
    public ResponseEntity<Resource> download(@PathVariable String id) {
        StoredFile file = files.findByIdAndOrganizationId(id, currentUser.organizationId()).orElseThrow(ApiException::notFound);
        return serve(file, CacheControl.maxAge(Duration.ofDays(7)).cachePrivate());
    }

    /** For public pages (warranty card, quote, ticket): valid only with an unexpired signature. */
    @GetMapping("/api/public/files/{id}")
    public ResponseEntity<Resource> publicDownload(@PathVariable String id, @RequestParam long exp, @RequestParam String sig) {
        if (!signedUrls.verify(id, exp, sig)) {
            throw ApiException.forbidden();
        }
        StoredFile file = files.findById(id).orElseThrow(ApiException::notFound);
        return serve(file, CacheControl.maxAge(Duration.ofHours(1)).cachePrivate());
    }

    private ResponseEntity<Resource> serve(StoredFile file, CacheControl cache) {
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(file.getContentType()))
            .cacheControl(cache)
            .header("X-Content-Type-Options", "nosniff")
            .body(storage.load(file));
    }
}
