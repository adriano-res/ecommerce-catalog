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

## 2. Pré-requisitos e Execução de Infraestrutura Local

- **Java 21 LTS**
- **Docker e Docker Compose**

Para subir o MongoDB (replica set `rs0`) e o Redis 7 localmente:

```bash
docker compose up -d
```

O MongoDB estará disponível na porta `27017` e o Redis na porta `6379`.

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
