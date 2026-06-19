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
