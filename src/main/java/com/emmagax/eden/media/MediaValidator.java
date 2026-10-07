package com.emmagax.eden.media;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

final class MediaValidator {
  static void validate(MediaAsset asset, Path file) throws Exception {
    if (Files.size(file) != asset.sizeBytes()) throw new MediaValidationException("SIZE_MISMATCH");
    var digest = MessageDigest.getInstance("SHA-256");
    byte[] prefix;
    try (var input = Files.newInputStream(file)) {
      prefix = input.readNBytes(12);
      digest.update(prefix);
      byte[] buffer = new byte[8192];
      int count;
      while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count);
    }
    if (!HexFormat.of().formatHex(digest.digest()).equals(asset.sha256()))
      throw new MediaValidationException("CHECKSUM_MISMATCH");
    boolean valid = switch (asset.contentType()) {
      case "audio/wav" -> starts(prefix, "RIFF") && prefix.length >= 12 && new String(prefix, 8, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("WAVE");
      case "audio/flac" -> starts(prefix, "fLaC");
      case "audio/mpeg" -> starts(prefix, "ID3") || (prefix.length >= 2 && (prefix[0] & 255) == 255 && (prefix[1] & 224) == 224);
      default -> false;
    };
    if (!valid) throw new MediaValidationException("TYPE_MISMATCH");
  }
  private static boolean starts(byte[] bytes, String text) {
    return bytes.length >= text.length() && new String(bytes, 0, text.length(), java.nio.charset.StandardCharsets.US_ASCII).equals(text);
  }
}
