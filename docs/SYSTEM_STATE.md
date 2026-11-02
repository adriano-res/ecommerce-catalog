# Estado Atual do Sistema (System State)

> **Documento Vivo**: Mantido automaticamente pelas skills do Kanban (`task-close`, `task-plan`).  
> **Propósito**: Servir como memória executiva ultracompacta para agentes e humanos, garantindo que o estado real do código seja consultado rapidamente sem consumir grandes volumes de tokens.

---

## 1. Status do Kanban de Desenvolvimento
- **Cards Concluídos (`done/`)**:
  - `001`: Setup Inicial do Projeto (Spring Boot 3.5, Java 21, Mongo replica set, Redis, Modulith e Shared Kernel).
  - `002`: Autenticação e Segurança Stateless via JWT (`/api/v1/auth/dev-token`, `JwtAuthenticationFilter`, RBAC `ADMIN`/`USER`).
- **Card Atual em Andamento (`doing/`)**:
  - `003`: Taxonomia e Categorias Hierárquicas (`com.ecommerce.category`, *Ancestors Path*, cache-aside no Redis).
- **Cards Planejados no Pipeline (`prototype/`)**:
  - `004`: Produtos com Attribute Pattern e Ciclo de Vida (`DRAFT`, `ACTIVE`, `ARCHIVED`).
  - `005`: Busca Facetada com Aggregation Pipeline e Paginação por Cursor.
  - `006`: Cache-Aside no Redis para Produtos e Invalidação Reativa via Domain Events.
  - `007`: Observabilidade, Auditoria Centralizada e Validação de Fronteiras Modulith.

---

## 2. Módulos e Bounded Contexts Ativos no Código

| Módulo / Pacote | Papel Principal | Componentes Chave |
| :--- | :--- | :--- |
| **`auth`** (`com.ecommerce.auth`) | Emissão de tokens JWT dev (stateless) | `DevTokenController`, `DevTokenRequest`, `DevTokenResponse` |
| **`category`** (`com.ecommerce.category`) | Gestão de taxonomia hierárquica e herança de atributos | `CategoryController`, `CategoryService`, `CategoryRepository`, `CategoryDocument`, `CategoryCache` |
| **`shared`** (`com.ecommerce.shared`) | Kernel transversal reutilizado entre módulos | `money`, `error`, `audit`, `pagination`, `security`, `config`, `cache`, `enums`, `db.migrations` |

---

## 3. Banco de Dados e Cache Ativos

### Coleções MongoDB (`ecommerce_catalog`)
- **`categories`** (gerenciada por `category`):
  - Documento: `CategoryDocument` (id, slug, name, parentId, ancestors, attributeDefinitions, audit).
  - Migrações Mongock ativas:
    - `V001_CreateCategoryIndexes`: Índice único em `slug`, índices secundários em `parentId` e multikey em `ancestors`.

### Namespaces de Chave no Redis
- **`cache:category-tree`**: Serialização JSON da árvore completa de categorias (TTL: 30 min, invalidada via `CacheService` a cada mutação de taxonomia).

---

## 4. Kernel Compartilhado (`com.ecommerce.shared`)
- **`shared.money`**: Value Object imutável `Money` (armazena centavos em `long amountMinor` com `Currency` ISO-4217; proibição estrita de float/double). Conversores customizados no MongoDB para evitar perda de precisão.
- **`shared.error`**: `DomainException`, taxonomia `FailureCode` e `ProblemDetailHandler` com suporte a RFC 7807, correlationId e detalhes de validação.
- **`shared.security`**: `SecurityConfig` stateless, `JwtService` com HMAC-SHA256, e `JwtAuthenticationFilter` integrado ao `MDC` (`correlationId`, `actor`).
- **`shared.audit`**: `AuditMetadata` e `AuditorAwareImpl` alimentado automaticamente pelo token JWT.
- **`shared.pagination`**: `CursorPage` e `CursorEncoder` operando em Base64URL determinístico sem padding.
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
