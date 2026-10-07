package com.emmagax.eden.media;

import com.emmagax.eden.config.AccountAccess;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/media")
@ConditionalOnProperty(name="eden.media.enabled", havingValue="true")
public class MediaController {
  public record UploadRequest(
      @NotBlank @Pattern(regexp="audio/(wav|mpeg|flac)") String contentType,
      @Min(1) @Max(104857600) long sizeBytes,
      @NotBlank @Pattern(regexp="[0-9a-f]{64}") String sha256,
      @Min(5) @Max(60) Integer previewSeconds) {}
  public record UploadResponse(UUID id, String uploadUrl, Map<String, List<String>> headers, int expiresInSeconds) {}
  public record StatusResponse(UUID id, String state, String failureCode, Double durationSeconds) {}
  private final MediaRepository repository;
  private final ObjectStorage storage;
  private final AccountAccess access;
  public MediaController(MediaRepository repository, ObjectStorage storage, AccountAccess access) {
    this.repository = repository; this.storage = storage; this.access = access;
  }
  @PostMapping("/uploads")
  @ResponseStatus(HttpStatus.CREATED)
  public UploadResponse create(@Valid @RequestBody UploadRequest request, Authentication authentication) {
    long owner = access.current(authentication).getId();
    UUID id = UUID.randomUUID();
    var asset = new MediaAsset(id, owner, "uploads/" + owner + "/" + id, request.contentType(),
        request.sizeBytes(), request.sha256(), request.previewSeconds(), "UPLOADING", 0, null, null, null, null, null);
    var signed = storage.signUpload(asset);
    repository.insert(asset);
    return new UploadResponse(id, signed.url(), signed.headers(), 600);
  }
  @GetMapping("/{id}")
  public StatusResponse get(@PathVariable UUID id, Authentication authentication) { return status(owned(id, authentication)); }
  private MediaAsset owned(UUID id, Authentication authentication) {
    long owner = access.current(authentication).getId();
    var asset = repository.find(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (asset.ownerId() != owner) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    return asset;
  }
  private StatusResponse status(MediaAsset asset) {
    return new StatusResponse(asset.id(), asset.state(), asset.failureCode(), asset.durationSeconds());
  }
}
