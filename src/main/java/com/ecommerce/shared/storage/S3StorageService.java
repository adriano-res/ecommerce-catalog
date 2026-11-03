package com.ecommerce.shared.storage;

import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/** S3-compatible implementation of {@link StorageService} backed by AWS SDK v2. */
@Service
public class S3StorageService implements StorageService {

  private static final Logger log = LoggerFactory.getLogger(S3StorageService.class);

  private final S3Client s3Client;
  private final StorageProperties properties;

  public S3StorageService(S3Client s3Client, StorageProperties properties) {
    this.s3Client = s3Client;
    this.properties = properties;
  }

  @Override
  public String upload(String key, InputStream content, long contentLength, String contentType) {
    PutObjectRequest putRequest =
        PutObjectRequest.builder()
            .bucket(properties.bucket())
            .key(key)
            .contentType(contentType)
            .contentLength(contentLength)
            .build();

    s3Client.putObject(putRequest, RequestBody.fromInputStream(content, contentLength));
    log.info(
        "Uploaded object to S3: bucket={}, key={}, size={}, contentType={}",
        properties.bucket(),
        key,
        contentLength,
        contentType);

    return getPublicUrl(key);
  }

  @Override
  public void delete(String key) {
    try {
      DeleteObjectRequest deleteRequest =
          DeleteObjectRequest.builder().bucket(properties.bucket()).key(key).build();
      s3Client.deleteObject(deleteRequest);
      log.info("Deleted object from S3: bucket={}, key={}", properties.bucket(), key);
    } catch (Exception e) {
      log.warn(
          "Failed to delete object from S3: bucket={}, key={}. Reason: {}",
          properties.bucket(),
          key,
          e.getMessage());
      throw new RuntimeException("Failed to delete object from S3: " + key, e);
    }
  }

  @Override
  public String getPublicUrl(String key) {
    String cleanKey = key.startsWith("/") ? key.substring(1) : key;
    String base;
    if (properties.publicUrlBase() != null && !properties.publicUrlBase().isBlank()) {
      base = properties.publicUrlBase().trim();
    } else if (properties.endpoint() != null && !properties.endpoint().isBlank()) {
      base = properties.endpoint().trim();
    } else {
      base =
          "https://" + properties.bucket() + ".s3." + properties.region() + ".amazonaws.com";
      return base + "/" + cleanKey;
    }

    if (base.endsWith("/")) {
      base = base.substring(0, base.length() - 1);
    }
    return base + "/" + properties.bucket() + "/" + cleanKey;
  }
}
