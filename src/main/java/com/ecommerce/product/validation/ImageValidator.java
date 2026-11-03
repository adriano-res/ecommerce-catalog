package com.ecommerce.product.validation;

import com.ecommerce.shared.error.DomainException;
import com.ecommerce.shared.error.FailureCode;
import com.ecommerce.shared.storage.StorageProperties;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/** Validates image payloads for catalog products according to format, mime type, and size limits. */
@Component
public class ImageValidator {

  private static final Set<String> ALLOWED_MIME_TYPES =
      Set.of("image/jpeg", "image/png", "image/webp");

  private static final Set<String> ALLOWED_EXTENSIONS =
      Set.of("jpg", "jpeg", "png", "webp");

  private final StorageProperties storageProperties;

  public ImageValidator(StorageProperties storageProperties) {
    this.storageProperties = storageProperties;
  }

  /**
   * Validates the multipart image payload and returns the normalized file extension.
   *
   * @param file uploaded multipart file
   * @return normalized extension (e.g. "png", "jpg", "webp")
   * @throws DomainException if payload fails any validation criteria
   */
  public String validateAndExtractExtension(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new DomainException(FailureCode.INVALID_PAYLOAD, "Image file must not be empty or null");
    }

    if (file.getSize() > storageProperties.maxSizeBytes()) {
      throw new DomainException(
          FailureCode.INVALID_PAYLOAD,
          "Image file size "
              + file.getSize()
              + " bytes exceeds maximum allowed size of "
              + storageProperties.maxSizeBytes()
              + " bytes");
    }

    String contentType = file.getContentType();
    if (contentType == null
        || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
      throw new DomainException(
          FailureCode.INVALID_PAYLOAD,
          "Invalid image MIME type: "
              + contentType
              + ". Allowed types: "
              + ALLOWED_MIME_TYPES);
    }

    String originalFilename = file.getOriginalFilename();
    if (originalFilename == null
        || originalFilename.isBlank()
        || !originalFilename.contains(".")) {
      throw new DomainException(
          FailureCode.INVALID_PAYLOAD, "Image file name must have a valid extension");
    }

    String extension =
        originalFilename
            .substring(originalFilename.lastIndexOf('.') + 1)
            .toLowerCase(Locale.ROOT);
    if (!ALLOWED_EXTENSIONS.contains(extension)) {
      throw new DomainException(
          FailureCode.INVALID_PAYLOAD,
          "Invalid image extension '."
              + extension
              + "'. Allowed extensions: "
              + ALLOWED_EXTENSIONS);
    }

    return extension;
  }
}
