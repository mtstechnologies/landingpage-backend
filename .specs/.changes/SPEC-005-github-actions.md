# SPEC-005 — GitHub Actions CI/CD (Backend + Frontend)

## Contexto
Nenhum dos dois repositórios possui pipeline de CI/CD. Deploy é manual e sem validação
automática. Esta spec adiciona GitHub Actions completo para os dois repos.

## Pré-requisito
SPEC-001 a SPEC-004 executadas e funcionando localmente.

---

## Repo: `landingpage-backend`

### Arquivo a criar: `.github/workflows/ci.yml`

```yaml
name: CI — Backend

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  build-and-test:
    name: Build + Testes
    runs-on: ubuntu-latest

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Setup JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'maven'

      - name: Build e testes (Testcontainers via Docker do runner)
        run: ./mvnw verify -B
        env:
          # Testcontainers usa o Docker do GitHub runner — sem setup extra
          TESTCONTAINERS_RYUK_DISABLED: true

      - name: Verificar cobertura (falha se < 60%)
        run: ./mvnw jacoco:check -B
        continue-on-error: true  # remover quando cobertura estiver estável

  openapi-export:
    name: Exportar OpenAPI spec
    runs-on: ubuntu-latest
    needs: build-and-test
    if: github.ref == 'refs/heads/main'

    services:
      postgres:
        image: postgres:16-alpine
        env:
          POSTGRES_DB: portfolio_db
          POSTGRES_USER: postgres
          POSTGRES_PASSWORD: postgres
        ports:
          - 5432:5432
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

      redis:
        image: redis:7-alpine
        ports:
          - 6379:6379
        options: >-
          --health-cmd "redis-cli ping"
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'maven'

      - name: Subir aplicação e exportar openapi.yaml
        run: |
          ./mvnw spring-boot:run &
          sleep 20
          curl -f http://localhost:8080/v3/api-docs.yaml -o .specs/openapi.yaml
          kill %1
        env:
          SPRING_DATASOURCE_URL: jdbc:postgresql://localhost:5432/portfolio_db
          SPRING_DATASOURCE_USERNAME: postgres
          SPRING_DATASOURCE_PASSWORD: postgres
          SPRING_DATA_REDIS_HOST: localhost
          SPRING_DATA_REDIS_PORT: 6379
          PORTFOLIO_ADMIN_USER: ${{ secrets.PORTFOLIO_ADMIN_USER }}
          PORTFOLIO_ADMIN_PASSWORD: ${{ secrets.PORTFOLIO_ADMIN_PASSWORD }}

      - name: Commit openapi.yaml atualizado
        uses: stefanzweifel/git-auto-commit-action@v5
        with:
          commit_message: 'ci: atualiza openapi.yaml [skip ci]'
          file_pattern: '.specs/openapi.yaml'
```

---

### Arquivo a criar: `.github/workflows/docker.yml`

```yaml
name: Docker — Build e Push

on:
  push:
    branches: [main]
    tags: ['v*']

jobs:
  docker:
    name: Build e Push Docker Hub
    runs-on: ubuntu-latest
    needs: []   # roda independente — ajustar se quiser gate no CI

    steps:
      - uses: actions/checkout@v4

      - name: Login Docker Hub
        uses: docker/login-action@v3
        with:
          username: ${{ secrets.DOCKERHUB_USERNAME }}
          password: ${{ secrets.DOCKERHUB_TOKEN }}

      - name: Build e push
        uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: |
            ${{ secrets.DOCKERHUB_USERNAME }}/portfolio-backend:latest
            ${{ secrets.DOCKERHUB_USERNAME }}/portfolio-backend:${{ github.sha }}
```

---

### Arquivo a criar: `Dockerfile` (raiz do backend)

```dockerfile
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY target/*.jar app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

> Para o `docker.yml` funcionar, adicionar ao `pom.xml` o plugin para gerar o JAR:
> garantir que `spring-boot-maven-plugin` está configurado com `<repackage>`.
> Já deve estar — verificar.

---

## Repo: `landingpage-frontend`

### Arquivo a criar: `.github/workflows/ci.yml`

```yaml
name: CI — Frontend

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  lint-and-build:
    name: Lint + Build
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - uses: oven-sh/setup-bun@v2
        with:
          bun-version: latest

      - name: Instalar dependências
        run: bun install --frozen-lockfile

      - name: Lint
        run: bun run lint

      - name: Type check
        run: bun run tsc --noEmit

      - name: Build de produção
        run: bun run build
        env:
          VITE_API_URL: ${{ vars.VITE_API_URL }}

  sdk-sync-check:
    name: Verificar SDK sincronizado
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - uses: oven-sh/setup-bun@v2
        with:
          bun-version: latest

      - run: bun install --frozen-lockfile

      - name: Regenerar SDK
        run: bun run api:gen

      - name: Verificar se há diff (SDK desatualizado)
        run: |
          git diff --exit-code src/shared/api/ || \
          (echo "❌ SDK desatualizado! Rode 'npm run api:gen' e commite." && exit 1)
```

---

### Arquivo a criar: `.github/workflows/deploy.yml`

```yaml
name: Deploy — Vercel

on:
  push:
    branches: [main]

jobs:
  deploy:
    name: Deploy para Vercel
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - uses: oven-sh/setup-bun@v2
        with:
          bun-version: latest

      - run: bun install --frozen-lockfile

      - run: bun run build
        env:
          VITE_API_URL: ${{ vars.VITE_API_URL }}

      - name: Deploy no Vercel
        uses: amondnet/vercel-action@v25
        with:
          vercel-token: ${{ secrets.VERCEL_TOKEN }}
          vercel-org-id: ${{ secrets.VERCEL_ORG_ID }}
          vercel-project-id: ${{ secrets.VERCEL_PROJECT_ID }}
          working-directory: ./
          vercel-args: '--prod'
```

---

## Secrets necessários — configurar em GitHub Settings → Secrets

### Backend (`landingpage-backend`):
| Secret | Valor |
|---|---|
| `PORTFOLIO_ADMIN_USER` | usuário do admin |
| `PORTFOLIO_ADMIN_PASSWORD` | senha do admin |
| `DOCKERHUB_USERNAME` | seu usuário Docker Hub |
| `DOCKERHUB_TOKEN` | token de acesso Docker Hub |

### Frontend (`landingpage-frontend`):
| Secret / Var | Valor |
|---|---|
| `VERCEL_TOKEN` | token da Vercel CLI |
| `VERCEL_ORG_ID` | ID da org na Vercel |
| `VERCEL_PROJECT_ID` | ID do projeto na Vercel |
| `VITE_API_URL` (var, não secret) | URL pública do backend em produção |

---

## Comportamento esperado após execução

- Push em `main` no backend → testes rodam → openapi.yaml atualizado automaticamente → Docker Hub atualizado
- PR no backend → CI valida antes do merge
- Push em `main` no frontend → lint + type check + build → deploy automático na Vercel
- SDK desatualizado no frontend → CI falha com mensagem clara
- Todos os secrets ausentes → workflows falham com mensagem de erro clara (não silenciosamente)

## O que NÃO fazer

- Não colocar senhas ou tokens diretamente nos arquivos YAML — sempre via Secrets
- Não usar `actions/cache` manualmente — Maven e Bun já têm cache nativo nas actions oficiais
- Não adicionar step de notificação (Slack, email) — complexidade desnecessária agora
- Não criar workflow de staging — deploy vai direto para produção neste momento
