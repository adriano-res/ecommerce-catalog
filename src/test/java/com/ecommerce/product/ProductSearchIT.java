package com.ecommerce.product;

import static org.assertj.core.api.Assertions.assertThat;

import com.ecommerce.category.CategoryRepository;
import com.ecommerce.category.entity.CategoryDocument;
import com.ecommerce.product.dto.FacetOption;
import com.ecommerce.product.dto.PriceRangeFacet;
import com.ecommerce.product.dto.ProductSearchItemResponse;
import com.ecommerce.product.dto.ProductSearchResult;
import com.ecommerce.product.dto.SearchFacetResponse;
import com.ecommerce.product.dto.SearchFilterRequest;
import com.ecommerce.product.dto.SearchSort;
import com.ecommerce.product.entity.AttributeValue;
import com.ecommerce.product.entity.ProductDocument;
import com.ecommerce.product.entity.ProductImage;
import com.ecommerce.product.entity.ProductStatus;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.shared.audit.AuditMetadata;
import com.ecommerce.shared.db.migrations.V001_CreateCategoryIndexes;
import com.ecommerce.shared.db.migrations.V002_CreateProductIndexes;
import com.ecommerce.shared.money.Money;
import com.ecommerce.support.AbstractIntegrationTest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ProductSearchIT extends AbstractIntegrationTest {

  @Autowired private ProductService productService;

  @Autowired private ProductRepository productRepository;

  @Autowired private CategoryRepository categoryRepository;

  @Autowired private TestRestTemplate restTemplate;

  @BeforeEach
  void setUpIndexes() {
    new V001_CreateCategoryIndexes().execute(mongoTemplate);
    new V002_CreateProductIndexes().execute(mongoTemplate);
  }

  @Test
  @DisplayName("Should return active products matching text when search query provided")
  void shouldReturnActiveProductsMatchingText_whenSearchQueryProvided() {
    CategoryDocument category = createCategory("tech", "Tecnologia", null, List.of());

    createProduct(
        "GALAXY-S24",
        "galaxy-s24-ultra",
        "Galaxy S24 Ultra",
        "Samsung",
        category.getId(),
        List.of(category.getId()),
        ProductStatus.ACTIVE,
        Money.of("3499.90", "BRL"),
        List.of(new AttributeValue("ram", "8GB", "GB")));

    createProduct(
        "IPHONE-15",
        "iphone-15-pro",
        "iPhone 15 Pro Max",
        "Apple",
        category.getId(),
        List.of(category.getId()),
        ProductStatus.ACTIVE,
        Money.of("5999.00", "BRL"),
        List.of(new AttributeValue("ram", "8GB", "GB")));

    createProduct(
        "GALAXY-DRAFT",
        "galaxy-tab-s9",
        "Galaxy Tab S9",
        "Samsung",
        category.getId(),
        List.of(category.getId()),
        ProductStatus.DRAFT,
        Money.of("2499.00", "BRL"),
        List.of(new AttributeValue("ram", "8GB", "GB")));

    SearchFilterRequest request =
        new SearchFilterRequest("Galaxy", null, null, null, null, SearchSort.RELEVANCE, null, 10);

    ProductSearchResult result = productService.search(request);

    assertThat(result.totalCount()).isEqualTo(1L);
    assertThat(result.items()).hasSize(1);
    assertThat(result.items().get(0).sku()).isEqualTo("GALAXY-S24");
    assertThat(result.items().get(0).thumbnailUrl()).isEqualTo("https://img.test/GALAXY-S24.jpg");
  }

  @Test
  @DisplayName("Should return products from subcategories when ancestor category queried")
  void shouldReturnProductsFromSubcategories_whenAncestorCategoryQueried() {
    CategoryDocument root = createCategory("eletronicos", "Eletrônicos", null, List.of());
    CategoryDocument sub =
        createCategory("smartphones", "Smartphones", root.getId(), List.of(root.getId()));
    CategoryDocument deep =
        createCategory(
            "smartphones-5g",
            "Smartphones 5G",
            sub.getId(),
            List.of(root.getId(), sub.getId()));
    CategoryDocument other = createCategory("livros", "Livros", null, List.of());

    createProduct(
        "PHONE-1",
        "phone-sub",
        "Smartphone Standard",
        "BrandX",
        sub.getId(),
        List.of(root.getId(), sub.getId()),
        ProductStatus.ACTIVE,
        Money.of("1200.00", "BRL"),
        List.of());

    createProduct(
        "PHONE-2",
        "phone-deep",
        "Smartphone 5G Pro",
        "BrandY",
        deep.getId(),
        List.of(root.getId(), sub.getId(), deep.getId()),
        ProductStatus.ACTIVE,
        Money.of("2500.00", "BRL"),
        List.of());

    createProduct(
        "BOOK-1",
        "clean-code",
        "Clean Code",
        "Prentice Hall",
        other.getId(),
        List.of(other.getId()),
        ProductStatus.ACTIVE,
        Money.of("99.00", "BRL"),
        List.of());

    SearchFilterRequest request =
        new SearchFilterRequest(
            null, "eletronicos", null, null, null, SearchSort.PRICE_ASC, null, 10);

    ProductSearchResult result = productService.search(request);

    assertThat(result.totalCount()).isEqualTo(2L);
    assertThat(result.items())
        .extracting(ProductSearchItemResponse::sku)
        .containsExactly("PHONE-1", "PHONE-2");
  }

  @Test
  @DisplayName("Should filter by attributes with OR logic for same key when multiple values provided")
  void shouldFilterByAttributesWithOrLogicForSameKey_whenMultipleValuesProvided() {
    CategoryDocument category = createCategory("smartphones", "Smartphones", null, List.of());

    createProduct(
        "RAM-8",
        "phone-8gb",
        "Phone 8GB",
        "BrandA",
        category.getId(),
        List.of(category.getId()),
        ProductStatus.ACTIVE,
        Money.of("1000.00", "BRL"),
        List.of(new AttributeValue("ram", "8GB", "GB")));

    createProduct(
        "RAM-16",
        "phone-16gb",
        "Phone 16GB",
        "BrandB",
        category.getId(),
        List.of(category.getId()),
        ProductStatus.ACTIVE,
        Money.of("1500.00", "BRL"),
        List.of(new AttributeValue("ram", "16GB", "GB")));

    createProduct(
        "RAM-32",
        "phone-32gb",
        "Phone 32GB",
        "BrandC",
        category.getId(),
        List.of(category.getId()),
        ProductStatus.ACTIVE,
        Money.of("2000.00", "BRL"),
        List.of(new AttributeValue("ram", "32GB", "GB")));

    SearchFilterRequest request =
        new SearchFilterRequest(
            null,
            null,
            Map.of("ram", List.of("8GB", "16GB")),
            null,
            null,
            SearchSort.PRICE_ASC,
            null,
            10);

    ProductSearchResult result = productService.search(request);

    assertThat(result.totalCount()).isEqualTo(2L);
    assertThat(result.items())
        .extracting(ProductSearchItemResponse::sku)
        .containsExactly("RAM-8", "RAM-16");
  }

  @Test
  @DisplayName(
      "Should filter by attributes with AND logic for distinct keys when multiple attributes provided")
  void shouldFilterByAttributesWithAndLogicForDistinctKeys_whenMultipleAttributesProvided() {
    CategoryDocument category = createCategory("smartphones", "Smartphones", null, List.of());

    createProduct(
        "P-MATCH",
        "phone-match",
        "Phone Matching",
        "BrandA",
        category.getId(),
        List.of(category.getId()),
        ProductStatus.ACTIVE,
        Money.of("1000.00", "BRL"),
        List.of(
            new AttributeValue("ram", "8GB", "GB"),
            new AttributeValue("storageGb", 256L, "GB")));

    createProduct(
        "P-DIFF-STORAGE",
        "phone-diff-storage",
        "Phone Diff Storage",
        "BrandB",
        category.getId(),
        List.of(category.getId()),
        ProductStatus.ACTIVE,
        Money.of("1200.00", "BRL"),
        List.of(
            new AttributeValue("ram", "8GB", "GB"),
            new AttributeValue("storageGb", 128L, "GB")));

    createProduct(
        "P-DIFF-RAM",
        "phone-diff-ram",
        "Phone Diff RAM",
        "BrandC",
        category.getId(),
        List.of(category.getId()),
        ProductStatus.ACTIVE,
        Money.of("1400.00", "BRL"),
        List.of(
            new AttributeValue("ram", "16GB", "GB"),
            new AttributeValue("storageGb", 256L, "GB")));

    SearchFilterRequest request =
        new SearchFilterRequest(
            null,
            null,
            Map.of("ram", List.of("8GB"), "storageGb", List.of("256")),
            null,
            null,
            SearchSort.PRICE_ASC,
            null,
            10);

    ProductSearchResult result = productService.search(request);

    assertThat(result.totalCount()).isEqualTo(1L);
    assertThat(result.items()).hasSize(1);
    assertThat(result.items().get(0).sku()).isEqualTo("P-MATCH");
  }

  @Test
  @DisplayName("Should return accurate facet counts when search executed")
  void shouldReturnAccurateFacetCounts_whenSearchExecuted() {
    CategoryDocument category = createCategory("smartphones", "Smartphones", null, List.of());

    createProduct(
        "F-1",
        "f-phone-1",
        "Phone 1",
        "BrandA",
        category.getId(),
        List.of(category.getId()),
        ProductStatus.ACTIVE,
        Money.of("1500.00", "BRL"),
        List.of(
            new AttributeValue("ram", "8GB", "GB"),
            new AttributeValue("storageGb", 256L, "GB")));

    createProduct(
        "F-2",
        "f-phone-2",
        "Phone 2",
        "BrandB",
        category.getId(),
        List.of(category.getId()),
        ProductStatus.ACTIVE,
        Money.of("2500.00", "BRL"),
        List.of(
            new AttributeValue("ram", "8GB", "GB"),
            new AttributeValue("storageGb", 128L, "GB")));

    createProduct(
        "F-3",
        "f-phone-3",
        "Phone 3",
        "BrandC",
        category.getId(),
        List.of(category.getId()),
        ProductStatus.ACTIVE,
        Money.of("4500.00", "BRL"),
        List.of(
            new AttributeValue("ram", "16GB", "GB"),
            new AttributeValue("storageGb", 256L, "GB")));

    SearchFilterRequest request =
        new SearchFilterRequest(null, null, null, null, null, SearchSort.PRICE_ASC, null, 10);

    ProductSearchResult result = productService.search(request);

    assertThat(result.totalCount()).isEqualTo(3L);
    SearchFacetResponse facets = result.facets();

    List<FacetOption> ramFacets = facets.attributes().get("ram");
    assertThat(ramFacets).isNotNull();
    assertThat(ramFacets)
        .extracting(FacetOption::value)
        .containsExactly("8GB", "16GB");
    assertThat(ramFacets)
        .extracting(FacetOption::count)
        .containsExactly(2L, 1L);

    List<FacetOption> storageFacets = facets.attributes().get("storageGb");
    assertThat(storageFacets).isNotNull();
    assertThat(storageFacets)
        .extracting(FacetOption::value)
        .containsExactly("256", "128");
    assertThat(storageFacets)
        .extracting(FacetOption::count)
        .containsExactly(2L, 1L);

    List<PriceRangeFacet> priceRanges = facets.priceRanges();
    assertThat(priceRanges).hasSize(3);
    assertThat(priceRanges.get(0).from()).isEqualTo("0.00");
    assertThat(priceRanges.get(0).to()).isEqualTo("1999.99");
    assertThat(priceRanges.get(0).count()).isEqualTo(1L);

    assertThat(priceRanges.get(1).from()).isEqualTo("2000.00");
    assertThat(priceRanges.get(1).to()).isEqualTo("3999.99");
    assertThat(priceRanges.get(1).count()).isEqualTo(1L);

    assertThat(priceRanges.get(2).from()).isEqualTo("4000.00");
    assertThat(priceRanges.get(2).to()).isNull();
    assertThat(priceRanges.get(2).count()).isEqualTo(1L);
  }

  @Test
  @DisplayName("Should paginate deterministically with cursor when navigating pages")
  void shouldPaginateDeterministicallyWithCursor_whenNavigatingPages() {
    CategoryDocument category = createCategory("smartphones", "Smartphones", null, List.of());

    List<Double> prices = List.of(100.0, 200.0, 200.0, 200.0, 300.0);
    for (int i = 0; i < prices.size(); i++) {
      int idx = i + 1;
      createProduct(
          "P-PAGE-" + idx,
          "p-page-" + idx,
          "Page Phone " + idx,
          "BrandP",
          category.getId(),
          List.of(category.getId()),
          ProductStatus.ACTIVE,
          Money.of(String.format(Locale.ROOT, "%.2f", prices.get(i)), "BRL"),
          List.of());
    }

    // Page 1
    SearchFilterRequest req1 =
        new SearchFilterRequest(null, null, null, null, null, SearchSort.PRICE_ASC, null, 2);
    ProductSearchResult page1 = productService.search(req1);

    assertThat(page1.totalCount()).isEqualTo(5L);
    assertThat(page1.items()).hasSize(2);
    assertThat(page1.items())
        .extracting(ProductSearchItemResponse::sku)
        .containsExactly("P-PAGE-1", "P-PAGE-2");
    assertThat(page1.nextCursor()).isNotNull();

    // Page 2
    SearchFilterRequest req2 =
        new SearchFilterRequest(
            null, null, null, null, null, SearchSort.PRICE_ASC, page1.nextCursor(), 2);
    ProductSearchResult page2 = productService.search(req2);

    assertThat(page2.totalCount()).isEqualTo(5L);
    assertThat(page2.items()).hasSize(2);
    assertThat(page2.items())
        .extracting(ProductSearchItemResponse::sku)
        .containsExactly("P-PAGE-3", "P-PAGE-4");
    assertThat(page2.nextCursor()).isNotNull();

    // Page 3
    SearchFilterRequest req3 =
        new SearchFilterRequest(
            null, null, null, null, null, SearchSort.PRICE_ASC, page2.nextCursor(), 2);
    ProductSearchResult page3 = productService.search(req3);

    assertThat(page3.totalCount()).isEqualTo(5L);
    assertThat(page3.items()).hasSize(1);
    assertThat(page3.items().get(0).sku()).isEqualTo("P-PAGE-5");
    assertThat(page3.nextCursor()).isNull();
  }

  @Test
  @DisplayName("Should filter by price range when priceMin and priceMax provided")
  void shouldFilterByPriceRange_whenPriceMinAndPriceMaxProvided() {
    CategoryDocument category = createCategory("smartphones", "Smartphones", null, List.of());

    createProduct(
        "PRICE-1",
        "price-low",
        "Budget Phone",
        "BrandX",
        category.getId(),
        List.of(category.getId()),
        ProductStatus.ACTIVE,
        Money.of("1000.00", "BRL"),
        List.of());

    createProduct(
        "PRICE-2",
        "price-mid",
        "Mid-range Phone",
        "BrandY",
        category.getId(),
        List.of(category.getId()),
        ProductStatus.ACTIVE,
        Money.of("2500.00", "BRL"),
        List.of());

    createProduct(
        "PRICE-3",
        "price-high",
        "Flagship Phone",
        "BrandZ",
        category.getId(),
        List.of(category.getId()),
        ProductStatus.ACTIVE,
        Money.of("5000.00", "BRL"),
        List.of());

    SearchFilterRequest request =
        new SearchFilterRequest(
            null,
            null,
            null,
            new BigDecimal("2000.00"),
            new BigDecimal("4000.00"),
            SearchSort.PRICE_ASC,
            null,
            10);

    ProductSearchResult result = productService.search(request);

    assertThat(result.totalCount()).isEqualTo(1L);
    assertThat(result.items()).hasSize(1);
    assertThat(result.items().get(0).sku()).isEqualTo("PRICE-2");
  }

  @Test
  @DisplayName("Should return HTTP OK with items and facets when invoking search endpoint via HTTP")
  void shouldReturnHttpOkWithItemsAndFacets_whenInvokingSearchEndpointViaHttp() {
    CategoryDocument category = createCategory("smartphones", "Smartphones", null, List.of());

    createProduct(
        "HTTP-P1",
        "http-phone-1",
        "HTTP Galaxy Phone",
        "Samsung",
        category.getId(),
        List.of(category.getId()),
        ProductStatus.ACTIVE,
        Money.of("2999.00", "BRL"),
        List.of(new AttributeValue("ram", "8GB", "GB")));

    ResponseEntity<ProductSearchResult> response =
        restTemplate.getForEntity(
            "/api/v1/products/search?q=Galaxy&attr.ram=8GB&limit=5",
            ProductSearchResult.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().totalCount()).isEqualTo(1L);
    assertThat(response.getBody().items()).hasSize(1);
    assertThat(response.getBody().items().get(0).sku()).isEqualTo("HTTP-P1");
    assertThat(response.getBody().facets().attributes()).containsKey("ram");
  }

  private CategoryDocument createCategory(
      String slug, String name, String parentId, List<String> ancestors) {
    CategoryDocument doc =
        new CategoryDocument(
            null,
            slug,
            name,
            parentId,
            ancestors != null ? ancestors : List.of(),
            List.of(),
            AuditMetadata.create("test-admin"));
    return categoryRepository.save(doc);
  }

  private ProductDocument createProduct(
      String sku,
      String slug,
      String name,
      String brand,
      String categoryId,
      List<String> categoryPath,
      ProductStatus status,
      Money price,
      List<AttributeValue> attributes) {
    ProductDocument doc =
        new ProductDocument(
            null,
            sku,
            slug,
            name,
            "Description for " + name,
            brand,
            categoryId,
            categoryPath != null ? categoryPath : List.of(),
            status,
            price,
            attributes != null ? attributes : List.of(),
            List.of(
                new ProductImage(
                    UUID.randomUUID().toString(),
                    "https://img.test/" + sku + ".jpg",
                    name)),
            0L,
            AuditMetadata.create("test-admin"));
    return productRepository.save(doc);
  }
}
