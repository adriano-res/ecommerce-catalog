package com.ecommerce.shared.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

class S3StorageServiceTest {

  private S3Client s3Client;
  private StorageProperties properties;
  private S3StorageService storageService;

  @BeforeEach
  void setUp() {
    s3Client = mock(S3Client.class);
    properties =
        new StorageProperties(
            "http://localhost:8333",
            "ecommerce-catalog",
            "anyadmin",
            "anyadmin",
            "us-east-1",
            5242880L,
            null);
    storageService = new S3StorageService(s3Client, properties);
  }

  @Test
  @DisplayName("Should upload object and return public URL when valid input provided")
  void shouldUploadObjectAndReturnPublicUrl_whenValidInputProvided() {
    byte[] contentBytes = "test-image-content".getBytes();
    InputStream stream = new ByteArrayInputStream(contentBytes);
    String key = "products/prod-1/image-1.png";
    String contentType = "image/png";

    String publicUrl = storageService.upload(key, stream, contentBytes.length, contentType);

    ArgumentCaptor<PutObjectRequest> putRequestCaptor =
        ArgumentCaptor.forClass(PutObjectRequest.class);
    verify(s3Client).putObject(putRequestCaptor.capture(), any(RequestBody.class));

    PutObjectRequest capturedRequest = putRequestCaptor.getValue();
    assertThat(capturedRequest.bucket()).isEqualTo("ecommerce-catalog");
    assertThat(capturedRequest.key()).isEqualTo(key);
    assertThat(capturedRequest.contentType()).isEqualTo(contentType);
    assertThat(capturedRequest.contentLength()).isEqualTo((long) contentBytes.length);

    assertThat(publicUrl)
        .isEqualTo("http://localhost:8333/ecommerce-catalog/products/prod-1/image-1.png");
  }

  @Test
  @DisplayName("Should delete object when key is provided")
  void shouldDeleteObject_whenKeyProvided() {
    String key = "products/prod-1/image-1.png";

    storageService.delete(key);

    ArgumentCaptor<DeleteObjectRequest> deleteRequestCaptor =
        ArgumentCaptor.forClass(DeleteObjectRequest.class);
    verify(s3Client).deleteObject(deleteRequestCaptor.capture());

    DeleteObjectRequest capturedRequest = deleteRequestCaptor.getValue();
    assertThat(capturedRequest.bucket()).isEqualTo("ecommerce-catalog");
    assertThat(capturedRequest.key()).isEqualTo(key);
  }

  @Test
  @DisplayName("Should propagate exception when delete fails in S3")
  void shouldPropagateException_whenDeleteFailsInS3() {
    String key = "products/prod-1/image-1.png";
    doThrow(S3Exception.builder().message("S3 connection error").build())
        .when(s3Client)
        .deleteObject(any(DeleteObjectRequest.class));

    assertThatThrownBy(() -> storageService.delete(key))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Failed to delete object from S3");
  }

  @Test
  @DisplayName("Should format public URL correctly when custom publicUrlBase is configured")
  void shouldFormatPublicUrlCorrectly_whenCustomPublicUrlBaseIsConfigured() {
    StorageProperties customProperties =
        new StorageProperties(
            "http://localhost:8333",
            "ecommerce-catalog",
            "anyadmin",
            "anyadmin",
            "us-east-1",
            5242880L,
            "https://cdn.example.com");
    S3StorageService customService = new S3StorageService(s3Client, customProperties);

    String url = customService.getPublicUrl("products/123/photo.jpg");

    assertThat(url).isEqualTo("https://cdn.example.com/ecommerce-catalog/products/123/photo.jpg");
  }

  @Test
  @DisplayName("Should normalize leading and trailing slashes when building public URL")
  void shouldNormalizeLeadingAndTrailingSlashes_whenBuildingPublicUrl() {
    StorageProperties customProperties =
        new StorageProperties(
            "http://localhost:8333/",
            "ecommerce-catalog",
            "anyadmin",
            "anyadmin",
            "us-east-1",
            5242880L,
            "http://localhost:8333/");
    S3StorageService customService = new S3StorageService(s3Client, customProperties);

    String url = customService.getPublicUrl("/products/123/photo.jpg");

    assertThat(url).isEqualTo("http://localhost:8333/ecommerce-catalog/products/123/photo.jpg");
  }
}
