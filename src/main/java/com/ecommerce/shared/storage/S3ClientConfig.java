package com.ecommerce.shared.storage;

import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.S3Exception;

/** Configuration class initializing the AWS SDK v2 S3Client and bucket verification runner. */
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class S3ClientConfig {

  private static final Logger log = LoggerFactory.getLogger(S3ClientConfig.class);

  @Bean
  public S3Client s3Client(StorageProperties properties) {
    String accessKey = properties.accessKey() != null ? properties.accessKey() : "anyadmin";
    String secretKey = properties.secretKey() != null ? properties.secretKey() : "anyadmin";
    String region = properties.region() != null ? properties.region() : "us-east-1";

    S3ClientBuilder builder =
        S3Client.builder()
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey)))
            .region(Region.of(region))
            .serviceConfiguration(
                S3Configuration.builder().pathStyleAccessEnabled(true).build());

    if (properties.endpoint() != null && !properties.endpoint().isBlank()) {
      builder.endpointOverride(URI.create(properties.endpoint()));
    }

    return builder.build();
  }

  @Bean
  public ApplicationRunner s3BucketInitializer(S3Client s3Client, StorageProperties properties) {
    return args -> {
      String bucket = properties.bucket();
      try {
        try {
          s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
          log.info("S3 bucket '{}' already exists", bucket);
        } catch (NoSuchBucketException e) {
          s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
          log.info("Created S3 bucket '{}'", bucket);
        } catch (S3Exception e) {
          if (e.statusCode() == 404) {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            log.info("Created S3 bucket '{}' after 404", bucket);
          } else {
            log.warn("S3 headBucket returned status {}: {}", e.statusCode(), e.getMessage());
          }
        }
      } catch (Exception e) {
        log.warn("Could not verify or create S3 bucket '{}': {}", bucket, e.getMessage());
      }
    };
  }
}
