package com.emmagax.eden.media;

import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class MediaRepository {
  private final JdbcTemplate jdbc;
  public MediaRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
  private static final RowMapper<MediaAsset> MAPPER = (rs, row) -> new MediaAsset(
      rs.getObject("id", UUID.class), rs.getLong("owner_id"), rs.getString("upload_key"),
      rs.getString("content_type"), rs.getLong("size_bytes"), rs.getString("sha256"),
      rs.getObject("preview_seconds", Integer.class), rs.getString("state"), rs.getInt("attempts"),
      rs.getObject("lease_token", UUID.class), rs.getString("stream_key"), rs.getString("preview_key"),
      rs.getObject("duration_seconds", Double.class), rs.getString("failure_code"));

  public void insert(MediaAsset asset) {
    jdbc.update("INSERT INTO media_assets(id,owner_id,upload_key,content_type,size_bytes,sha256,preview_seconds) VALUES (?,?,?,?,?,?,?)",
        asset.id(), asset.ownerId(), asset.uploadKey(), asset.contentType(), asset.sizeBytes(), asset.sha256(), asset.previewSeconds());
  }
  public Optional<MediaAsset> find(UUID id) {
    return jdbc.query("SELECT * FROM media_assets WHERE id=?", MAPPER, id).stream().findFirst();
  }
}
