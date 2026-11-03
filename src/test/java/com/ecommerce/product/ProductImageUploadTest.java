package com.ecommerce.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.mockito.ArgumentMatchers.startsWith;

import com.ecommerce.category.CategoryService;
import com.ecommerce.outbox.OutboxApi;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.entity.ProductDocument;
import com.ecommerce.product.entity.ProductImage;
import com.ecommerce.product.entity.ProductStatus;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.product.repository.ProductSearchRepository;
import com.ecommerce.product.validation.AttributeValidator;
import com.ecommerce.product.validation.ImageValidator;
import com.ecommerce.shared.audit.AuditMetadata;
import com.ecommerce.shared.error.DomainException;
import com.ecommerce.shared.error.FailureCode;
import com.ecommerce.shared.money.Money;
import com.ecommerce.shared.storage.StorageProperties;
import com.ecommerce.shared.storage.StorageService;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.AuditorAware;
import org.springframework.mock.web.MockMultipartFile;

class ProductImageUploadTest {

  private ProductRepository productRepository;
  private ProductSearchRepository productSearchRepository;
  private CategoryService categoryService;
  private AttributeValidator attributeValidator;
  private AuditorAware<String> auditorAware;
  private StorageService storageService;
  private ImageValidator imageValidator;
  private StorageProperties storageProperties;
  private OutboxApi outboxApi;
  private ProductService productService;

  @BeforeEach
  void setUp() {
    productRepository = mock(ProductRepository.class);
    productSearchRepository = mock(ProductSearchRepository.class);
    categoryService = mock(CategoryService.class);
    attributeValidator = mock(AttributeValidator.class);
    auditorAware = mock(AuditorAware.class);
    storageService = mock(StorageService.class);
    outboxApi = mock(OutboxApi.class);

    storageProperties =
        new StorageProperties(
            "http://localhost:8333",
            "ecommerce-catalog",
            "anyadmin",
            "anyadmin",
            "us-east-1",
            5242880L,
            null);
    imageValidator = new ImageValidator(storageProperties);

    when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of("admin-tester"));

