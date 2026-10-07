package com.emmagax.eden.media;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.*;
import jakarta.annotation.PreDestroy;

@Component
@ConditionalOnProperty(name="eden.media.enabled", havingValue="true")
public class S3ObjectStorage implements ObjectStorage {
  private final S3Client client;
  private final S3Presigner signer;
  private final String bucket;

  public S3ObjectStorage(@Value("${eden.media.endpoint:}") String endpoint,
      @Value("${eden.media.region:us-east-1}") String region,
      @Value("${eden.media.bucket}") String bucket) {
    this.bucket = bucket;
    var configuration = S3Configuration.builder().pathStyleAccessEnabled(true).build();
    var credentials = DefaultCredentialsProvider.create();
    var clientBuilder = S3Client.builder().region(Region.of(region)).credentialsProvider(credentials).serviceConfiguration(configuration);
    var signerBuilder = S3Presigner.builder().region(Region.of(region)).credentialsProvider(credentials).serviceConfiguration(configuration);
    if (!endpoint.isBlank()) {
      clientBuilder.endpointOverride(URI.create(endpoint));
      signerBuilder.endpointOverride(URI.create(endpoint));
    }
    client = clientBuilder.build();
    signer = signerBuilder.build();
  }
  public SignedUpload signUpload(MediaAsset asset) {
    var put = PutObjectRequest.builder().bucket(bucket).key(asset.uploadKey())
        .contentType(asset.contentType()).contentLength(asset.sizeBytes()).build();
    var signed = signer.presignPutObject(PutObjectPresignRequest.builder()
        .signatureDuration(Duration.ofMinutes(10)).putObjectRequest(put).build());
    return new SignedUpload(signed.url().toString(), signed.signedHeaders());
  }
  public Metadata metadata(String key) {
    var head = client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
    return new Metadata(head.contentLength(), head.contentType());
  }
  public void download(String key, Path target, long maximumBytes) throws Exception {
    try (var input = client.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build());
         var output = Files.newOutputStream(target)) {
      long count = 0;
      byte[] buffer = new byte[8192];
      int read;
      while ((read = input.read(buffer)) != -1) {
        count += read;
        if (count > maximumBytes) throw new MediaValidationException("SIZE_MISMATCH");
        output.write(buffer, 0, read);
      }
    }
  }
  public void upload(String key, Path source, String contentType) {
    client.putObject(PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).build(), RequestBody.fromFile(source));
  }
  public String signDownload(String key) {
    return signer.presignGetObject(GetObjectPresignRequest.builder().signatureDuration(Duration.ofMinutes(5))
        .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(key).build()).build()).url().toString();
  }
  @PreDestroy public void close() { client.close(); signer.close(); }
}
