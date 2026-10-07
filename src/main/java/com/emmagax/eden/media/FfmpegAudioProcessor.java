package com.emmagax.eden.media;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name="eden.media.enabled", havingValue="true")
public class FfmpegAudioProcessor implements AudioProcessor {
  private final String ffmpeg;
  private final String ffprobe;
  public FfmpegAudioProcessor(@Value("${eden.media.ffmpeg:ffmpeg}") String ffmpeg,
      @Value("${eden.media.ffprobe:ffprobe}") String ffprobe) {
    this.ffmpeg = ffmpeg; this.ffprobe = ffprobe;
  }
  public Output process(MediaAsset asset, Path input, Path directory) throws Exception {
    MediaValidator.validate(asset, input);
    var probe = directory.resolve("probe.txt");
    run(List.of(ffprobe, "-v", "error", "-protocol_whitelist", "file,pipe", "-f", demuxer(asset.contentType()), "-select_streams", "a:0",
        "-show_entries", "stream=codec_type:format=duration", "-of", "default=noprint_wrappers=1", input.toString()), probe, 20);
    var info = Files.readString(probe);
    if (!info.contains("codec_type=audio")) throw new MediaValidationException("INVALID_AUDIO");
    double duration;
    try {
      duration = Double.parseDouble(info.lines().filter(line -> line.startsWith("duration="))
          .findFirst().orElseThrow().substring(9));
    } catch (RuntimeException exception) { throw new MediaValidationException("INVALID_DURATION"); }
    if (!Double.isFinite(duration) || duration <= 0 || duration > 7200)
      throw new MediaValidationException("INVALID_DURATION");
    var stream = directory.resolve("stream.mp3");
    encode(input, stream, asset.contentType(), null, directory.resolve("encode.log"));
    Path preview = null;
    if (asset.previewSeconds() != null) {
      preview = directory.resolve("preview.mp3");
      encode(input, preview, asset.contentType(), asset.previewSeconds(), directory.resolve("preview.log"));
    }
    return new Output(stream, preview, duration);
  }
  private String demuxer(String contentType) {
    return switch (contentType) {
      case "audio/wav" -> "wav";
      case "audio/mpeg" -> "mp3";
      case "audio/flac" -> "flac";
      default -> throw new MediaValidationException("TYPE_MISMATCH");
    };
  }
  private void encode(Path input, Path output, String contentType, Integer seconds, Path log) throws Exception {
    var command = new ArrayList<>(List.of(ffmpeg, "-nostdin", "-v", "error", "-y", "-protocol_whitelist", "file,pipe",
        "-f", demuxer(contentType), "-i", input.toString(), "-map", "0:a:0", "-vn", "-map_metadata", "-1", "-ac", "2", "-ar", "44100",
        "-c:a", "libmp3lame", "-b:a", "192k"));
    if (seconds != null) command.addAll(List.of("-t", seconds.toString()));
    command.add(output.toString());
    run(command, log, 240);
    if (!Files.exists(output) || Files.size(output) == 0) throw new MediaValidationException("INVALID_AUDIO");
  }
  private void run(List<String> command, Path log, int timeoutSeconds) throws Exception {
    var process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile()).start();
    try {
      if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) throw new IllegalStateException("PROCESS_TIMEOUT");
      if (process.exitValue() != 0) throw new MediaValidationException("INVALID_AUDIO");
    } finally { if (process.isAlive()) process.destroyForcibly().waitFor(); }
  }
}
