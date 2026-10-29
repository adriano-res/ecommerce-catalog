# Diretrizes e Boas Práticas de Codificação (Coding Guidelines — Java + Spring Modulith + MongoDB + Redis)

**Versão:** 2.0  
**Data:** Outubro de 2026  
**Escopo:** Java 21 LTS, Spring Boot 3.5+, Spring Modulith, Spring Data MongoDB, Spring Data Redis (Lettuce), Mongock  
**Documentos Relacionados:** [ARCHITECTURE.md](file:///workspace/ecommerce-catalog/docs/ARCHITECTURE.md), [SPECIFICATION.md](file:///workspace/ecommerce-catalog/docs/SPECIFICATION.md) e [TEMPLATE_TAREFA.md](file:///workspace/ecommerce-catalog/docs/TEMPLATE_TAREFA.md)

---

## 1. Visão Geral

Este documento estabelece os padrões de qualidade de código, convenções idiomáticas em **Java moderno**, regras estritas de modularidade com Spring Modulith, modelagem de dados no MongoDB e boas práticas de cache com Redis para o projeto **E-commerce Catalog & Faceted Search Engine**.

Para visão estrutural, fluxos de agregação e arquitetura de módulos, consulte o [ARCHITECTURE.md](file:///workspace/ecommerce-catalog/docs/ARCHITECTURE.md).

---

## 2. Estilo de Código e Diretrizes Gerais

* **Idioma Obrigatório no Código e Banco de Dados (English Only):**
  * Todos os identificadores (**pacotes, classes, records, interfaces, métodos, variáveis, constantes, enums, exceções**) e elementos de persistência (**coleções, campos de documentos, índices, chaves Redis, nomes de eventos**) **devem ser estritamente escritos em inglês**.
  * O uso de "Portunglês" em qualquer identificador é proibido.

* **Código Autoexplicativo e Limpo (Clean Code):**
  * **Proibição de comentários procedurais inline** explicando *o que* o código faz (ex: `// 1. busca produto`, `// valida se existe`).
  * O código deve ser autoexplicativo através de nomes intencionais e métodos curtos.
  * Comentários são reservados para **Javadoc em contratos públicos de facade** (`CategoryApi`, `ProductApi`, `SearchApi`), eventos e Value Objects, ou para explicar o **porquê** de decisões algorítmicas não óbvias (como pipelines de agregação do MongoDB).
  * Formatação automática padronizada com **Spotless + google-java-format** (`./mvnw spotless:apply`).

* **Java Moderno e Imutabilidade por Padrão:**
  * DTOs de requisição, resposta, eventos de domínio, snapshots e Value Objects **devem ser `record`**.
  * Hierarquias fechadas de tipos devem utilizar **`sealed interface`** combinadas com pattern matching em expressões `switch`.
  * `Optional` é permitido **exclusivamente como tipo de retorno de métodos**; nunca como campo de classe, parâmetro de método ou elemento de coleções.
  * Coleções expostas devem ser imutáveis (`List.copyOf`, `Set.copyOf`, `Stream.toList()`).
  * Métodos de facade pública nunca devem retornar `null`; utilize `Optional` para ausências esperadas ou lance exceção de domínio.

* **Injeção de Dependências e Concorrência:**
  * **Injeção exclusivamente por construtor**, com atributos `private final`. O uso de `@Autowired` em atributos é proibido.
  * Proibição de estado global mutável (`static Map`, singletons manuais).
  * Habilitação de **Virtual Threads** (`spring.threads.virtual.enabled=true`) para concorrência barata de I/O em endpoints síncronos.

* **Logger Estruturado e Observabilidade (Twelve-Factor App):**
  * Utilizar **SLF4J** (`private static final Logger log = LoggerFactory.getLogger(X.class)`) com parametrização de mensagens (`log.info("Product published sku={}", sku)`). Concatenação manual de strings em logs é proibida.
  * O `correlationId` deve ser propagado via **MDC** pelo filtro HTTP e repassado para listeners assíncronos.
  * Saída padronizada em `stdout`: legível para humanos no perfil `local` e formato JSON estruturado em ambientes de container.
  * Chamadas a `System.out.println`, `System.err.println` ou `e.printStackTrace()` são proibidas em código de produção.

* **Simetria Arquitetural e Simplicidade (KISS / YAGNI / POLA):**
  * Componentes que desempenham o mesmo papel em módulos distintos seguem rigorosamente a mesma nomenclatura e assinatura: `Controller` → `Service` → `Repository`.
  * Não crie interfaces especulativas com apenas uma implementação para serviços internos. Interfaces são reservadas para **facades públicas entre módulos** (`CategoryApi`, `ProductApi`, `SearchApi`).

---

## 3. Regras de Modularidade (Spring Modulith)

* **Fronteiras Verificadas por Testes:** O teste `ModularityTests` executa `ApplicationModules.of(CatalogApplication.class).verify()` e deve passar limpo em todos os commits. Violações de fronteira quebram o build.
* **Encapsulamento de Pacotes:** Apenas os pacotes raiz de cada módulo (`com.ecommerce.category`, `com.ecommerce.product`, `com.ecommerce.search`) são visíveis externamente. Tudo contido em `internal/` é estritamente privado ao módulo.
* **Tipos Permitidos na API Pública:** Facades públicas (`*Api`), DTOs `record` e eventos de domínio (`*Changed`). É terminantemente proibido expor classes `@Document`, repositórios ou serviços internos.
* **Isolamento de Persistência:** Um módulo **nunca** injeta repositórios, executa queries ou lê coleções/chaves gerenciadas por outro módulo. Toda interação é feita através da facade pública ou de eventos.
* **Eventos de Domínio:**
  * Nomeados no particípio passado (`ProductChanged`, `CategoryChanged`) como `record` imutável.
  * Publicados via `ApplicationEventPublisher` e consumidos com `@ApplicationModuleListener` (assíncrono, após commit da transação do Spring Modulith).
  * Listeners devem ser **idempotentes**, pois o registry de eventos pode reentregar mensagens em caso de retry.
* **Módulo `shared`:** Marcado como `@ApplicationModule(type = Type.OPEN)`. Contém apenas utilitários transversais sem regra de negócio e nunca depende de módulos de domínio.

---

## 4. Convenções de Nomenclatura

| Elemento | Convenção | Exemplo |
| :--- | :--- | :--- |
| **Pacotes** | `lowercase`, palavra única por segmento | `com.ecommerce.product.internal` |
| **Classes / Records** | `PascalCase` com sufixo do papel arquitetural | `ProductService`, `ProductRepository`, `ProductDocument`, `ProductController` |
| **Facades Públicas** | `<Module>Api` | `CategoryApi`, `ProductApi`, `SearchApi` |
| **DTOs HTTP** | `<Action><Entity>Request` / `<Entity>Response` | `CreateProductRequest`, `ProductResponse` |
| **Eventos de Domínio** | Substantivo + Verbo no Passado | `ProductChanged`, `CategoryChanged` |
| **Coleções MongoDB** | `snake_case` no plural | `categories`, `products` |
| **Campos de Documento** | `camelCase` | `categoryPath`, `amountMinor`, `attributeDefinitions` |
| **Chaves no Redis** | `cache:<namespace>:<identificador>` | `cache:product:{idOrSlug}`, `cache:category-tree` |
| **Testes Automatizados** | `<Classe>Test` (unitário) / `<Classe>IT` (integração) | `MoneyTest`, `ProductSearchIT` |
| **Métodos de Teste** | `should<ResultadoEsperado>_when<Cenário>` | `shouldReturnFacets_whenSearchFiltersAreApplied` |

---

## 5. Diretrizes de MongoDB e Migrations

* **Validação de Esquema e Índices via Mongock:**
  * Toda definição de índice ou validador `$jsonSchema` reside exclusivamente em classes `@ChangeUnit` versionadas numericamente (`V001_CreateCategoryIndexes`, `V002_CreateProductIndexes`).
  * **Imutabilidade Absoluta:** É expressamente proibido alterar `@ChangeUnit` já comitados. Novas necessidades de índice ou schema exigem um novo ChangeUnit incremental.
  * A propriedade `spring.data.mongodb.auto-index-creation` deve permanecer `false`.
* **Attribute Pattern para Atributos Heterogêneos:**
  * Atributos dinâmicos são persistidos na coleção `products` sob a propriedade `attributes: [{ k, v, unit }]`.
  * Queries facetadas e filtros sobre atributos devem utilizar o índice composto `{ "attributes.k": 1, "attributes.v": 1 }`.
* **Controle de Concorrência Otimista (`@Version`):**
  * O documento `ProductDocument` deve anotar o campo `version` com `@Version`. Modificações concorrentes que encontrem versão divergente lançam `OptimisticLockingFailureException`, mapeada para `409 CONCURRENT_MODIFICATION`.
* **Paginação por Cursor em Listagens:**
  * É proibido o uso de `$skip` para navegação profunda em grandes volumes.
  * As buscas e listagens devem utilizar cursor opaco codificado em Base64URL contendo o campo de ordenação e o `_id` como critério determinístico de desempate.

---

## 6. Diretrizes de Cache com Redis

* **Padrão Cache-Aside:**
  * O cache atua como acelerador de leitura. Consultas de detalhe de produto ou árvore taxonômica verificam primeiro o Redis; em caso de miss, consultam o MongoDB e populam a chave com TTL.
* **Expiração Obrigatória (TTL):**
  * É proibido criar chaves no Redis sem tempo de expiração definido. `cache:product:*` tem TTL de 10 minutos; `cache:category-tree` tem TTL de 30 minutos.
* **Resiliência e Fallback Transparente:**
  * Falhas de rede ou timeout com o Redis não devem interromper o serviço. O componente de cache captura exceções de conexão, loga em nível `WARN`, incrementa métricas operacionais e cai diretamente para o MongoDB.
* **Invalidação Reativa:**
  * Listeners de eventos de domínio (`ProductChanged`, `CategoryChanged`) executam o `DEL` das chaves afetadas logo após o commit da transação.

---

## 7. Diretrizes de Testes Automatizados

* **Pirâmide de Testes:**
  * **Testes Unitários (`*Test.java`):** Rápidos, sem carregar o contexto Spring. Validação do Value Object `Money`, validadores puros de atributos e mappers.
  * **Testes de Integração de Módulo (`*IT.java`):** Utilizam `@ApplicationModuleTest` para subir apenas o contexto do módulo sob teste.
  * **Testes de Arquitetura (`ModularityTests.java`):** Validam a integridade das regras do Spring Modulith e geram documentação de componentes.
* **Uso Obrigatório de Testcontainers:**
  * É expressamente proibido mockar `MongoTemplate` ou utilizar bancos em memória (Fongo/EmbeddedMongo).
  * Todos os testes de integração de repositório, busca e cache rodam contra containers reais de **MongoDB 7** e **Redis 7** gerenciados pelo Testcontainers.
* **Comandos Padronizados de Verificação:**
  * `./mvnw spotless:check` (verificação de estilo e formatação)
  * `./mvnw compile -q` (verificação de compilação e tipagem)
  * `./mvnw verify` (execução completa dos testes unitários, integração e arquitetura)
