# E-Commerce Catalog & Faceted Search Engine

High-performance backend for a heterogeneous e-commerce catalog and dynamic faceted search engine, implemented as a **Modular Monolith in Java 21** using **Spring Boot 3.3+**, **Spring Modulith**, **MongoDB 7**, and **Redis 7**.

---

## 1. Architectural Overview

The system is structured into cohesive, decoupled bounded contexts enforced and validated at build time by Spring Modulith (`ApplicationModules.verify()`):

- **`category`** (`com.ecommerce.category`): Hierarchical category taxonomy modeled via *Ancestors Path*, attribute definition inheritance, dynamic taxonomy tree caching, and cross-module DTO contracts (`@NamedInterface("dto")`).
- **`product`** (`com.ecommerce.product`): Product lifecycle management (`DRAFT`, `ACTIVE`, `ARCHIVED`), heterogeneous specifications via the **Attribute Pattern**, strict taxonomy validation, optimistic concurrency control (`@Version` / `If-Match`), single-trip faceted search aggregations (`$match` + `$facet`), opaque cursor pagination, and surgical Redis cache-aside.
- **`outbox`** (`com.ecommerce.outbox`): Transactional outbox implementation providing fault-tolerant, asynchronous cleanup of physical object storage files (SeaweedFS / AWS S3) without distributed dual-write inconsistencies.
- **`auth`** (`com.ecommerce.auth`): Stateless JWT token issuer (`/api/v1/auth/dev-token`) supporting development and automated testing for `ROLE_ADMIN` and `ROLE_USER` identities.
- **`shared`** (`com.ecommerce.shared`): Open shared kernel containing immutable Value Objects (`Money`), RFC 7807 error problem details (`ProblemDetail`), auditing metadata (`AuditorAware`), storage abstractions, MDC correlation logging, health indicators, and OpenAPI configurations.

---

## 2. Key Architectural Patterns & Features

- **Attribute Pattern:** Stores polymorphic product attributes in indexed multikey arrays (`attributes: [{ k, v, unit }]`), enabling dynamic filtering without requiring runtime MongoDB DDL schema changes.
- **Faceted Aggregations:** Computes dynamic attribute counts, price buckets, and paginated items in a single network roundtrip using MongoDB Aggregation Pipelines.
- **Stable Cursor Pagination:** Opaque, tamper-resistant Base64URL cursor tokens ensuring non-skipping pagination even under concurrent catalog writes.
- **Resilient Redis Cache-Aside:** High-throughput caching for active products (`cache:product:{id}` and `cache:product:{slug}`) with automatic 10-minute TTL, surgical eviction upon mutations, and transparent fail-safe fallback to MongoDB on Redis connectivity outages.
- **LGTM Observability Stack:** Complete observability integration featuring MDC correlation IDs (`X-Correlation-Id`), OpenTelemetry W3C distributed tracing, Prometheus metrics (`catalog.cache.*`, `catalog.search.duration`, `catalog.products.count`), Loki log aggregation via Promtail, and provisioned Grafana dashboards.
- **Interactive OpenAPI 3 / Swagger UI:** Interactive documentation available out of the box with Bearer JWT authorization support.

---

## 3. Prerequisites & Infrastructure Setup

- **Java 21 LTS**
- **Docker & Docker Compose**

### 3.1. Local Development Backing Services (`dev-infra`)
To spin up **only the supporting services** required to run and test the application on the host machine (MongoDB with replica set `rs0`, Redis 7, and SeaweedFS S3 storage):

```bash
docker compose -f docker-compose.dev-infra.yml up -d
```

- **MongoDB:** `localhost:27017` (replica set member initialized to `localhost:27017`)
- **Redis:** `localhost:6379`
- **SeaweedFS S3 API:** `http://localhost:8333` (Master/Web UI: `http://localhost:9333`)

To stop the development infrastructure:
```bash
docker compose -f docker-compose.dev-infra.yml down
```

### 3.2. Full Production Stack (`app` + infrastructure + observability)
To build the containerized application image via the multi-stage `Dockerfile` (Alpine JRE 21, non-root user) and run the entire integrated production stack:

```bash
docker compose up -d --build
```

- **Catalog Application:** `http://localhost:8080`
- **Swagger UI:** `http://localhost:8080/swagger-ui/index.html`
- **Grafana Dashboards:** `http://localhost:3000` (`admin` / `admin`)
- **Prometheus UI:** `http://localhost:9090`
- **Loki Endpoint:** `http://localhost:3100`

To stop the production stack:
```bash
docker compose down
```

---

## 4. Build & Execution Commands

Tool versions are defined in `.tool-versions` (managed via `asdf` or direct local installation). Use Maven (`mvn`):

```bash
# Compile and validate types
mvn compile -q

# Run application locally (with dev-infra running)
mvn spring-boot:run

# Check code formatting (Spotless / Google Java Format)
mvn spotless:check

# Apply automated formatting fixes
mvn spotless:apply

# Execute unit, integration, and architecture tests
mvn verify
```

---

## 5. API Documentation & Interactive Exploration

The REST API exposes interactive documentation via OpenAPI 3 / Swagger UI:

- **Swagger UI Console:** `http://localhost:8080/swagger-ui/index.html`
- **OpenAPI 3 JSON Specification:** `http://localhost:8080/swagger-ui/api-docs`

### Testing Authenticated Endpoints:
1. Call `POST /api/v1/auth/dev-token` with `{ "username": "admin", "role": "ADMIN" }` to obtain a JWT.
2. In the Swagger UI dashboard, click the green **Authorize** 🔓 button at the top right.
3. Paste the token into the `Value` field (without the `Bearer ` prefix) and click **Authorize**.
4. All protected mutational endpoints (`POST`, `PUT`, `PATCH`, `DELETE`) will automatically propagate the authorization header.

---

## 6. Architecture & Reference Documentation

- [SPECIFICATION.md](docs/SPECIFICATION.md): Functional and non-functional requirements, business rules, domain schemas, and API contracts.
- [ARCHITECTURE.md](docs/ARCHITECTURE.md): Directory structures, collection mappings, single responsibility guidelines, and module boundaries.
- [CODING_GUIDELINES.md](docs/CODING_GUIDELINES.md): Modern Java conventions, immutability, Spring Modulith rules, and database/cache best practices.
- [SYSTEM_STATE.md](docs/SYSTEM_STATE.md): Executive state tracking, active bounded contexts, and key architectural decision records.
- [OBSERVABILIDADE.md](.local/OBSERVABILIDADE.md): Comprehensive guide to logs, metrics, distributed tracing, and Grafana dashboard operation.
- [SWAGGER_OPENAPI.md](.local/SWAGGER_OPENAPI.md): Detailed walkthrough of the OpenAPI 3 configuration, auto-configuration, and Spring Security setup.
