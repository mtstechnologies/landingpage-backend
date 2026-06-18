# SPEC-002 — Campo `slug` na entidade Projeto + endpoint público por slug

## Contexto
O frontend já possui a rota `projects.$slug.tsx` que espera um endpoint
`GET /api/v1/portfolio/projetos/{slug}`. O backend não tem o campo `slug`
na entidade `Projeto` nem o endpoint correspondente. Esta spec adiciona ambos.

## Objetivo
Permitir que cada projeto tenha uma URL pública amigável (`/projects/portfolio-backend-api`)
e que o frontend consiga buscar os dados de um projeto específico por esse identificador.

---

## Arquivos a criar / modificar

### 1. CRIAR — migração Flyway

**Arquivo:** `src/main/resources/db/migration/portfolio/V2__add_slug_to_projeto.sql`

```sql
ALTER TABLE projeto ADD COLUMN slug VARCHAR(255);

UPDATE projeto
SET slug = LOWER(
  REGEXP_REPLACE(
    REGEXP_REPLACE(titulo, '[^a-zA-Z0-9\s-]', '', 'g'),
    '\s+', '-', 'g'
  )
)
WHERE slug IS NULL;

ALTER TABLE projeto ALTER COLUMN slug SET NOT NULL;
ALTER TABLE projeto ADD CONSTRAINT projeto_slug_unique UNIQUE (slug);
CREATE INDEX idx_projeto_slug ON projeto(slug);
```

> O UPDATE popula o slug dos projetos já existentes a partir do título,
> garantindo que a migração não quebre dados em produção.

---

### 2. MODIFICAR — `Projeto.java`

**Arquivo:** `src/main/java/com/portfolio/backend/modulo_portfolio/domain/Projeto.java`

Adicionar o campo `slug` à entidade JPA:

```java
@Column(nullable = false, unique = true)
private String slug;
```

Adicionar no construtor / builder (se existir) e no getter/setter (ou usar Lombok `@Getter @Setter`).

---

### 3. MODIFICAR — DTO de Request (criação/edição de projeto)

**Arquivo:** `src/main/java/com/portfolio/backend/modulo_portfolio/web/dto/ProjetoRequest.java`
(ou nome equivalente — verificar o arquivo existente)

Adicionar campo com validação:

```java
@NotBlank(message = "Slug é obrigatório")
@Pattern(
  regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
  message = "Slug deve conter apenas letras minúsculas, números e hífens"
)
@Size(max = 255)
private String slug;
```

---

### 4. MODIFICAR — DTO de Response de Projeto

**Arquivo:** `src/main/java/com/portfolio/backend/modulo_portfolio/web/dto/ProjetoResponse.java`
(ou nome equivalente)

Adicionar o campo `slug` no response:

```java
private String slug;
```

Garantir que o mapper preencha esse campo ao converter `Projeto` → `ProjetoResponse`.

---

### 5. MODIFICAR — `GestaoPortfolioService.java`

**Arquivo:** `src/main/java/com/portfolio/backend/modulo_portfolio/application/service/GestaoPortfolioService.java`

Adicionar método:

```java
public ProjetoResponse buscarProjetoPorSlug(String slug) {
    Projeto projeto = projetoRepository.findBySlug(slug)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Projeto não encontrado: " + slug));
    return toResponse(projeto);
}
```

No método de criação de projeto, popular o campo `slug` a partir do request:

```java
projeto.setSlug(request.getSlug());
```

---

### 6. MODIFICAR — `ProjetoRepository.java`

**Arquivo:** `src/main/java/com/portfolio/backend/modulo_portfolio/infrastructure/repository/ProjetoRepository.java`

Adicionar método de busca:

```java
Optional<Projeto> findBySlug(String slug);
```

---

### 7. MODIFICAR — `PortfolioPublicoController.java`

**Arquivo:** `src/main/java/com/portfolio/backend/modulo_portfolio/web/PortfolioPublicoController.java`

Adicionar endpoint:

```java
@GetMapping("/projetos/{slug}")
public ResponseEntity<ProjetoResponse> buscarProjetoPorSlug(
        @PathVariable String slug,
        HttpServletResponse response) {
    ProjetoResponse projeto = gestaoPortfolioService.buscarProjetoPorSlug(slug);
    response.setHeader("Cache-Control", "public, max-age=300");
    return ResponseEntity.ok(projeto);
}
```

---

### 8. MODIFICAR — `PortfolioAdminController.java`

Garantir que o endpoint `POST /admin/portfolio/projetos` aceite e persista o campo `slug`.
Nenhuma mudança na assinatura do método — apenas garantir que o `ProjetoRequest` mapeado
inclui o campo `slug` e que o service o persiste.

---

## Comportamento esperado após execução

```bash
# Criar projeto com slug
curl -X POST http://localhost:8080/api/v1/admin/portfolio/projetos \
  -u "admin:senha" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: $(uuidgen)" \
  -d '{
    "titulo": "Portfolio Backend API",
    "slug": "portfolio-backend-api",
    "descricao": "API RESTful com Clean Architecture.",
    "urlCapa": "https://example.com/capa.png",
    "linkProducao": "https://meu-portfolio.com",
    "linkRepositorio": "https://github.com/mtstechnologies/backend",
    "dataDesenvolvimento": "2026-06-01",
    "tecnologiaIds": []
  }'
# → 201 Created com Location: /api/v1/portfolio/projetos/portfolio-backend-api

# Buscar por slug
curl http://localhost:8080/api/v1/portfolio/projetos/portfolio-backend-api
# → 200 OK com o projeto

# Slug inexistente
curl http://localhost:8080/api/v1/portfolio/projetos/nao-existe
# → 404 Not Found

# Slug com formato inválido na criação
# (ex: "Meu Projeto!" com maiúsculas e espaços)
# → 400 Bad Request com mensagem de validação
```

## O que NÃO fazer

- Não alterar a estrutura de pastas existente
- Não remover o campo `titulo` — slug é um campo adicional, não substituto
- Não criar endpoint de geração automática de slug — o admin informa manualmente
- Não alterar testes existentes — apenas adicionar novos se necessário
- Não mexer em `SecurityConfig.java`
