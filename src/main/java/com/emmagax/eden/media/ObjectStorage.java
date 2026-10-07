package com.emmagax.eden.media;

import java.util.List;
import java.util.Map;

public interface ObjectStorage {
  record SignedUpload(String url, Map<String, List<String>> headers) {}
  SignedUpload signUpload(MediaAsset asset);
}
