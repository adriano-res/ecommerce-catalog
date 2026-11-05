package com.ecommerce.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  public static final String SECURITY_SCHEME_NAME = "BearerAuth";

  @Bean
  public OpenAPI customOpenAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("E-Commerce Catalog API")
                .version("1.0.0")
                .description(
                    """
                    Production-grade E-Commerce Catalog & Faceted Search Engine API built with Spring Modulith, MongoDB, and Redis.

                    ### Core Architecture & Patterns:
                    - **Attribute Pattern:** Flexible heterogeneous product specifications (`attributes: [{ k, v, unit }]`) indexed for multi-attribute dynamic search without dynamic DDL index creation.
                    - **Hierarchical Taxonomy:** Category tree navigation using ancestor paths with deterministic attribute inheritance and conflict resolution.
                    - **Faceted Aggregations:** Dynamic facet counting and paginated results computed in a single network roundtrip via MongoDB `$facet` aggregation pipelines.
                    - **Cursor Pagination:** Stable, non-skipping pagination using opaque Base64URL-encoded cursor tokens.
                    - **Redis Cache-Aside:** Sub-millisecond reads with surgical domain-event invalidation (`ProductChanged`) and transparent resilience fallback to MongoDB.
                    - **Optimistic Concurrency:** HTTP `If-Match` / `ETag` headers preventing lost updates on concurrent mutations.
                    - **RFC 7807 Problem Details:** Consistent, machine-readable error responses across all endpoints.
                    """)
                .contact(
                    new Contact()
                        .name("E-Commerce Architecture Team")
                        .email("catalog-team@ecommerce.com"))
                .license(
                    new License()
                        .name("Apache 2.0")
                        .url("https://www.apache.org/licenses/LICENSE-2.0")))
        .components(
            new Components()
                .addSecuritySchemes(
                    SECURITY_SCHEME_NAME,
                    new SecurityScheme()
                        .name(SECURITY_SCHEME_NAME)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Enter JWT Bearer token obtained from /api/v1/auth/dev-token")))
        .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
  }
}
