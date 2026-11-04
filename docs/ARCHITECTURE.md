# Arquitetura do Sistema (System Architecture & Design)

**Versão:** 2.0  
**Data:** Outubro de 2026  
**Escopo:** E-commerce Catalog & Faceted Search Engine  
**Documentos Relacionados:** [CODING_GUIDELINES.md](file:///workspace/ecommerce-catalog/docs/CODING_GUIDELINES.md), [SPECIFICATION.md](file:///workspace/ecommerce-catalog/docs/SPECIFICATION.md) e [TEMPLATE_TAREFA.md](file:///workspace/ecommerce-catalog/docs/TEMPLATE_TAREFA.md)

---

## 1. Visão Geral e Princípios Arquiteturais

O **E-commerce Catalog & Faceted Search Engine** é construído como um **Monolito Modular em Java 21** com **Spring Boot 3.5+** e **Spring Modulith**. Este documento funciona como um **guia normativo de arquitetura**, orientando onde posicionar novos arquivos e componentes criados no projeto.

### Princípios Norteadores de Organização de Código
1. **Convenção de Pacote por Feature (`com.ecommerce.<modulo>`):** O código é organizado por módulos funcionais de primeiro nível (`auth`, `category`, `product`, `outbox`) mantendo a raiz do pacote limpa e focada na espinha dorsal operacional:
   - **Na raiz do pacote do módulo:** Vivem unicamente a espinha dorsal operacional essencial e a facade pública:
     - `*Controller.java` (Entrada e adaptação de transporte HTTP REST)
     - `*Service.java` (Núcleo de regras de negócio, transações e orquestração)
     - `*Api.java` (Facade pública para consumo síncrono por outros módulos)
     - `*Repository.java` (Interface pura de persistência e I/O no banco, em módulos de repositório único)
   - **No subpacote `repository/` (`com.ecommerce.<modulo>.repository`):** Isolamento estrito de persistência e queries (`*Repository.java`, `*SearchRepository.java`), agrupando repositórios e despoluindo a raiz do módulo.
   - **No subpacote `entity/` (`com.ecommerce.<modulo>.entity`):** Isolamento estrito de persistência: documentos `@Document`, subdocumentos de banco de dados e enums de estado interno do módulo.
   - **No subpacote `dto/` (`com.ecommerce.<modulo>.dto`):** Centralização de todos os records de transferência de dados (`*Request.java`, `*Response.java`, snapshots e agrupamentos), anotado com `@NamedInterface("dto")`.
   - **No subpacote `validation/` (`com.ecommerce.<modulo>.validation`):** Validadores especializados de regras e integridade de domínio desacoplados do serviço.
   - **No subpacote `mapper/` (`com.ecommerce.<modulo>.mapper`):** Funções puras utilitárias de conversão DTO <-> Document.
   - **No subpacote `jobs/` (`com.ecommerce.<modulo>.jobs`):** Processadores em background, workers assíncronos e agendadores periódicos (`@Scheduled`) responsáveis pela execução de tarefas desacopladas do ciclo síncrono da requisição HTTP (ex: `OutboxProcessor`).
2. **Centralização Transversal no `shared` (`com.ecommerce.shared`):** Qualquer componente, enum, modelo, configuração ou infraestrutura que seja utilizado por mais de um módulo deve residir obrigatoriamente sob o pacote `com.ecommerce.shared` (ex: `shared.enums`, `shared.security`, `shared.error`, `shared.audit`, `shared.money`, `shared.pagination`, `shared.config`, `shared.storage`, `shared.db.migrations`).
3. **Propriedade Exclusiva de Dados (*Collection per Module*):** Cada coleção do MongoDB e namespace de chave do Redis pertence exclusivamente a um único módulo. Nenhum módulo executa queries, mutações ou leituras diretas em coleções de outro módulo.
4. **Simetria e Uniformidade Arquitetural (POLA — Principle of Least Astonishment):** Todos os módulos seguem a mesma convenção em camadas (`Controller` → `Service` → `Repository`), com nomes previsíveis para DTOs, mappers e documentos.
5. **Isolamento de I/O e Inversão de Controle:** Repositórios realizam I/O puro no MongoDB; serviços orquestram regras e fronteiras de transação; comunicação síncrona entre módulos usa facades públicas (`*Api`).
6. **MongoDB como Autoridade, Redis como Acelerador Efêmero:** O Redis funciona exclusivamente como camada de cache-aside. Se o Redis falhar, a aplicação degrada suavemente para o MongoDB sem indisponibilidade ou perda de dados.
7. **Centralização Estrita de Migrações de Banco (Mongock):** Todas as migrações DDL e criação de índices MongoDB devem residir exclusivamente em `com.ecommerce.shared.db.migrations`. Módulos de feature (`category`, `product`, `search`) são estritamente proibidos de conter classes de migração para evitar fragmentação do histórico do banco de dados.

---

## 2. Estrutura de Diretórios e Guia de Posicionamento de Arquivos

Ao criar novos arquivos no projeto, consulte a estrutura padronizada abaixo como referência de posicionamento:

```text
ecommerce-catalog/
├── docs/
│   ├── ARCHITECTURE.md                      # Estrutura do código, camadas e SRP
│   ├── CODING_GUIDELINES.md                 # Padrões de código Java e boas práticas
│   ├── SPECIFICATION.md                     # Requisitos, fluxos, diagramas e contratos
│   └── TEMPLATE_TAREFA.md                   # Template para tarefas do kanban
├── docker-compose.yml                       # MongoDB 7 (rs0) + Redis 7
├── .env.example                             # Variáveis de ambiente de referência
├── pom.xml                                  # Dependências (Spring Boot, Modulith, Testcontainers)
└── src/
    ├── main/
    │   ├── java/com/ecommerce/
    │   │   ├── CatalogApplication.java     # Bootstrap (@SpringBootApplication + @Modulithic)
    │   │   │
    │   │   ├── shared/                      # Módulo OPEN: Componentes reutilizados entre módulos
    │   │   │   ├── money/                   # Value Object imutável Money (cents em long + Currency)
    │   │   │   ├── error/                   # DomainException, FailureCode, ProblemDetailHandler (RFC 7807)
    │   │   │   ├── audit/                   # AuditMetadata (createdAt/By, updatedAt/By) e AuditorAware
    │   │   │   ├── pagination/              # CursorPage, CursorEncoder e decodificador Base64URL
    │   │   │   ├── enums/                   # Enums transversais compartilhados (ex: UserRole)
    │   │   │   ├── security/                # Infraestrutura Spring Security (SecurityConfig, JwtService, JwtAuthenticationFilter, SecurityUtils)
    │   │   │   ├── config/                  # MongoConfig, RedisConfig, JacksonConfig, WebMvcConfig (prefixo global /api/v1)
    │   │   │   ├── cache/                   # CacheService resiliente (get, put com TTL, evict)
    │   │   │   ├── storage/                 # Storage transversal S3/SeaweedFS (StorageProperties, S3ClientConfig, StorageService)
    │   │   │   ├── util/                    # Utilitários transversais (ex: SlugUtils)
    │   │   │   └── db/                      # Infraestrutura de banco e migrações versionadas
    │   │   │       └── migrations/          # ChangeUnits sequenciais do Mongock (V001_..., V002_..., V003_...)
    │   │   │
    │   │   ├── auth/                        # Feature: Autenticação e tokens dev
    │   │   │   ├── DevTokenController.java  # Controller na raiz do módulo
    │   │   │   └── dto/                     # DTOs de request/response encapsulados
    │   │   │       ├── DevTokenRequest.java
    │   │   │       └── DevTokenResponse.java
    │   │   │
    │   │   ├── category/                    # Feature: Taxonomia e Definição de Atributos (US-01)
    │   │   │   ├── CategoryController.java   # Controller na raiz do módulo
    │   │   │   ├── CategoryService.java      # Service na raiz do módulo (regras de negócio, cache e consultas)
    │   │   │   ├── CategoryRepository.java   # Repository na raiz do módulo
    │   │   │   ├── entity/                   # Persistência isolada
    │   │   │   │   └── CategoryDocument.java # Mapeamento @Document("categories") com audit
    │   │   │   ├── mapper/                   # Conversores puros
    │   │   │   │   └── CategoryMapper.java   # Conversão DTO <-> Document
    │   │   │   └── dto/                      # DTOs de request/response encapsulados (@NamedInterface)
    │   │   │       ├── CreateCategoryRequest.java
    │   │   │       ├── CategoryResponse.java
    │   │   │       ├── CategoryTreeResponse.java
    │   │   │       ├── CategoryView.java
    │   │   │       └── AttributeDefinition.java
    │   │   │
    │   │   ├── product/                     # Feature: Produtos, Ciclo de Vida e Busca Facetada (US-02, US-03, US-04)
    │   │   │   ├── ProductController.java    # Controller na raiz do módulo (CRUD e busca facetada)
    │   │   │   ├── ProductService.java       # Service na raiz do módulo (regras de negócio, cache-aside direto e orquestração)
    │   │   │   ├── ProductApi.java           # Facade pública para outros módulos
    │   │   │   ├── repository/               # Camada isolada de persistência e queries
    │   │   │   │   ├── ProductRepository.java       # MongoRepository de produtos
    │   │   │   │   └── ProductSearchRepository.java # Aggregation Pipeline com $match e $facet
    │   │   │   ├── entity/                   # Persistência isolada
    │   │   │   │   ├── ProductDocument.java  # Mapeamento @Document("products") (Attribute Pattern)
    │   │   │   │   ├── AttributeValue.java   # Subdocumento BSON (k, v, unit)
    │   │   │   │   ├── ProductImage.java     # Subdocumento de imagem
    │   │   │   │   └── ProductStatus.java    # Enum de ciclo de vida (DRAFT, ACTIVE, ARCHIVED)
    │   │   │   ├── validation/               # Regras de negócio desacopladas
    │   │   │   │   ├── AttributeValidator.java # Validador de tipos/enums contra CategoryService
    │   │   │   │   └── ImageValidator.java   # Validador de payloads e formatos de imagem
    │   │   │   ├── mapper/                   # Conversores puros
    │   │   │   │   └── ProductMapper.java    # Mapeamento DTO <-> Document
    │   │   │   └── dto/                      # DTOs de request/response encapsulados (@NamedInterface)
    │   │   │       ├── CreateProductRequest.java
    │   │   │       ├── UpdateProductRequest.java
    │   │   │       ├── ProductResponse.java
    │   │   │       ├── ProductSnapshot.java
    │   │   │       ├── SearchFilterRequest.java
    │   │   │       ├── SearchSort.java
    │   │   │       ├── SearchFacetResponse.java
    │   │   │       ├── FacetOption.java
    │   │   │       ├── PriceRangeFacet.java
    │   │   │       ├── ProductSearchItemResponse.java
    │   │   │       └── ProductSearchResult.java
    │   │   │
    │   │   └── outbox/                      # Feature: Transactional Outbox e Consistência Assíncrona
    │   │       ├── OutboxApi.java           # Facade pública para publicação atômica de eventos (publish)
    │   │       ├── OutboxService.java       # Service na raiz do módulo
    │   │       ├── OutboxRepository.java    # Repository na raiz do módulo
    │   │       ├── entity/                  # Persistência isolada
    │   │       │   ├── OutboxDocument.java  # Mapeamento @Document("outbox_events")
    │   │       │   └── OutboxStatus.java    # Enum de ciclo de vida (PENDING, PROCESSING, COMPLETED, FAILED)
    │   │       └── jobs/                    # Processadores em background
    │   │           └── OutboxProcessor.java # Worker agendado com @Scheduled e retries
    │   │
    │   └── resources/
    │       ├── application.yml              # Configurações gerais e fail-fast
    │       ├── application-local.yml        # Perfil local (pretty logging)
    │       └── logback-spring.xml           # Configuração de logs estruturados (JSON stdout)
    │
    └── test/java/com/ecommerce/
        ├── ModularityTests.java             # Validação estrita de fronteiras com Spring Modulith
        ├── shared/                          # Testes unitários do kernel compartilhado
        │   ├── money/                       # MoneyTest
        │   ├── error/                       # ProblemDetailHandlerTest
        │   ├── audit/                       # AuditorAwareTest
        │   ├── pagination/                  # CursorEncoderTest
        │   └── security/                    # JwtServiceTest, SecurityFilterChainTest
        ├── auth/                            # Testes da feature de autenticação
        │   └── DevTokenControllerTest.java  # DevTokenControllerTest (MockMvc)
        ├── category/                        # Testes da feature de Category
        │   ├── CategoryServiceIT.java
        │   └── CategoryCacheIT.java
        ├── product/                         # Testes da feature de Product
        │   ├── ProductServiceIT.java
        │   ├── ProductConcurrencyIT.java
        │   ├── ProductImageUploadIT.java
        │   ├── ProductSearchIT.java
        │   └── AttributeValidatorTest.java
        ├── outbox/                          # Testes da feature de Outbox
        │   └── jobs/                        # OutboxProcessorTest.java
        └── support/                         # Suporte compartilhado (MongoDB e Redis Testcontainers)
            ├── AbstractIntegrationTest.java
            └── TestContainersConfig.java
```

---

## 3. Arquitetura em Camadas e Responsabilidades (SRP)

Cada módulo de domínio organiza seu código interno seguindo estritamente a separação em camadas:

```mermaid
flowchart TD
    Client(["Cliente HTTP / Web"]) -->|JSON Request| Controller["Controller (@RestController)"]
    OtherModule(["Outro Módulo"]) -->|Chamada Síncrona| Facade["Facade Pública (*Api)"]
    EventBroker(["Spring Event Bus"]) -->|Evento Assíncrono| Listener["Listener (@ApplicationModuleListener)"]

    Controller -->|DTO Request| Service["Service (Regras de Negócio & Orquestração)"]
    Facade -->|Implementação| Service
    Listener --> Service

    Service -->|I/O Puro| Repository["Repository (Spring Data / MongoTemplate)"]
    Service -->|Cache-Aside| Cache["Cache Component (Lettuce / RedisTemplate)"]
    Service -->|publishEvent| EventPublisher["ApplicationEventPublisher"]
    Service -.->|Conversão Pura| Mapper["Mapper (Static Pure Methods)"]
    Service -.->|Validação Especializada| Validator["Validator (Componente de Regra)"]

    Repository --> Mongo[("MongoDB 7")]
    Cache --> Redis[("Redis 7")]
```

### 3.1. Controllers (`*Controller.java`)
- **Papel:** Camada de adaptação de transporte HTTP (REST).
- **Responsabilidades:**
  - Decodificar payloads JSON e validar restrições via Bean Validation (`@Valid`, `@NotNull`).
  - Extrair cabeçalhos obrigatórios (`If-Match` para controle de concorrência, `X-Actor` para auditoria).
  - Delegar a execução para o `Service` do módulo.
  - Serializar DTOs de resposta (`record`) com status HTTP semântico (`200 OK`, `201 Created`, `204 No Content`).
  - Mapear exceções de domínio para a RFC 7807 (`ProblemDetail`) via `@RestControllerAdvice`.
- **Proibições:**
  - Nunca acessar repositórios (`*Repository`) ou templates (`MongoTemplate`, `RedisTemplate`) diretamente.
  - Nunca conter regras de negócio, validações taxonômicas ou cálculos.
  - Nunca retornar instâncias de `@Document` para o cliente HTTP.

### 3.2. Services (`*Service.java`)
- **Papel:** Núcleo de regras de negócio, validações de domínio e orquestração.
- **Responsabilidades:**
  - Desacoplados de qualquer protocolo HTTP (`HttpServletRequest`, headers web).
  - Coordenar o fluxo: validar contra outras facades, consultar o cache/banco, executar mutações e disparar eventos.
  - Definir demarcações transacionais (`@Transactional`) quando houver gravação multi-documento ou registro de outbox.
  - Publicar eventos de domínio via `ApplicationEventPublisher`.
  - Aplicar tratamento de concorrência otimista capturando `OptimisticLockingFailureException`.
- **Proibições:**
  - Nunca chamar repositórios ou componentes internos de outros módulos diretamente (a comunicação síncrona deve ocorrer via facade `*Api` e a assíncrona via eventos `*Changed`).
  - Nunca conter código de formatação HTTP ou serialização JSON.

### 3.3. Facades Públicas (`*Api.java`)
- **Papel:** Contrato público exposto pelo módulo para consumo síncrono por outros domínios.
- **Responsabilidades:**
  - Interface Java localizada na raiz do pacote do módulo (ex: `com.ecommerce.category.CategoryApi`).
  - Recebe e devolve exclusivamente tipos públicos primitivos, Value Objects de `shared` ou DTOs `record` imutáveis.
- **Proibições:**
  - Nunca vazar entidades internas, documentos `@Document` ou exceções de infraestrutura do driver MongoDB/Redis.

### 3.4. Repositories (`*Repository.java` e `*SearchRepository.java`)
- **Papel:** Camada pura de I/O de dados com o MongoDB.
- **Responsabilidades:**
  - Residir no subpacote `repository/` do módulo (ex: `com.ecommerce.product.repository.ProductRepository` e `ProductSearchRepository`) ou na raiz em módulos com repositório único (ex: `CategoryRepository`).
  - Herdar de `MongoRepository<T, ID>` para operações CRUD convencionais.
  - Utilizar `MongoTemplate` de forma limpa para montagem de pipelines de agregação complexos (`$match`, `$facet`, `$unwind`, `$bucket`).
  - Executar consultas cobertas por índices sem carregar dados desnecessários na memória.
- **Proibições:**
  - Nenhuma regra de negócio ou validação taxonômica deve ser executada dentro do repositório.

### 3.5. Entidades de Persistência, Validadores e Mappers
- **`entity/*Document.java`:**
  - Reside exclusivamente no subpacote `entity/` do módulo, mantendo a raiz despoluída.
  - Representa o esquema persistido no MongoDB anotado com `@Document("colecao")`.
  - Subdocumentos (ex: `AttributeValue`, `ProductImage`) e enums de estado interno residem dentro de `entity/`.
  - Contém anotações de auditoria (`@CreatedDate`, `@LastModifiedDate`, etc.) e `@Version` para locking otimista.
- **`validation/*Validator.java`:**
  - Reside no subpacote `validation/` do módulo.
  - Componente `@Component` com validações especializadas de regras e integridade de domínio, evitando inflar o Service.
- **`mapper/*Mapper.java`:**
  - Reside no subpacote `mapper/` do módulo.
  - Classe utilitária com métodos estáticos puros que convertem DTOs em Documents e vice-versa.
  - Não possui dependências com banco de dados ou contexto Spring.

### 3.6. Caches e Resiliência (`CacheService`)
- **Padrão Cache-Aside Direto:**
  - Serviços de negócio injetam diretamente `CacheService` (`com.ecommerce.shared.cache.CacheService`), eliminando camadas intermediárias artificiais e indireção de eventos quando a invalidação síncrona for preferida.
  - O `CacheService` encapsula o acesso resiliente ao Redis (`StringRedisTemplate`), implementando tratamento fail-safe com logs de `WARN` e fallback transparente para o MongoDB.
  - Impõe TTL obrigatório em todas as gravações (ex: 10 min para detalhes de produtos, 30 min para árvore de categorias).
  - Invalidação pontual e cirúrgica executada diretamente no momento de cada mutação com sucesso.

### 3.7. Workers em Background e Processadores Agendados (`jobs/*Processor.java` e `jobs/*Worker.java`)
- **Papel:** Execução assíncrona desacoplada da camada HTTP síncrona.
- **Responsabilidades:**
  - Residir exclusivamente no subpacote `jobs/` do respectivo módulo (ex: `com.ecommerce.outbox.jobs.OutboxProcessor`).
  - Anotar métodos de execução periódica com `@Scheduled(fixedDelay = ...)` ou `@Scheduled(cron = ...)`.
  - Processar tarefas e mensagens de forma idempotente, com controle estrito de retentativas (`retryCount`), tolerância a falhas transitórias e registro estruturado de erros com SLF4J (`log.warn`, `log.error`).
  - Interagir com outros módulos exclusivamente através de interfaces públicas ou facades (`*Api`).
- **Proibições:**
  - Nunca bloquear ou sincronizar o processamento de background com a resposta imediata da requisição HTTP.
  - Nunca expor controllers ou endpoints REST dentro do subpacote `jobs/`.

---

## 4. Padrões de Persistência, Consistência e Modelagem

### 4.1. Mapeamento de Coleções e Chaves por Módulo

Para garantir a regra de ouro de fronteiras de dados (*Collection per Module*):

| Domínio / Módulo | Coleção MongoDB | Chaves Redis | Responsabilidade Exclusiva |
| :--- | :--- | :--- | :--- |
| `auth` | — *(stateless)* | — | Emissão de tokens JWT para ambiente de desenvolvimento (`/api/v1/auth/dev-token`). |
| `category` | `categories` | `cache:category-tree` | Estrutura hierárquica, slugs e catálogo de definições de atributos. |
| `product` | `products` | `cache:product:{idOrSlug}` | Cadastro de produtos, ciclo de vida, Attribute Pattern, locking otimista e motor de busca facetada via `ProductSearchRepository`. |
| `outbox` | `outbox_events` | — | Registro e consumo transacional de eventos assíncronos (ex: deleção e compensação de storage). |
| Spring Modulith | `event_publication` | — | Registro transacional de outbox para garantia de entrega de eventos. |

---

### 4.2. Implementação do Attribute Pattern no Código Java

No módulo `product`, atributos heterogêneos chegam via API como um mapa amigável `Map<String, Object>` e são persistidos no MongoDB como lista tipada de subdocumentos para permitir indexação uniforme:

```java
// Estrutura física persistida no ProductDocument:
public record AttributeValue(
    @Field("k") String key,
    @Field("v") Object value,
    @Field("unit") String unit
) {}
```

- **Mapeamento:** O `ProductMapper` traduz `{ "ram": "8GB", "storageGb": 256 }` para `[ { "k": "ram", "v": "8GB" }, { "k": "storageGb", "v": 256, "unit": "GB" } ]`.
- **Indexação:** Uma única entrada de índice no MongoDB cobre qualquer busca por atributo:
  `{ "attributes.k": 1, "attributes.v": 1 }`.

---

### 4.3. Resolução de Ancestrais e Navegação Hierárquica

O módulo `category` implementa o padrão *Ancestors Path*:
- Ao criar uma categoria com `parentId`, o `CategoryService` recupera os ancestrais do pai e concatena o ID do pai, gravando:
  `ancestors: [rootId, subcategoryParentId]`.
- Ao cadastrar um produto, o `ProductService` busca os ancestrais da categoria via `CategoryService` e preenche `categoryPath`:
  `categoryPath: [rootId, subcategoryParentId, categoryId]`.
- **Benefício:** Uma query por produtos da categoria pai usa o índice composto `{ "status": 1, "categoryPath": 1 }`, encontrando produtos de todas as subcategorias filhas instantaneamente.

---

### 4.4. Controle de Concorrência Otimista com `@Version`

- A classe `ProductDocument` declara um atributo `private Long version;` anotado com `@Version`.
- Em atualizações (`PATCH /api/v1/products/{id}`), o cliente HTTP deve enviar o cabeçalho `If-Match: <versao>`.
- Se outro usuário tiver atualizado o produto entre a leitura e a escrita, o Spring Data lança `OptimisticLockingFailureException`. O `ProblemDetailHandler` intercepta e retorna `409 Conflict` com código `CONCURRENT_MODIFICATION`, prevenindo sobrescritas silenciosas.

---

### 4.5. Migrações e Versionamento de Banco de Dados (Mongock)

Para garantir integridade de esquema em produção e cumprir a regra **RNF-04** (`auto-index-creation = false`):

- **Localização Única:** Todas as classes de migração e criação de índices devem residir exclusivamente no pacote centralizado `com.ecommerce.shared.db.migrations`.
- **Proibição de Migrações em Features:** Nenhuma classe `@ChangeUnit` pode ser criada dentro dos pacotes de módulos de negócio (`category`, `product`, `search`, etc.). Isso evita fragmentação do histórico e mantém a ordem cronológica em um ponto único de auditoria.
- **Convenção de Nomenclatura:** `V{NNN}_{DescricaoEmCamelCase}.java` (ex: `V001_CreateCategoryIndexes.java`, `V002_CreateProductIndexes.java`).
- **Configuração do Mongock:** A propriedade `mongock.migration-scan-package` no `application.yml` aponta unicamente para `com.ecommerce.shared.db.migrations`.
