package com.emmagax.eden.media;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@ConditionalOnProperty(name="eden.media.processing-enabled", havingValue="true")
public class MediaWorker {
  private static final Logger LOG = LoggerFactory.getLogger(MediaWorker.class);
  private final MediaRepository repository;
  private final ObjectStorage storage;
  private final AudioProcessor processor;
  public MediaWorker(MediaRepository repository, ObjectStorage storage, AudioProcessor processor) {
    this.repository = repository; this.storage = storage; this.processor = processor;
  }
  @Scheduled(fixedDelayString="${eden.media.poll-ms:5000}")
  public void poll() { repository.claim().ifPresent(this::process); }

  void process(MediaAsset asset) {
    Path directory = null;
    try {
      directory = Files.createTempDirectory("eden-audio-");
      var input = directory.resolve("input");
      storage.download(asset.uploadKey(), input, asset.sizeBytes());
      var output = processor.process(asset, input, directory);
      // Each lease writes distinct keys. A stale worker cannot overwrite the winning output.
      String base = "processed/" + asset.id() + "/" + asset.leaseToken();
      String stream = base + "/stream.mp3";
      String preview = output.preview() == null ? null : base + "/preview.mp3";
      storage.upload(stream, output.stream(), "audio/mpeg");
      if (preview != null) storage.upload(preview, output.preview(), "audio/mpeg");
      repository.ready(asset, stream, preview, output.duration());
    } catch (MediaValidationException exception) {
      repository.fail(asset, exception.getMessage(), false);
    } catch (Exception exception) {
      repository.fail(asset, "PROCESSING_FAILED", true);
      LOG.warn("Media processing failed for {} ({})", asset.id(), exception.getClass().getSimpleName());
      if (exception instanceof InterruptedException) Thread.currentThread().interrupt();
    } finally {
      if (directory != null) {
        try (var files = Files.walk(directory)) {
          for (var file : files.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(file);
        } catch (Exception exception) { LOG.warn("Unable to clean media temporary directory"); }
      }
    }
  }
}
