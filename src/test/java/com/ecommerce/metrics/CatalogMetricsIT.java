package com.ecommerce.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import com.ecommerce.product.dto.SearchFilterRequest;
import com.ecommerce.product.entity.ProductDocument;
import com.ecommerce.product.entity.ProductStatus;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.product.repository.ProductSearchRepository;
import com.ecommerce.shared.audit.AuditMetadata;
import com.ecommerce.shared.cache.CacheService;
import com.ecommerce.shared.money.Money;
import com.ecommerce.support.AbstractIntegrationTest;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class CatalogMetricsIT extends AbstractIntegrationTest {

  @Autowired
  private MeterRegistry meterRegistry;

  @Autowired
  private CacheService cacheService;

  @Autowired
  private ProductSearchRepository productSearchRepository;

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private TestRestTemplate restTemplate;

  @BeforeEach
  void cleanData() {
    productRepository.deleteAll();
  }

  @Test
  @DisplayName("Should increment cache hits and misses metrics when cache operations are executed")
  void shouldIncrementCacheHitsAndMisses_whenCacheOperationsExecuted() {
    double initialMisses = meterRegistry.find("catalog.cache.misses")
        .tag("cache", "product")
        .counter() != null
        ? meterRegistry.get("catalog.cache.misses").tag("cache", "product").counter().count()
        : 0.0;

    cacheService.get("cache:product:non-existent-key", String.class);

    double updatedMisses = meterRegistry.get("catalog.cache.misses")
        .tag("cache", "product")
        .counter()
        .count();
    assertThat(updatedMisses).isEqualTo(initialMisses + 1.0);

    cacheService.put("cache:product:active-item-key", "{\"test\":\"value\"}", Duration.ofMinutes(5));

    double initialHits = meterRegistry.find("catalog.cache.hits")
        .tag("cache", "product")
        .counter() != null
        ? meterRegistry.get("catalog.cache.hits").tag("cache", "product").counter().count()
        : 0.0;

    cacheService.get("cache:product:active-item-key", String.class);

    double updatedHits = meterRegistry.get("catalog.cache.hits")
        .tag("cache", "product")
        .counter()
        .count();
    assertThat(updatedHits).isEqualTo(initialHits + 1.0);
  }

  @Test
  @DisplayName("Should record duration when product search is executed")
  void shouldRecordDuration_whenProductSearchExecuted() {
    long initialCount = meterRegistry.get("catalog.search.duration").timer().count();

    productSearchRepository.search(
        new SearchFilterRequest(null, null, null, null, null, null, null, 10),
        null);

    long updatedCount = meterRegistry.get("catalog.search.duration").timer().count();
    assertThat(updatedCount).isGreaterThan(initialCount);
  }

  @Test
  @DisplayName("Should expose product count gauge when counting products by status")
  void shouldExposeProductCountGauge_whenCountingProductsByStatus() {
    ProductDocument draft = new ProductDocument(
        null, "SKU-DRAFT-METRIC", "draft-metric-prod", "Draft Metric Product", "Desc", "Brand",
        "cat1", List.of("cat1"), ProductStatus.DRAFT, Money.brl("100.00"),
        List.of(), List.of(), 0L, AuditMetadata.create("admin"));
    ProductDocument active = new ProductDocument(
        null, "SKU-ACTIVE-METRIC", "active-metric-prod", "Active Metric Product", "Desc", "Brand",
        "cat1", List.of("cat1"), ProductStatus.ACTIVE, Money.brl("200.00"),
        List.of(), List.of(), 0L, AuditMetadata.create("admin"));

    productRepository.saveAll(List.of(draft, active));

    double draftCount = meterRegistry.get("catalog.products.count")
        .tag("status", "DRAFT")
        .gauge()
        .value();
    double activeCount = meterRegistry.get("catalog.products.count")
        .tag("status", "ACTIVE")
        .gauge()
        .value();

    assertThat(draftCount).isEqualTo(1.0);
    assertThat(activeCount).isEqualTo(1.0);
  }

  @Test
  @DisplayName("Should expose metrics when actuator endpoints are queried")
  void shouldExposeMetrics_whenActuatorEndpointsAreQueried() {
    ResponseEntity<String> searchMetricResponse = restTemplate.getForEntity(
        "/actuator/metrics/catalog.search.duration",
        String.class);
    assertThat(searchMetricResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(searchMetricResponse.getBody()).contains("catalog.search.duration");

    ResponseEntity<String> prometheusResponse = restTemplate.getForEntity(
        "/actuator/prometheus",
        String.class);
    assertThat(prometheusResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(prometheusResponse.getBody()).contains("catalog_search_duration_seconds");
  }
}
