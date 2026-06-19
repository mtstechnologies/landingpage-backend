# SPEC-009 — Spring Actuator + Health Check

## Contexto
`spring-boot-starter-actuator` não está no `pom.xml`. Sem ele:
- Deploy no Railway/Render não sabe se a app está pronta para receber tráfego
- Não há endpoint `/health` para monitoramento externo
- O step de CI que exporta o openapi.yaml faz `sleep 20` sem garantia real de readiness

## Pré-requisito
SPEC-008 executada e testes passando.

---

## Arquivos a modificar

### 1. `pom.xml` — Adicionar dependência do Actuator

Localizar o bloco de dependências e adicionar após `spring-boot-starter-data-redis`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

---

### 2. `src/main/resources/application.yml` — Expandir configuração do Actuator

Localizar o bloco `management:` que foi adicionado na SPEC-007 e substituir por:

```yaml
management:
  endpoints:
    web:
      base-path: /actuator
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: when-authorized
      show-components: always
      probes:
        enabled: true
    info:
      enabled: true
  health:
    redis:
      enabled: true
    db:
      enabled: true
  info:
    app:
      enabled: true
    java:
      enabled: true

info:
  app:
    name: Portfolio Backend API
    description: API RESTful de suporte ao portfólio de engenharia de software
    version: 1.0.0
    author: Michael Trindade da Silva
    contact: michaeltrindadedasilva@gmail.com
```

> Se o bloco `management:` ainda não existir (caso SPEC-007 não tenha sido
> totalmente aplicada), adicionar todo o bloco acima ao final do arquivo.

---

### 3. `src/main/java/com/portfolio/backend/config/SecurityConfig.java`

Verificar se o seguinte já está no `authorizeHttpRequests` (foi adicionado na SPEC-007):

```java
.requestMatchers("/actuator/health", "/actuator/info").permitAll()
```

Se não estiver, adicionar antes da linha `.requestMatchers("/api/v1/admin/**")`.

---

### 4. CI backend — melhorar o step de export do openapi

**Arquivo:** `.github/workflows/ci.yml`

Localizar o step:
```yaml
      - name: Subir aplicação e exportar openapi.yaml
        run: |
          ./mvnw spring-boot:run &
          sleep 20
          curl -f http://localhost:8080/v3/api-docs.yaml -o .specs/openapi.yaml
          kill %1
```

Substituir por:
```yaml
      - name: Subir aplicação e exportar openapi.yaml
        run: |
          ./mvnw spring-boot:run &
          APP_PID=$!

          # Aguardar readiness via health check (até 60 segundos)
          echo "Aguardando aplicação ficar healthy..."
          for i in $(seq 1 30); do
            STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health || echo "000")
            if [ "$STATUS" = "200" ]; then
              echo "✅ Aplicação pronta após ${i}x (${i}0s)"
              break
            fi
            echo "⏳ Tentativa $i — status: $STATUS"
            sleep 2
          done

          # Exportar spec
          curl -f http://localhost:8080/v3/api-docs.yaml -o .specs/openapi.yaml
          kill $APP_PID
```

---

## Comportamento esperado após execução

```bash
# 1. Health check básico (público)
curl http://localhost:8080/actuator/health
# → {"status":"UP","components":{"db":{"status":"UP"},"redis":{"status":"UP"},"ping":{"status":"UP"}}}

# 2. Health sem banco rodando (deve retornar DOWN)
# Parar o postgres e verificar
# → {"status":"DOWN","components":{"db":{"status":"DOWN",...}}}

# 3. Info endpoint
curl http://localhost:8080/actuator/info
# → {"app":{"name":"Portfolio Backend API","version":"1.0.0",...}}

# 4. Endpoints não expostos permanecem ocultos
curl http://localhost:8080/actuator/env
# → 404 Not Found

# 5. Testes continuam passando
./mvnw test
```

## O que NÃO fazer
- Não expor `/actuator/env` ou `/actuator/beans` — dados sensíveis
- Não expor `/actuator/shutdown` — permite derrubar a app remotamente
- Não adicionar Prometheus nesta spec — escopo da observabilidade futura
- Não alterar nenhum arquivo de domínio ou controller
- Não alterar testes existentes
