# SPEC-013 — Production-Ready: configuração de produção + DEPLOY.md

## Contexto
O projeto está funcionalmente completo. Esta spec finaliza os últimos ajustes
para que o portfólio possa ir ao ar com segurança e seja facilmente apresentável
para recrutadores técnicos.

Problemas identificados no código atual:
- `show-sql: true` e `format_sql: true` hardcoded — escrevem queries no log em produção
- Sem profile `application-prod.yml` — mesmas configurações de dev em produção
- Sem `DEPLOY.md` — recrutadores técnicos não sabem como rodar o projeto
- Sem `favicon` personalizado — aba do browser mostra ícone genérico
- Sem `robots.txt` — Google não recebe diretrizes de indexação

## Pré-requisito
SPEC-012 executada e funcionando.

---

## PARTE A — Backend

### 1. MODIFICAR — `src/main/resources/application.yml`

#### Substituição exata — show-sql e format_sql

Localizar:
```yaml
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true
    open-in-view: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
```

Substituir por:
```yaml
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: ${JPA_SHOW_SQL:false}
    open-in-view: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: ${JPA_FORMAT_SQL:false}
```

#### Substituição — logging (tornar configurável)

Localizar:
```yaml
logging:
  level:
    root: INFO
    com.portfolio.backend: DEBUG
    org.springframework.web: INFO
    org.hibernate.SQL: DEBUG
```

Substituir por:
```yaml
logging:
  level:
    root: ${LOG_LEVEL_ROOT:INFO}
    com.portfolio.backend: ${LOG_LEVEL_APP:INFO}
    org.springframework.web: ${LOG_LEVEL_WEB:WARN}
    org.hibernate.SQL: ${LOG_LEVEL_SQL:WARN}
```

---

### 2. CRIAR — `src/main/resources/application-prod.yml`

```yaml
# Configurações sobrescritas em produção (profile: prod)
# Ativado com: SPRING_PROFILES_ACTIVE=prod

spring:
  jpa:
    show-sql: false
    properties:
      hibernate:
        format_sql: false

# SpringDoc — desabilitar Swagger UI em produção
springdoc:
  swagger-ui:
    enabled: false
  api-docs:
    enabled: false

logging:
  level:
    root: WARN
    com.portfolio.backend: INFO
    org.springframework.web: WARN
    org.hibernate.SQL: WARN
```

> Para ativar em produção, adicionar a variável de ambiente:
> `SPRING_PROFILES_ACTIVE=prod`
> No Railway: Settings → Variables → adicionar `SPRING_PROFILES_ACTIVE=prod`

---

### 3. MODIFICAR — `.env.example`

Adicionar ao final:

```dotenv
# JPA — habilitar apenas em desenvolvimento local
JPA_SHOW_SQL=true
JPA_FORMAT_SQL=true

# Logging — ajustar por ambiente
LOG_LEVEL_ROOT=INFO
LOG_LEVEL_APP=DEBUG
LOG_LEVEL_SQL=DEBUG

# Spring profile — usar 'prod' em produção
SPRING_PROFILES_ACTIVE=default
```

---

### 4. CRIAR — `DEPLOY.md` (raiz do backend)

```markdown
# Deploy — Portfolio Backend API

## Stack de produção recomendada

| Serviço | Provedor | Tier | Custo |
|---------|----------|------|-------|
| Backend | Railway | Starter | ~$5/mês |
| PostgreSQL | Railway (managed) | Starter | incluso |
| Redis | Upstash | Free | gratuito |
| DNS/SSL | Cloudflare | Free | gratuito |

---

## Deploy no Railway

### 1. Criar projeto no Railway

```bash
# Instalar Railway CLI
npm install -g @railway/cli

# Login
railway login