    productService =
        new ProductService(
            productRepository,
            productSearchRepository,
            categoryService,
            attributeValidator,
            auditorAware,
            storageService,
            imageValidator,
            outboxApi);
  }

  @Test
  @DisplayName("Should accept valid PNG and JPEG files and extract correct extensions")
  void shouldExtractExtension_whenValidImageFilesProvided() {
    MockMultipartFile pngFile =
        new MockMultipartFile("file", "image.png", "image/png", "fake-bytes".getBytes());
    MockMultipartFile jpgFile =
        new MockMultipartFile("file", "photo.jpeg", "image/jpeg", "fake-bytes".getBytes());
    MockMultipartFile webpFile =
        new MockMultipartFile("file", "banner.webp", "image/webp", "fake-bytes".getBytes());

    assertThat(imageValidator.validateAndExtractExtension(pngFile)).isEqualTo("png");
    assertThat(imageValidator.validateAndExtractExtension(jpgFile)).isEqualTo("jpeg");
    assertThat(imageValidator.validateAndExtractExtension(webpFile)).isEqualTo("webp");
  }

  @Test
  @DisplayName("Should reject upload when file format or MIME type is invalid")
  void shouldRejectUpload_whenFileHasInvalidFormatOrMime() {
    MockMultipartFile pdfFile =
        new MockMultipartFile("file", "doc.pdf", "application/pdf", "fake-bytes".getBytes());
    MockMultipartFile exeFile =
        new MockMultipartFile(
            "file", "script.exe", "application/x-msdownload", "fake-bytes".getBytes());
    MockMultipartFile spoofedFile =
        new MockMultipartFile("file", "image.exe", "image/png", "fake-bytes".getBytes());

    assertThatThrownBy(() -> imageValidator.validateAndExtractExtension(pdfFile))
        .isInstanceOf(DomainException.class)
        .extracting(ex -> ((DomainException) ex).getFailureCode())
        .isEqualTo(FailureCode.INVALID_PAYLOAD);

    assertThatThrownBy(() -> imageValidator.validateAndExtractExtension(exeFile))
        .isInstanceOf(DomainException.class)
        .extracting(ex -> ((DomainException) ex).getFailureCode())
        .isEqualTo(FailureCode.INVALID_PAYLOAD);

    assertThatThrownBy(() -> imageValidator.validateAndExtractExtension(spoofedFile))
        .isInstanceOf(DomainException.class)
        .extracting(ex -> ((DomainException) ex).getFailureCode())
        .isEqualTo(FailureCode.INVALID_PAYLOAD);
  }

  @Test
  @DisplayName("Should reject upload when file is empty or null")
  void shouldRejectUpload_whenFileIsEmptyOrNull() {
    MockMultipartFile emptyFile =
        new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

    assertThatThrownBy(() -> imageValidator.validateAndExtractExtension(emptyFile))
        .isInstanceOf(DomainException.class)
        .extracting(ex -> ((DomainException) ex).getFailureCode())
        .isEqualTo(FailureCode.INVALID_PAYLOAD);

    assertThatThrownBy(() -> imageValidator.validateAndExtractExtension(null))
        .isInstanceOf(DomainException.class)
        .extracting(ex -> ((DomainException) ex).getFailureCode())
        .isEqualTo(FailureCode.INVALID_PAYLOAD);
  }

  @Test
  @DisplayName("Should reject upload when file exceeds maximum allowed size")
  void shouldRejectUpload_whenFileExceedsMaximumSize() {
    byte[] oversizedContent = new byte[(int) storageProperties.maxSizeBytes() + 1];
    MockMultipartFile oversizedFile =
        new MockMultipartFile("file", "large.png", "image/png", oversizedContent);

    assertThatThrownBy(() -> imageValidator.validateAndExtractExtension(oversizedFile))
        .isInstanceOf(DomainException.class)
        .extracting(ex -> ((DomainException) ex).getFailureCode())
        .isEqualTo(FailureCode.INVALID_PAYLOAD);
  }

  @Test
  @DisplayName("Should upload image and add ProductImage to product when valid PNG provided")
  void shouldUploadImageAndAddToProduct_whenValidPngProvided() {
    String productId = "prod-100";
    ProductDocument product = createTestProduct(productId, "Smartphone X");

    when(productRepository.findById(productId)).thenReturn(Optional.of(product));
    when(productRepository.save(any(ProductDocument.class))).thenAnswer(inv -> inv.getArgument(0));

    String expectedPublicUrl =
        "http://localhost:8333/ecommerce-catalog/products/prod-100/uuid-test.png";
    when(storageService.upload(anyString(), any(InputStream.class), anyLong(), anyString()))
        .thenReturn(expectedPublicUrl);

    MockMultipartFile file =
        new MockMultipartFile("file", "test.png", "image/png", "fake-bytes".getBytes());

    ProductResponse response = productService.addImage(productId, file, "Frontal view");

    assertThat(response).isNotNull();
    assertThat(response.images()).hasSize(1);
    ProductImage addedImage = response.images().get(0);
    assertThat(addedImage.url()).isEqualTo(expectedPublicUrl);
    assertThat(addedImage.alt()).isEqualTo("Frontal view");

    verify(storageService).upload(anyString(), any(InputStream.class), eq(10L), eq("image/png"));
    verify(productRepository).save(product);
  }

  @Test
  @DisplayName("Should default alt text to product name when alt parameter is null or blank")
  void shouldDefaultAltTextToProductName_whenAltParamIsBlank() {
    String productId = "prod-200";
    ProductDocument product = createTestProduct(productId, "Laptop Pro 15");

    when(productRepository.findById(productId)).thenReturn(Optional.of(product));
    when(productRepository.save(any(ProductDocument.class))).thenAnswer(inv -> inv.getArgument(0));

    String expectedPublicUrl =
        "http://localhost:8333/ecommerce-catalog/products/prod-200/uuid-test.jpg";
    when(storageService.upload(anyString(), any(InputStream.class), anyLong(), anyString()))
        .thenReturn(expectedPublicUrl);

    MockMultipartFile file =
        new MockMultipartFile("file", "laptop.jpg", "image/jpeg", "fake-bytes".getBytes());

    ProductResponse response = productService.addImage(productId, file, "   ");

    assertThat(response.images()).hasSize(1);
    assertThat(response.images().get(0).alt()).isEqualTo("Laptop Pro 15");
  }

  @Test
  @DisplayName("Should remove image by imageId and persist outbox event when image exists")
  void shouldRemoveImageAndSaveOutboxEvent_whenImageExists() {
    String productId = "prod-300";
    ProductDocument product = createTestProduct(productId, "Headphones ANC");
    String imageUrl =
        "http://localhost:8333/ecommerce-catalog/products/prod-300/photo-abc.webp";
    product.setImages(new ArrayList<>(List.of(new ProductImage("img-123", imageUrl, "Angle 1"))));

    when(productRepository.findById(productId)).thenReturn(Optional.of(product));
    when(productRepository.save(any(ProductDocument.class))).thenAnswer(inv -> inv.getArgument(0));

    ProductResponse response = productService.removeImage(productId, "img-123");

    assertThat(response.images()).isEmpty();
    verify(productRepository).save(product);
    verify(storageService, never()).delete(anyString());
    verify(outboxApi).publish("DELETE_STORAGE_FILE", "products/prod-300/photo-abc.webp");
  }

  @Test
  @DisplayName("Should publish outbox deletion event when direct compensation fails after product save error")
  void shouldPublishOutboxEvent_whenDirectCompensationFailsAfterProductSaveError() {
    String productId = "prod-500";
    ProductDocument product = createTestProduct(productId, "Keyboard RGB");

    when(productRepository.findById(productId)).thenReturn(Optional.of(product));
    when(storageService.upload(anyString(), any(InputStream.class), anyLong(), anyString()))
        .thenReturn("http://localhost:8333/ecommerce-catalog/products/prod-500/key.png");
    when(productRepository.save(any(ProductDocument.class)))
        .thenThrow(new RuntimeException("Database error during product save"));
    doThrow(new RuntimeException("S3 error during compensation"))
        .when(storageService)
        .delete(anyString());

    MockMultipartFile file =
        new MockMultipartFile("file", "test.png", "image/png", "fake-bytes".getBytes());

    assertThatThrownBy(() -> productService.addImage(productId, file, "Keyboard"))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Database error during product save");

    verify(outboxApi).publish(eq("DELETE_STORAGE_FILE"), startsWith("products/prod-500/"));
  }

  @Test
  @DisplayName("Should throw RESOURCE_NOT_FOUND when removing non-existent image")
  void shouldThrowResourceNotFound_whenRemovingNonExistentImage() {
    String productId = "prod-400";
    ProductDocument product = createTestProduct(productId, "Camera 4K");
    product.setImages(new ArrayList<>());

    when(productRepository.findById(productId)).thenReturn(Optional.of(product));

    assertThatThrownBy(() -> productService.removeImage(productId, "non-existent.png"))
        .isInstanceOf(DomainException.class)
        .extracting(ex -> ((DomainException) ex).getFailureCode())
        .isEqualTo(FailureCode.RESOURCE_NOT_FOUND);
  }

  private ProductDocument createTestProduct(String id, String name) {
    AuditMetadata audit = AuditMetadata.create("admin-tester");
    return new ProductDocument(
        id,
        "SKU-" + id,
        "slug-" + id,
        name,
        "Description",
        "Brand",
        "cat-1",
        List.of("cat-1"),
        ProductStatus.DRAFT,
        Money.brl("199.90"),
        List.of(),
        new ArrayList<>(),
        1L,
        audit);
  }
}
