package com.emmagax.eden.media;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface ObjectStorage {
  record SignedUpload(String url, Map<String, List<String>> headers) {}
  record Metadata(long size, String contentType) {}
  SignedUpload signUpload(MediaAsset asset);
  Metadata metadata(String key);
  void download(String key, Path target, long maximumBytes) throws Exception;
}
