# Estado Atual do Sistema (System State)

> **Documento Vivo**: Mantido automaticamente pelas skills do Kanban (`task-close`, `task-plan`).  
> **Propósito**: Servir como memória executiva ultracompacta para agentes e humanos, garantindo que o estado real do código seja consultado rapidamente sem consumir grandes volumes de tokens.

---

## 1. Status do Kanban de Desenvolvimento
- **Cards Concluídos (`done/`)**:
  - `001`: Setup Inicial do Projeto (Spring Boot 3.5, Java 21, Mongo replica set, Redis, Modulith e Shared Kernel).
  - `002`: Autenticação e Segurança Stateless via JWT (`/api/v1/auth/dev-token`, `JwtAuthenticationFilter`, RBAC `ADMIN`/`USER`).
  - `003`: Taxonomia e Categorias Hierárquicas (`com.ecommerce.category`, *Ancestors Path*, cache-aside no Redis, `@NamedInterface("dto")`).
  - `004`: Cadastro e Ciclo de Vida de Produtos com Attribute Pattern (`com.ecommerce.product`, DRAFT/ACTIVE/ARCHIVED, `@Version` ETag/If-Match, Mongock `V002`, `AttributeValidator`, `ProductApi`).
  - `005`: Busca de Produtos com Filtros Dinâmicos e Paginação por Cursor (`ProductSearchRepository`, MongoDB `$match` e `$facet` em viagem única, cursor Base64URL determinístico, subpacote `repository`).
  - `006`: Cache-Aside no Redis para Produtos e Invalidação Cirúrgica Direta (`ProductService`, `CacheService`, TTL de 10 min, dual keys `id`/`slug`, resiliência com fallback transparente para MongoDB, governança de status DRAFT/ARCHIVED).
  - `007`: Observabilidade, Auditoria Centralizada, Telemetria OpenTelemetry, Logs Legíveis e Stack Grafana/Loki (`CorrelationIdFilter`, `RedisReadinessHealthIndicator` DEGRADED, Micrometer, subpacote `metrics/`, LGTM stack).
  - `008`: Armazenamento de Imagens com SeaweedFS via S3 API e Transactional Outbox (`shared.storage`, `com.ecommerce.outbox`, `ProductImage` com `id`, eliminação de Dual-Write e endpoints REST).
- **Card Atual em Andamento (`doing/`)**:
  - Nenhum no momento.
- **Card em Revisão (`review/`)**:
  - Nenhum no momento.
- **Cards Planejados no Pipeline (`prototype/`)**:
  - Nenhum no momento.

---

## 2. Módulos e Bounded Contexts Ativos no Código

| Módulo / Pacote | Papel Principal | Componentes Chave |
| :--- | :--- | :--- |
| **`auth`** (`com.ecommerce.auth`) | Emissão de tokens JWT dev (stateless) | `DevTokenController`, `DevTokenRequest`, `DevTokenResponse` |
| **`category`** (`com.ecommerce.category`) | Gestão de taxonomia hierárquica e herança de atributos | `CategoryController`, `CategoryService`, `CategoryRepository`, `entity/` (`CategoryDocument`), `mapper/` (`CategoryMapper`), `dto/` (`@NamedInterface`) |
| **`product`** (`com.ecommerce.product`) | Cadastro, ciclo de vida de produtos (DRAFT, ACTIVE, ARCHIVED), busca facetada, cache-aside direto e métricas | `ProductController`, `ProductService`, `ProductApi`, `repository/` (`ProductRepository`, `ProductSearchRepository`), `entity/` (`ProductDocument`, `AttributeValue`, `ProductImage`, `ProductStatus`), `metrics/` (`ProductMetrics`), `validation/` (`AttributeValidator`, `ImageValidator`), `mapper/` (`ProductMapper`), `dto/` (`@NamedInterface`) |
| **`outbox`** (`com.ecommerce.outbox`) | Transactional outbox e processamento assíncrono com tolerância a falhas | `OutboxApi`, `OutboxService`, `OutboxRepository`, `entity/` (`OutboxDocument`, `OutboxStatus`), `jobs/` (`OutboxProcessor`) |
| **`shared`** (`com.ecommerce.shared`) | Kernel transversal reutilizado entre módulos | `money`, `error`, `audit`, `pagination`, `security`, `config`, `cache`, `storage`, `web`, `enums`, `db.migrations` |

---

## 3. Banco de Dados e Cache Ativos

### Coleções MongoDB (`ecommerce_catalog`)
- **`categories`** (gerenciada por `category`):
  - Documento: `CategoryDocument` em `entity/` (id, slug, name, parentId, ancestors, attributeDefinitions, audit).
  - Migrações Mongock ativas:
    - `V001_CreateCategoryIndexes`: Índice único em `slug`, índices secundários em `parentId` e multikey em `ancestors`.
- **`products`** (gerenciada por `product`):
  - Documento: `ProductDocument` em `entity/` (id, sku, slug, name, brand, description, categoryId, categoryPath, status, price em Money, attributes em Attribute Pattern `k/v/unit`, images [id, url, alt], version, audit).
  - Migrações Mongock ativas:
    - `V002_CreateProductIndexes`: Índices únicos em `sku` e `slug`, composto `{ status: 1, categoryPath: 1, "price.amountMinor": 1 }`, multikey em `{ "attributes.k": 1, "attributes.v": 1 }` e índice de texto ponderado em português.
- **`outbox_events`** (gerenciada por `outbox`):
  - Documento: `OutboxDocument` em `entity/` (id, eventType, payload, status [PENDING, PROCESSING, COMPLETED, FAILED], retryCount, lastError, createdAt, updatedAt).
  - Migrações Mongock ativas:
    - `V003_CreateOutboxIndexes`: Índice composto `{ status: 1, createdAt: 1 }`.

