<div align="center">

# 🗂️ Portfolio Backend

**API RESTful de suporte ao sistema de portfólio de engenharia de software.**  
Construída com arquitetura modular limpa, idempotência por design e testes de integração com infraestrutura efêmera.

[![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3-6DB33F?style=for-the-badge&logo=spring&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-316192?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7-DC382D?style=for-the-badge&logo=redis&logoColor=white)](https://redis.io/)
[![Flyway](https://img.shields.io/badge/Flyway-Migrations-CC0200?style=for-the-badge&logo=flyway&logoColor=white)](https://flywaydb.org/)

</div>

---

## ✨ Visão Geral do Produto

Este projeto implementa a **camada de backend** de um sistema de portfólio pessoal de engenharia de software. A API serve dois propósitos distintos:

- **🌐 Exposição Pública:** Endpoints de leitura sem autenticação, consumidos pelo frontend da landing page para exibir projetos e o perfil profissional.
- **🔐 Painel Administrativo (CMS):** Endpoints de escrita protegidos por autenticação HTTP Basic e autoridade `PORTFOLIO_ESCRITA`, permitindo o gerenciamento total de projetos, tecnologias e perfil.

A arquitetura é projetada com rigor técnico: **Clean Architecture estratégica**, **controle de concorrência otimista** com `@Version`, **idempotência por Redis** para operações de criação e **rastreabilidade end-to-end** via header `X-Trace-Id`.

---

## 🏛️ Arquitetura

O projeto segue uma **arquitetura Monolito Modular** com separação estrita de camadas, inspirada em DDD estratégico.

```
src/main/java/com/portfolio/backend/
│
├── config/                         # Configurações transversais (cross-cutting concerns)
│   ├── IdempotencyInterceptor.java  # Interceptador de idempotência via Redis
│   ├── MdcFilter.java              # Propagação do X-Trace-Id entre as camadas
│   ├── SecurityConfig.java         # Configuração de segurança HTTP
│   └── WebMvcConfig.java           # Registro dos interceptadores MVC
│
└── modulo_portfolio/               # Bounded Context: Gestão de Portfólio
    ├── domain/                     # Entidades JPA, exceções de domínio
    │   ├── Perfil.java
    │   ├── Projeto.java
    │   ├── Tecnologia.java
    │   └── exception/
    ├── application/
    │   └── service/
    │       └── GestaoPortfolioService.java  # Orquestração de regras de negócio
    ├── infrastructure/
    │   └── repository/             # Spring Data JPA Repositories
    └── web/
        ├── PortfolioPublicoController.java  # Rotas públicas de leitura
        ├── PortfolioAdminController.java    # Rotas administrativas protegidas
        └── dto/                            # Request e Response DTOs
```

### Fluxo de uma requisição de escrita (Admin)

```
Cliente → [MdcFilter: injeta X-Trace-Id]
        → [IdempotencyInterceptor: valida chave no Redis]
        → [SecurityConfig: verifica autoridade PORTFOLIO_ESCRITA]
        → [PortfolioAdminController: valida payload com @Valid]
        → [GestaoPortfolioService: orquestra @Transactional]
        → [Repository: persiste no PostgreSQL via JPA]
        → Response com Location header + X-Trace-Id
```

---

## 🚀 Stack Tecnológica

| Categoria              | Tecnologia                          | Versão  |
|------------------------|-------------------------------------|---------|
| **Linguagem**          | Java                                | 21 LTS  |
| **Framework**          | Spring Boot                         | 3.3.x   |
| **Segurança**          | Spring Security (HTTP Basic + RBAC) | —       |
| **Persistência**       | Spring Data JPA + Hibernate         | —       |
| **Banco de Dados**     | PostgreSQL                          | 16      |
| **Cache / Idempotência** | Spring Data Redis                 | —       |
| **Migrações**          | Flyway                              | —       |
| **Geração de código**  | Lombok                              | —       |
| **Logs Estruturados**  | Logback + Logstash JSON Encoder     | 7.4     |
| **Testes de Integração** | Testcontainers (PostgreSQL)       | 1.20.4  |
| **Build**              | Maven                               | —       |

---

## ⚙️ Como Executar Localmente

### Pré-requisitos

- **JDK 21** instalado e configurado no `JAVA_HOME`
- **Docker** (para subir PostgreSQL e Redis)
- **Maven** (ou usar o wrapper `./mvnw` incluso no projeto)

### 1. Subir a infraestrutura com Docker

Crie um arquivo `docker-compose.yml` na raiz do projeto com o conteúdo abaixo:

```yaml
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: portfolio_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
```

Em seguida, inicie os containers:

```bash
docker-compose up -d
```

### 2. Executar a aplicação

```bash
./mvnw spring-boot:run
```

A API estará disponível em: **`http://localhost:8080`**

### 3. Verificar a saúde da aplicação

```bash
curl http://localhost:8080/api/v1/portfolio/perfil
```

---

## 📡 Referência de Endpoints

A API está dividida em dois domínios: **público** (sem autenticação) e **administrativo** (requer credenciais).

### 🌐 Endpoints Públicos

| Método | Endpoint                      | Descrição                                         |
|--------|-------------------------------|---------------------------------------------------|
| `GET`  | `/api/v1/portfolio/perfil`    | Retorna o perfil profissional da landing page     |
| `GET`  | `/api/v1/portfolio/projetos`  | Lista todos os projetos com suas tecnologias      |

**Exemplo de resposta — `GET /api/v1/portfolio/projetos`:**

```json
[
  {
    "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "titulo": "Portfolio Backend API",
    "descricao": "API RESTful com Clean Architecture para gestão de portfólio.",
    "urlCapa": "https://example.com/capa.png",
    "linkProducao": "https://meu-portfolio.com",
    "linkRepositorio": "https://github.com/mtstechnologies/landingpage-backend",
    "dataDesenvolvimento": "2026-06-01",
    "tecnologias": [
      { "id": "...", "nome": "Java", "urlIcone": "https://..." },
      { "id": "...", "nome": "Spring Boot", "urlIcone": "https://..." }
    ],
    "versao": 0
  }
]
```

---

### 🔐 Endpoints Administrativos

> **Autenticação:** HTTP Basic Auth com autoridade `PORTFOLIO_ESCRITA`  
> **Idempotência:** As operações `POST` exigem o header `Idempotency-Key` (UUID único por requisição), armazenado no Redis por 10 minutos. Requisições duplicadas retornam `409 Conflict`.

#### Projetos

| Método   | Endpoint                                              | Descrição                               |
|----------|-------------------------------------------------------|-----------------------------------------|
| `POST`   | `/api/v1/admin/portfolio/projetos`                    | Cria um novo projeto                    |
| `PUT`    | `/api/v1/admin/portfolio/projetos/{id}`               | Atualiza um projeto existente           |
| `DELETE` | `/api/v1/admin/portfolio/projetos/{id}`               | Remove um projeto                       |

#### Tecnologias

| Método | Endpoint                                                          | Descrição                                      |
|--------|-------------------------------------------------------------------|------------------------------------------------|
| `POST` | `/api/v1/admin/portfolio/tecnologias`                             | Cadastra uma nova tecnologia                   |
| `POST` | `/api/v1/admin/portfolio/projetos/{projetoId}/tecnologias/{tecnologiaId}` | Associa uma tecnologia a um projeto |

#### Perfil

| Método | Endpoint                              | Descrição                |
|--------|---------------------------------------|--------------------------|
| `POST` | `/api/v1/admin/portfolio/perfil`      | Cria o perfil profissional |
| `PUT`  | `/api/v1/admin/portfolio/perfil/{id}` | Atualiza o perfil         |

**Exemplo de criação de projeto:**

```bash
curl -X POST http://localhost:8080/api/v1/admin/portfolio/projetos \
  -u "admin:senha" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: $(uuidgen)" \
  -H "X-Trace-Id: trace-001" \
  -d '{
    "titulo": "Portfolio Backend API",
    "descricao": "API RESTful com Clean Architecture.",
    "urlCapa": "https://example.com/capa.png",
    "linkProducao": "https://meu-portfolio.com",
    "linkRepositorio": "https://github.com/mtstechnologies/backend",
    "dataDesenvolvimento": "2026-06-01",
    "tecnologiaIds": []
  }'
```

---

## 🗃️ Banco de Dados

As migrações são gerenciadas pelo **Flyway** e aplicadas automaticamente na inicialização.

### Modelo de dados (`modulo_portfolio`)

```sql
perfil             -- Perfil profissional público da landing page
  ├─ id (UUID PK)
  ├─ nome, titulo, bio
  ├─ link_linkedin, link_github, url_foto
  ├─ usuario_id (UUID lógico — sem FK física para contextos externos)
  └─ versao (controle de concorrência otimista)

projeto            -- Projetos desenvolvidos
  ├─ id (UUID PK)
  ├─ titulo, descricao, url_capa
  ├─ link_producao, link_repositorio, data_desenvolvimento
  └─ versao

tecnologia         -- Catálogo de tecnologias
  ├─ id (UUID PK)
  ├─ nome (UNIQUE), url_icone
  └─ versao

projeto_tecnologia -- Tabela associativa N:N
  ├─ projeto_id (FK → projeto)
  └─ tecnologia_id (FK → tecnologia)
```

> **Isolamento de contexto:** O campo `usuario_id` em `perfil` é uma referência lógica (UUID simples), sem `FOREIGN KEY` física para tabelas de outros módulos, respeitando o princípio de isolamento do Bounded Context.

---

## 🧪 Testes

O projeto possui uma suíte completa de testes unitários e de integração.

### Rodar todos os testes

```bash
./mvnw test
```

### Suíte de Testes Unitários — `GestaoPortfolioServiceTest`

Testa os comportamentos de negócio isoladamente com mocks:

- ✅ Lança `RecursoNaoEncontradoException` ao tentar associar uma tecnologia inexistente
- ✅ Atualiza campos do perfil corretamente sem violar o `@Version` (Optimistic Locking)

### Suíte de Testes de Integração — `PortfolioAdminControllerIT`

Utiliza **Testcontainers** para subir uma instância **real e efêmera** do `postgres:16-alpine` durante a execução:

- ✅ Fluxo completo de criação de projeto via HTTP (MockMvc)
- ✅ Verificação de inserção real no banco efêmero via `ProjetoRepository`
- ✅ Bloqueio de requisição duplicada pelo mecanismo de idempotência Redis (`409 Conflict`)
- ✅ Rejeição de requisição sem `Idempotency-Key` (`400 Bad Request`)

```
[Testcontainers] → PostgreSQL 16-alpine (efêmero)
[MockMvc]        → Spring Boot Application Context completo
[MockBean]       → Redis StringRedisTemplate (mock isolado)
```

---

## 📐 Decisões de Arquitetura

| Decisão | Raciocínio |
|---|---|
| **Idempotência por Redis** | `POST` em APIs REST não são nativamente idempotentes. O interceptador garante *exactly-once semantics* para operações de criação, essencial em ambientes com retry automático. |
| **Optimistic Locking (`@Version`)** | Previne conflitos de escrita concorrente no painel administrativo sem necessitar de locks pessimistas (que degradariam a performance). |
| **Flyway com path isolado** | Migrações em `db/migration/portfolio/` garantem que o módulo possa evoluir de forma independente sem conflitar com versões de outros módulos futuros. |
| **Referência lógica (`usuario_id`)** | Evita acoplamento de infraestrutura entre bounded contexts, mantendo o isolamento do módulo `portfolio` em relação a módulos de autenticação/usuário. |
| **Logs JSON estruturados** | O `logstash-logback-encoder` gera logs em JSON com o `X-Trace-Id` propagado via MDC, facilitando correlação de eventos em ferramentas como Grafana/Kibana. |

---

## 📄 Documentação OpenAPI

O contrato completo da API está disponível em:

- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **JSON:** `http://localhost:8080/v3/api-docs`
- **YAML:** `http://localhost:8080/v3/api-docs.yaml`

O arquivo `src/main/resources/openapi/openapi.yaml` é gerado automaticamente pelo **SpringDoc** na inicialização da aplicação e é consumido pelo frontend para geração automática do SDK via **Orval**.

---

## 📬 Contato

**Michael Trindade da Silva**  
Engenheiro de Software · Pesquisador de Mestrado (UFRGS)  
📧 [michaeltrindadedasilva@gmail.com](mailto:michaeltrindadedasilva@gmail.com)  
🔗 [LinkedIn](https://www.linkedin.com/in/michael-trindade/) · [GitHub](https://github.com/mtstechnologies)