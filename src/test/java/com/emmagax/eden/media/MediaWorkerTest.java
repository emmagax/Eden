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

class MediaWorkerTest {
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
    FfmpegAudioProcessor.validate(asset("audio/wav", bytes.length, hash), file);
    assertThrows(MediaValidationException.class, () -> FfmpegAudioProcessor.validate(asset("audio/flac", bytes.length, hash), file));
    assertThrows(MediaValidationException.class, () -> FfmpegAudioProcessor.validate(asset("audio/wav", bytes.length, "0".repeat(64)), file));
    assertThrows(MediaValidationException.class, () -> FfmpegAudioProcessor.validate(asset("audio/wav", 1, hash), file));
  }
  @Test void publishesOnlyAfterBothOutputsAreStoredAndCleansTemporaryFiles() throws Exception {
    var repository = mock(MediaRepository.class);
    var storage = mock(ObjectStorage.class);
    var processor = mock(AudioProcessor.class);
    var asset = asset("audio/wav", 10, "0".repeat(64));
    Path[] temporary = new Path[1];
    when(processor.process(eq(asset), any(), any())).thenAnswer(invocation -> {
      Path dir = invocation.getArgument(2); temporary[0] = dir;
      var stream = Files.write(dir.resolve("stream.mp3"), new byte[]{1});
      var preview = Files.write(dir.resolve("preview.mp3"), new byte[]{2});
      return new AudioProcessor.Output(stream, preview, 60);
    });
    new MediaWorker(repository, storage, processor).process(asset);
    var order = inOrder(storage, repository);
    order.verify(storage).download(eq(asset.uploadKey()), any(), eq(10L));
    order.verify(storage).upload(endsWith("stream.mp3"), any(), eq("audio/mpeg"));
    order.verify(storage).upload(endsWith("preview.mp3"), any(), eq("audio/mpeg"));
    order.verify(repository).ready(eq(asset), anyString(), anyString(), eq(60.0));
    assertFalse(Files.exists(temporary[0]));
  }
  @Test void invalidMediaFailsPermanentlyWithoutPlayback() throws Exception {
    var repository = mock(MediaRepository.class); var storage = mock(ObjectStorage.class); var processor = mock(AudioProcessor.class);
    var asset = asset("audio/wav", 10, "0".repeat(64));
    when(processor.process(eq(asset), any(), any())).thenThrow(new MediaValidationException("CHECKSUM_MISMATCH"));
    new MediaWorker(repository, storage, processor).process(asset);
    verify(repository).fail(asset, "CHECKSUM_MISMATCH", false);
    verify(repository, never()).ready(any(), any(), any(), anyDouble());
    verify(storage, never()).upload(any(), any(), any());
  }
  @Test void transientStorageFailureIsRetryable() throws Exception {
    var repository = mock(MediaRepository.class); var storage = mock(ObjectStorage.class); var processor = mock(AudioProcessor.class);
    var asset = asset("audio/wav", 10, "0".repeat(64));
    doThrow(new java.io.IOException()).when(storage).download(any(), any(), anyLong());
    new MediaWorker(repository, storage, processor).process(asset);
    verify(repository).fail(asset, "PROCESSING_FAILED", true);
  }
}
