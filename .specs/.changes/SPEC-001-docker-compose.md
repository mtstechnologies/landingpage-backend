# SPEC-001 — Docker Compose + Variáveis de Ambiente

## Contexto
O repositório `landingpage-backend` não possui `docker-compose.yml` commitado.
O README instrui o dev a criar o arquivo manualmente, o que quebra o onboarding e
impede automação de CI/CD. Esta spec resolve isso.

## Objetivo
Commitar infraestrutura local completa para que qualquer dev (ou agent) execute
o backend com um único comando: `docker-compose up -d && ./mvnw spring-boot:run`.

---

## Arquivos a criar

### 1. `docker-compose.yml` (raiz do projeto)

```yaml
services:
  postgres:
    image: postgres:16-alpine
    container_name: portfolio_postgres
    environment:
      POSTGRES_DB: ${POSTGRES_DB:-portfolio_db}
      POSTGRES_USER: ${POSTGRES_USER:-postgres}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-postgres}
    ports:
      - "${POSTGRES_PORT:-5432}:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER:-postgres}"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: portfolio_redis
    ports:
      - "${REDIS_PORT:-6379}:6379"
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  postgres_data:
```

---

### 2. `.env.example` (raiz do projeto)

```dotenv
# PostgreSQL
POSTGRES_DB=portfolio_db
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
POSTGRES_PORT=5432

# Redis
REDIS_PORT=6379

# Spring Boot
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/portfolio_db
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
SPRING_DATA_REDIS_HOST=localhost
SPRING_DATA_REDIS_PORT=6379

# Segurança — altere antes de deploy em produção
PORTFOLIO_ADMIN_USER=admin
PORTFOLIO_ADMIN_PASSWORD=troque-em-producao

# Servidor
SERVER_PORT=8080
```

---

### 3. Atualizar `.gitignore` (adicionar ao final)

```
# Variáveis de ambiente locais
.env
.env.local
```

---

### 4. Atualizar `README.md` — seção "Como Executar Localmente"

Substituir o bloco atual "1. Subir a infraestrutura com Docker" por:

```markdown
### 1. Configurar variáveis de ambiente

Copie o arquivo de exemplo e ajuste os valores se necessário:

```bash
cp .env.example .env
```

### 2. Subir a infraestrutura com Docker

```bash
docker-compose up -d
```

Aguarde os healthchecks passarem (postgres e redis ficam `healthy`).

### 3. Executar a aplicação

```bash
./mvnw spring-boot:run
```
```

---

## Comportamento esperado após execução

- `docker-compose up -d` sobe postgres e redis sem erro
- `docker-compose ps` mostra ambos os serviços como `healthy`
- `./mvnw spring-boot:run` conecta no postgres e redis sem configuração extra
- `curl http://localhost:8080/api/v1/portfolio/perfil` retorna `200` ou `404` (sem `perfil` ainda)
- `.env` está no `.gitignore` e não aparece no `git status`

## O que NÃO fazer

- Não alterar nenhuma classe Java
- Não alterar `pom.xml`
- Não criar `docker-compose.prod.yml` — deixar para SPEC-005
- Não hardcodar senhas nos arquivos (usar variáveis com defaults)
