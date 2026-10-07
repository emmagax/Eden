package com.emmagax.eden.media;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class MediaValidationTest {
  @TempDir Path directory;
  MediaAsset asset(String type, long size, String hash) {
    return new MediaAsset(UUID.randomUUID(), 1, "uploads/source", type, size, hash, 30,
        "PROCESSING", 1, UUID.randomUUID(), null, null, null, null);
  }
  @Test void validatesBytesAndChecksumInsteadOfTrustingDeclaredMime() throws Exception {
    var file = directory.resolve("test.wav");
    byte[] bytes = "RIFFxxxxWAVEdata".getBytes();
    Files.write(file, bytes);
    String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    MediaValidator.validate(asset("audio/wav", bytes.length, hash), file);
    assertThrows(MediaValidationException.class, () -> MediaValidator.validate(asset("audio/flac", bytes.length, hash), file));
    assertThrows(MediaValidationException.class, () -> MediaValidator.validate(asset("audio/wav", bytes.length, "0".repeat(64)), file));
    assertThrows(MediaValidationException.class, () -> MediaValidator.validate(asset("audio/wav", 1, hash), file));
  }
}
