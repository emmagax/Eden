package com.emmagax.eden.media;

import java.nio.file.Path;

public interface AudioProcessor {
  record Output(Path stream, Path preview, double duration) {}
  Output process(MediaAsset asset, Path input, Path directory) throws Exception;
}
