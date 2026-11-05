# E-commerce Catalog & Faceted Search Engine

Backend de alta performance para catálogo heterogêneo de e-commerce e motor de busca com filtros dinâmicos, implementado como um **Monolito Modular em Java 21** com **Spring Boot 3.5+**, **Spring Modulith**, **MongoDB 7** e **Redis 7**.

---

## 1. Visão Geral da Arquitetura

O sistema é estruturado em módulos de domínio independentes:
- **`category`**: Árvore hierárquica de categorias (*Ancestors Path*), herança de definições de atributos e cache de árvore taxonômica.
- **`product`**: Cadastro de produtos com atributos heterogêneos (*Attribute Pattern*), validação estrita contra a categoria, controle de versão otimista (`@Version`), motor de busca com filtros dinâmicos via Aggregation Pipeline (`$match` + `$facet`), paginação por cursor e cache-aside no Redis.
- **`shared`**: Componentes transversais agnósticos (`Money` Value Object imutável, RFC 7807 `ProblemDetail`, `AuditMetadata`, `CursorPage` e decodificador Base64URL).

A separação e o isolamento entre módulos são auditados e validados por testes arquiteturais do Spring Modulith (`ApplicationModules.verify()`).

---

## 2. Pré-requisitos e Execução de Infraestrutura

- **Java 21 LTS**
- **Docker e Docker Compose**

### 2.1. Ambiente de Desenvolvimento Local (dev-infra)
Para subir **apenas os serviços de suporte** necessários para testar e rodar a aplicação localmente pelo host (MongoDB com replica set `rs0`, Redis 7 e SeaweedFS S3):

```bash
docker compose -f docker-compose.dev-infra.yml up -d
```

- **MongoDB:** `localhost:27017`
- **Redis:** `localhost:6379`
- **SeaweedFS S3:** `http://localhost:8333` (Master/UI: `9333`)

Para parar a infraestrutura de desenvolvimento:
```bash
docker compose -f docker-compose.dev-infra.yml down
```

### 2.2. Ambiente de Produção Completo (app + infra + observabilidade)
Para construir a imagem containerizada da aplicação via `Dockerfile` (multi-stage com extração de camadas) e subir todo o ecossistema integrado:

```bash
docker compose up -d --build
```

- **Ecommerce Catalog App:** `http://localhost:8080` (Swagger UI: `http://localhost:8080/swagger-ui/index.html`)
- **Grafana:** `http://localhost:3000` (admin/admin)
- **Prometheus:** `http://localhost:9090`
- **Loki:** `http://localhost:3100`

---

## 3. Comandos de Build e Execução

O projeto gerencia as ferramentas via `.tool-versions` (asdf). Utilize o `mvn`:

```bash
# Compilar projeto
mvn compile -q

# Executar aplicação localmente
mvn spring-boot:run

# Verificar formatação (Google Java Format / Spotless)
mvn spotless:check

# Aplicar formatação automática
mvn spotless:apply

# Executar testes unitários e de integração
mvn verify
```

---

## 4. Documentação de Referência

- [SPECIFICATION.md](docs/SPECIFICATION.md): Requisitos funcionais/não-funcionais, regras de negócio, diagramas e contratos de API.
- [ARCHITECTURE.md](docs/ARCHITECTURE.md): Estrutura de pastas, mapeamento de coleções, SRP e diretrizes de camadas.
- [CODING_GUIDELINES.md](docs/CODING_GUIDELINES.md): Convenções de código Java moderno, imutabilidade e boas práticas de banco/cache.
