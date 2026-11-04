package com.ecommerce.product.metrics;

import com.ecommerce.product.entity.ProductStatus;
import com.ecommerce.product.repository.ProductRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Metric instruments for product catalog status counts and distributions.
 */
@Component
public class ProductMetrics {

  public ProductMetrics(ProductRepository productRepository, MeterRegistry meterRegistry) {
    Gauge.builder("catalog.products.count", () -> productRepository.countByStatus(ProductStatus.ACTIVE))
        .tag("status", "ACTIVE")
        .description("Number of active products")
        .strongReference(true)
        .register(meterRegistry);

    Gauge.builder("catalog.products.count", () -> productRepository.countByStatus(ProductStatus.DRAFT))
        .tag("status", "DRAFT")
        .description("Number of draft products")
        .strongReference(true)
        .register(meterRegistry);

    Gauge.builder("catalog.products.count", () -> productRepository.countByStatus(ProductStatus.ARCHIVED))
        .tag("status", "ARCHIVED")
        .description("Number of archived products")
        .strongReference(true)
        .register(meterRegistry);
  }
}