### Namespaces de Chave no Redis
- **`cache:product:{idOrSlug}`**: Serialização JSON de `ProductResponse` (TTL: 10 min, gravado simultaneamente por ID e slug para produtos ACTIVE, invalidado diretamente via `CacheService` a cada mutação).
- **`cache:category-tree`**: Serialização JSON da árvore completa de categorias (TTL: 30 min, invalidada via `CacheService` a cada mutação de taxonomia).

---

## 4. Kernel Compartilhado (`com.ecommerce.shared`)
- **`shared.money`**: Value Object imutável `Money` (armazena centavos em `long amountMinor` com `Currency` ISO-4217; proibição estrita de float/double). Desserialização estrita exigindo objeto `{ amount, currency }`. Conversores customizados no MongoDB para evitar perda de precisão.
- **`shared.error`**: `DomainException`, taxonomia `FailureCode` e `ProblemDetailHandler` com suporte a RFC 7807, correlationId, tratamento de `HttpMessageNotReadableException` (400 `VALIDATION_ERROR`) e detalhes de validação.
- **`shared.security`**: `SecurityConfig` stateless, `JwtService` com HMAC-SHA256, `JwtAuthenticationFilter` integrado ao `MDC` (`correlationId`, `actor`) e `SecurityUtils` (`isAdmin()`, `hasRole()`, `hasAuthority()`).
- **`shared.audit`**: `AuditMetadata` e `AuditorAwareImpl` alimentado automaticamente pelo token JWT.
- **`shared.pagination`**: `CursorPage` e `CursorEncoder` operando em Base64URL determinístico sem padding.
- **`shared.util`**: `SlugUtils` (`resolveSlug()`) centralizando a normalização determinística de slugs amigáveis.
- **`shared.storage`**: `StorageProperties`, `S3ClientConfig` (resiliente com auto-criação de bucket), `StorageService` e `S3StorageService` compatível com AWS S3 / SeaweedFS via AWS SDK v2 com `forcePathStyle(true)`.
- **`shared.config`**: `WebMvcConfig` aplicando prefixo global `/api/v1` para controllers de `com.ecommerce`.

---

## 5. Log de Decisões Importantes (Micro-ADRs dos Cards)
- **[Card 001] Precisão Monetária**: `Money` utiliza inteiros de 64 bits (`long amountMinor`) e serialização customizada no Mongo (`MoneyWritingConverter` / `MoneyReadingConverter`) para garantir precisão absoluta sem float/double.
- **[Card 001] Paginação Segura**: `CursorEncoder` adota Base64URL sem padding e separador determinístico `::` para paginação estável e rápida.
- **[Card 002] Segurança Stateless**: Autenticação estritamente stateless via JWT no `SecurityFilterChain`, sem sessão e sem banco relacional de usuários. Integração direta com `AuditorAware` propagando `sub` para `createdBy`/`updatedBy`.
- **[Card 002] Prefixo de API Centralizado**: Prefixo `/api/v1` aplicado globalmente via `WebMvcConfig` para evitar repetições manuais em controllers.
- **[Card 003] Ancestors Path em Categorias**: Armazenamento do array ordenado `ancestors` (`[rootId, subId]`) para resolução instantânea de herança e consultas indexadas sem recursão de banco.
- **[Card 003] Isolamento de Entidades no Service**: `CategoryService` expõe exclusivamente records DTOs (`CategoryView`, `AttributeDefinition`) para outros módulos, impedindo o vazamento de instâncias `@Document` no monolito.
- **[Card 003] Migrações Centralizadas**: Todas as classes Mongock residem unicamente em `com.ecommerce.shared.db.migrations` para manter histórico unificado e controle de ordem de execução.
- **[Card 004] Attribute Pattern & Validação Estrita**: Produtos armazenam atributos dinâmicos em array heterogêneo `{ k, v, unit }` indexado com multikey, validados estritamente contra as regras da taxonomia (`CategoryService.getEffectiveAttributes`).
- **[Card 004] Concorrência Otimista com If-Match**: Endpoints de mutação (`PATCH`) exigem cabeçalho `If-Match` alinhado à anotação `@Version` do Spring Data Mongo; inconsistências geram `409 CONCURRENT_MODIFICATION`.
- **[Card 004] Centralização de Segurança e Utilitários de Slug**: Extração de `SecurityUtils` e `SlugUtils` para o kernel compartilhado (`com.ecommerce.shared`), evitando acoplamento de `SecurityContextHolder` e duplicação de normalização de slugs nos controllers e services.
- **[Card 005] Agregação em Viagem Única e Subpacote `repository`**: Execução de busca facetada via MongoDB Aggregation Pipeline com `$match` e `$facet` em viagem única (`ProductSearchRepository`), combinando resultados paginados, contagem total e facetas dinâmicas. Isolamento de `ProductRepository` e `ProductSearchRepository` no subpacote `com.ecommerce.product.repository` para manter a raiz do módulo focada exclusivamente na API pública, controller e service.
- **[Card 008] Eliminação de Dual-Write com Transactional Outbox**: Substituição da chamada síncrona de deleção ao storage S3 em `ProductService.removeImage` por gravação atômica transacional no MongoDB (`ProductDocument` + `OutboxDocument`). O processamento físico da deleção no S3 é delegado ao `OutboxProcessor` assíncrono com tolerância a falhas e política de 5 retries. O método `addImage` opera sem `@Transactional` para evitar retenção de conexões durante upload de rede, contando com a atomicidade nativa de documento único do MongoDB e compensação com fallback para o outbox.