# Criar projeto
railway init
```

### 2. Adicionar PostgreSQL e Redis

No dashboard Railway:
- New → Database → PostgreSQL 16
- New → Database → Redis

O Railway injeta automaticamente as variáveis de conexão.

### 3. Configurar variáveis de ambiente

No Railway: Settings → Variables → adicionar:

```
SPRING_DATASOURCE_URL=${{Postgres.DATABASE_URL}}
SPRING_DATASOURCE_USERNAME=${{Postgres.PGUSER}}
SPRING_DATASOURCE_PASSWORD=${{Postgres.PGPASSWORD}}
SPRING_DATA_REDIS_HOST=${{Redis.REDIS_HOST}}
SPRING_DATA_REDIS_PORT=${{Redis.REDIS_PORT}}
PORTFOLIO_ADMIN_USER=seu_usuario_admin
PORTFOLIO_ADMIN_PASSWORD=senha_forte_aqui
PORTFOLIO_CORS_ALLOWED_ORIGINS=https://seudominio.com
SPRING_PROFILES_ACTIVE=prod
```

### 4. Deploy

```bash
railway up
```

O Dockerfile na raiz já está configurado. O Railway detecta automaticamente.

### 5. Verificar

```bash
# Health check
curl https://seu-app.railway.app/actuator/health
# → {"status":"UP","components":{"db":{"status":"UP"},"redis":{"status":"UP"}}}
```

---

## Deploy manual com Docker

```bash
# Build
./mvnw package -DskipTests
docker build -t portfolio-backend .

# Run
docker run -d \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/portfolio_db \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=senha \
  -e SPRING_DATA_REDIS_HOST=host \
  -e PORTFOLIO_ADMIN_USER=admin \
  -e PORTFOLIO_ADMIN_PASSWORD=senha_forte \
  -e PORTFOLIO_CORS_ALLOWED_ORIGINS=https://seudominio.com \
  -e SPRING_PROFILES_ACTIVE=prod \
  portfolio-backend
```

---

## Cloudflare (DNS + SSL)

1. Adicionar domínio no Cloudflare
2. Apontar CNAME `api` para o domínio do Railway
3. SSL: Full (strict)
4. Proxy: Ativado (laranja) — HTTPS automático

---

## Checklist de go-live

- [ ] `PORTFOLIO_ADMIN_PASSWORD` com senha forte (mín. 16 chars, especiais)
- [ ] `PORTFOLIO_CORS_ALLOWED_ORIGINS` com domínio de produção do frontend
- [ ] `SPRING_PROFILES_ACTIVE=prod` ativado
- [ ] Health check passando: `GET /actuator/health → {"status":"UP"}`
- [ ] Flyway migrations aplicadas: verificar nos logs de startup
- [ ] Primeiro perfil criado via admin: `POST /api/v1/admin/portfolio/perfil`
```

---

## PARTE B — Frontend

### 1. CRIAR — `public/robots.txt`

```
User-agent: *
Allow: /
Disallow: /admin
Disallow: /login

Sitemap: https://michaeltrindade.dev/sitemap.xml
```

> Substituir `michaeltrindade.dev` pelo domínio real quando disponível.

---

### 2. CRIAR — `public/sitemap.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
  <url>
    <loc>https://michaeltrindade.dev/</loc>
    <changefreq>weekly</changefreq>
    <priority>1.0</priority>
  </url>
</urlset>
```

> URL de projetos individuais serão adicionadas dinamicamente após o cadastro
> dos projetos reais. Por ora o sitemap cobre apenas a landing page.

---

### 3. CRIAR — `public/favicon.svg`

```svg
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 32 32">
  <rect width="32" height="32" rx="6" fill="#0f172a"/>
  <text
    x="16"
    y="22"
    font-family="system-ui, -apple-system, sans-serif"
    font-size="16"
    font-weight="700"
    fill="white"
    text-anchor="middle"
  >MT</text>
</svg>
```

---

### 4. MODIFICAR — `src/routes/__root.tsx`

Adicionar o favicon e o Sonner Toaster que está faltando. Localizar o bloco `links:` no `head()` e adicionar:

```ts
links: [
  { rel: 'stylesheet', href: appCss },
  { rel: 'icon', type: 'image/svg+xml', href: '/favicon.svg' },
],
```

Adicionar o `<Toaster />` do Sonner dentro do `RootComponent` (após `<Outlet />`):

```tsx
import { Toaster } from '../components/ui/sonner'

