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
  public boolean queue(UUID id) {
    return jdbc.update("UPDATE media_assets SET state='QUEUED', updated_at=now() WHERE id=? AND state='UPLOADING'", id) == 1;
  }
  public Optional<MediaAsset> claim() {
    // A single atomic statement prevents two workers from claiming the same job.
    UUID token = UUID.randomUUID();
    jdbc.update("UPDATE media_assets SET state='FAILED', failure_code='ATTEMPTS_EXHAUSTED', updated_at=now() WHERE state='PROCESSING' AND lease_until < now() AND attempts >= 3");
    return jdbc.query("""
        WITH candidate AS (
          SELECT id FROM media_assets
          WHERE (state='QUEUED' OR (state='PROCESSING' AND lease_until < now())) AND attempts < 3
          ORDER BY created_at FOR UPDATE SKIP LOCKED LIMIT 1
        )
        UPDATE media_assets m SET state='PROCESSING', attempts=attempts+1,
          lease_token=?, lease_until=now()+interval '10 minutes', updated_at=now(), failure_code=NULL
        FROM candidate c WHERE m.id=c.id RETURNING m.*
        """, MAPPER, token).stream().findFirst();
  }
  public boolean ready(MediaAsset asset, String stream, String preview, double duration) {
    return jdbc.update("""
        UPDATE media_assets SET state='READY',stream_key=?,preview_key=?,duration_seconds=?,
          lease_until=NULL,updated_at=now() WHERE id=? AND state='PROCESSING' AND lease_token=? AND lease_until > now()
        """, stream, preview, duration, asset.id(), asset.leaseToken()) == 1;
  }
  public void fail(MediaAsset asset, String code, boolean retryable) {
    jdbc.update("""
        UPDATE media_assets SET state=?,failure_code=?,lease_until=NULL,updated_at=now()
        WHERE id=? AND state='PROCESSING' AND lease_token=? AND lease_until > now()
        """, retryable && asset.attempts() < 3 ? "QUEUED" : "FAILED", code, asset.id(), asset.leaseToken());
  }
}
