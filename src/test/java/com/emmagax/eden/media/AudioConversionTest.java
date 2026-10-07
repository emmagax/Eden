package com.emmagax.eden.media;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.UUID;
import java.util.HexFormat;
import java.security.MessageDigest;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="EDEN_TEST_FFMPEG", matches=".+")
class AudioConversionTest {
  @TempDir Path directory;
  @Test void generatesPlayableMp3AndTruncatedPreviewFromRealWav() throws Exception {
    String ffmpeg = System.getenv("EDEN_TEST_FFMPEG"), ffprobe = System.getenv("EDEN_TEST_FFPROBE");
    var input = directory.resolve("source.wav");
    var generate = new ProcessBuilder(ffmpeg, "-v", "error", "-f", "lavfi", "-i", "sine=frequency=440:duration=8",
        input.toString()).redirectErrorStream(true).redirectOutput(directory.resolve("generate.log").toFile()).start();
    assertTrue(generate.waitFor(20, TimeUnit.SECONDS)); assertEquals(0, generate.exitValue());
    byte[] bytes = Files.readAllBytes(input);
    String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    var asset = new MediaAsset(UUID.randomUUID(), 1, "source", "audio/wav", bytes.length, hash, 5,
        "PROCESSING", 1, UUID.randomUUID(), null, null, null, null);
    var output = new FfmpegAudioProcessor(ffmpeg, ffprobe).process(asset, input, directory);
    assertEquals(8, output.duration(), 0.1);
    assertTrue(Files.size(output.stream()) > Files.size(output.preview()));
    var probeLog = directory.resolve("preview-duration.txt");
    var probe = new ProcessBuilder(ffprobe, "-v", "error", "-show_entries", "format=duration", "-of",
        "default=noprint_wrappers=1:nokey=1", output.preview().toString()).redirectOutput(probeLog.toFile()).start();
    assertTrue(probe.waitFor(20, TimeUnit.SECONDS)); assertEquals(0, probe.exitValue());
    assertEquals(5, Double.parseDouble(Files.readString(probeLog).trim()), 0.2);
  }
}