function RootComponent() {
  const { queryClient } = Route.useRouteContext()
  return (
    <QueryClientProvider client={queryClient}>
      <Outlet />
      <Toaster richColors position="top-right" />
    </QueryClientProvider>
  )
}
```

> Se o `<Toaster />` já estiver presente em algum arquivo, não duplicar.

---

### 5. CRIAR — `DEPLOY.md` (raiz do frontend)

```markdown
# Deploy — Portfolio Frontend

## Stack de produção recomendada

| Serviço | Provedor | Tier | Custo |
|---------|----------|------|-------|
| Frontend | Vercel | Hobby | gratuito |
| DNS/SSL | Cloudflare | Free | gratuito |

---

## Deploy na Vercel

### 1. Via GitHub (recomendado)

1. Acessar [vercel.com](https://vercel.com) → New Project
2. Importar o repositório `landingpage-frontend`
3. Framework Preset: **Other** (TanStack Start é detectado automaticamente)
4. Build Command: `bun run build`
5. Output Directory: `dist`

### 2. Variáveis de ambiente na Vercel

Settings → Environment Variables → adicionar:

```
VITE_API_URL=https://sua-api.railway.app
```

### 3. Domínio personalizado

Settings → Domains → adicionar `michaeltrindade.dev`

---

## Cloudflare (DNS + SSL)

1. Adicionar domínio no Cloudflare
2. Apontar CNAME `@` para o domínio da Vercel (`cname.vercel-dns.com`)
3. SSL: Full
4. Proxy: Ativado

---

## Fluxo de atualização de conteúdo

```
Admin cadastra projeto em /admin
        ↓
POST /api/v1/admin/portfolio/projetos (backend Railway)
        ↓
GET /api/v1/portfolio/projetos (frontend consome em tempo real)
        ↓
Projeto aparece na landing page sem novo deploy
```

## Checklist de go-live

- [ ] `VITE_API_URL` apontando para o backend de produção
- [ ] Build de produção sem erros: `bun run build`
- [ ] Testes passando: `bun run test`
- [ ] TypeCheck limpo: `bun run typecheck`
- [ ] Favicon aparecendo na aba do browser
- [ ] Meta tags corretas: inspecionar `view-source:https://seudominio.com`
- [ ] LinkedIn preview correto: testar em [cards.linkedin.com/post-inspector](https://cards.linkedin.com/post-inspector/)
- [ ] Perfil cadastrado no admin: nome, bio, foto, LinkedIn, GitHub
- [ ] Pelo menos 1 projeto real cadastrado com slug, descrição e tecnologias
```

---

## Comportamento esperado após execução

### Backend
```bash
# show-sql desativado por padrão
./mvnw spring-boot:run
# Logs NÃO mostram queries SQL

# Com JPA_SHOW_SQL=true em dev
JPA_SHOW_SQL=true ./mvnw spring-boot:run
# Logs mostram queries (comportamento anterior)

# Profile prod
SPRING_PROFILES_ACTIVE=prod ./mvnw spring-boot:run
# Swagger UI desabilitado: GET /swagger-ui.html → 404
# Logs apenas WARN e acima
```

### Frontend
```bash
# robots.txt acessível
curl http://localhost:3000/robots.txt
# → User-agent: * ...

# Favicon na aba do browser
# → Ícone "MT" em fundo escuro

# Toaster funcional
# → Criar projeto no admin exibe toast verde
```

## O que NÃO fazer
- Não alterar nenhum arquivo Java além de `application.yml` e o novo `application-prod.yml`
- Não desabilitar o Swagger em desenvolvimento — apenas em prod via profile
- Não commitar senhas reais no `DEPLOY.md` — usar placeholders
- Não alterar nenhum componente de feature — apenas arquivos de configuração e `__root.tsx`
- Não criar imagens PNG para o favicon — SVG é suficiente e leve
- Não alterar testes existentes
