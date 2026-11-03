package com.ecommerce.shared.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration properties mapping S3-compatible object storage settings. */
@ConfigurationProperties(prefix = "app.storage.s3")
public record StorageProperties(
    String endpoint,
    String bucket,
    String accessKey,
    String secretKey,
    String region,
    long maxSizeBytes,
    String publicUrlBase) {

  private static final long DEFAULT_MAX_SIZE_BYTES = 5L * 1024 * 1024; // 5 MB

  public StorageProperties {
    if (bucket == null || bucket.isBlank()) {
      bucket = "ecommerce-catalog";
    }
    if (region == null || region.isBlank()) {
      region = "us-east-1";
    }
    if (maxSizeBytes <= 0) {
      maxSizeBytes = DEFAULT_MAX_SIZE_BYTES;
    }
  }
}
