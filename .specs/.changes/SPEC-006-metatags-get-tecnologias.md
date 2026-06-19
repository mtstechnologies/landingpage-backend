# SPEC-006 — Fix urgente: Meta tags reais + GET /tecnologias

## Contexto
Dois problemas bloqueadores encontrados na análise do código real:

1. **Frontend:** `src/routes/__root.tsx` ainda tem os valores padrão da Lovable:
   `title: "Lovable App"`, `author: "Lovable"`, `og:title: "Lovable App"`,
   `twitter:site: "@Lovable"`. Qualquer link compartilhado no LinkedIn ou Google
   mostra "Lovable App" em vez do portfólio de Michael Trindade.

2. **Backend:** O `TechSelector` do admin chama `useGetApiV1AdminPortfolioTecnologias()`,
   mas `PortfolioAdminController` não tem endpoint `GET /tecnologias` — só tem `POST`.
   O formulário de criação/edição de projeto quebra ao abrir o seletor.

Esta spec resolve ambos de forma atômica.

---

## PARTE A — Frontend: `src/routes/__root.tsx`

### Arquivo a modificar
`frontend/src/routes/__root.tsx`

### Mudança exata no bloco `head()`

Localizar o bloco atual:
```ts
head: () => ({
  meta: [
    { charSet: "utf-8" },
    { name: "viewport", content: "width=device-width, initial-scale=1" },
    { title: "Lovable App" },
    { name: "description", content: "Lovable Generated Project" },
    { name: "author", content: "Lovable" },
    { property: "og:title", content: "Lovable App" },
    { property: "og:description", content: "Lovable Generated Project" },
    { property: "og:type", content: "website" },
    { name: "twitter:card", content: "summary" },
    { name: "twitter:site", content: "@Lovable" },
  ],
```

Substituir por:
```ts
head: () => ({
  meta: [
    { charSet: "utf-8" },
    { name: "viewport", content: "width=device-width, initial-scale=1" },
    { title: "Michael Trindade — Engenheiro de Software" },
    {
      name: "description",
      content:
        "Portfólio de Michael Trindade, engenheiro de software full-stack. Especialista em arquitetura limpa, APIs RESTful e produtos escaláveis. Pesquisador de Mestrado na UFRGS.",
    },
    { name: "author", content: "Michael Trindade da Silva" },
    { name: "keywords", content: "engenheiro de software, Java, Spring Boot, React, TypeScript, portfólio, desenvolvedor full-stack, UFRGS" },
    { property: "og:title", content: "Michael Trindade — Engenheiro de Software" },
    {
      property: "og:description",
      content:
        "Portfólio de Michael Trindade, engenheiro de software full-stack. Especialista em arquitetura limpa, APIs RESTful e produtos escaláveis.",
    },
    { property: "og:type", content: "website" },
    { property: "og:url", content: "https://michaeltrindade.dev" },
    { property: "og:image", content: "https://michaeltrindade.dev/og-image.png" },
    { property: "og:locale", content: "pt_BR" },
    { property: "og:site_name", content: "Michael Trindade — Portfólio" },
    { name: "twitter:card", content: "summary_large_image" },
    { name: "twitter:title", content: "Michael Trindade — Engenheiro de Software" },
    {
      name: "twitter:description",
      content: "Portfólio de Michael Trindade, engenheiro de software full-stack.",
    },
    { name: "twitter:image", content: "https://michaeltrindade.dev/og-image.png" },
    { name: "robots", content: "index, follow" },
  ],
```

> **Nota:** As URLs `https://michaeltrindade.dev` são placeholders.
> Substituir pelo domínio real quando disponível.
> O campo `og:image` deve apontar para uma imagem 1200×630px.
> Se não tiver domínio ainda, usar a URL do Vercel temporariamente.

### Também remover a importação do lovable-error-reporting se não for necessária
Verificar se `reportLovableError` ainda é usado em outros lugares.
Se `src/lib/lovable-error-reporting.ts` for exclusivamente da Lovable e não tiver
lógica útil, pode manter por ora — não é urgente remover.

---

## PARTE B — Backend: GET /admin/portfolio/tecnologias

### 1. MODIFICAR — `GestaoPortfolioService.java`

**Arquivo:** `src/main/java/com/portfolio/backend/modulo_portfolio/application/service/GestaoPortfolioService.java`

Adicionar método após `criarTecnologia()`:

```java
@Transactional(readOnly = true)
public List<Tecnologia> listarTecnologias() {
    return tecnologiaRepository.findAll();
}
```

### 2. MODIFICAR — `TecnologiaRepository.java`

**Arquivo:** `src/main/java/com/portfolio/backend/modulo_portfolio/infrastructure/repository/TecnologiaRepository.java`

Verificar se já herda `JpaRepository<Tecnologia, UUID>` — se sim, `findAll()` já existe,
nenhuma mudança necessária no repository.

### 3. MODIFICAR — `PortfolioAdminController.java`

**Arquivo:** `src/main/java/com/portfolio/backend/modulo_portfolio/web/PortfolioAdminController.java`

Adicionar import:
```java
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
```

Adicionar endpoint após `criarTecnologia()`:

```java
@GetMapping("/tecnologias")
public ResponseEntity<List<TecnologiaResponse>> listarTecnologias() {
    List<TecnologiaResponse> tecnologias = gestaoPortfolioService.listarTecnologias()
            .stream()
            .map(TecnologiaResponse::fromEntity)
            .toList();
    return ResponseEntity.ok(tecnologias);
}
```

### 4. MODIFICAR — `PortfolioPublicoController.java` (opcional mas recomendado)

Adicionar também endpoint público para listar tecnologias (útil para filtros futuros):

```java
@GetMapping("/tecnologias")
public ResponseEntity<List<TecnologiaResponse>> listarTecnologias() {
    List<TecnologiaResponse> tecnologias = gestaoPortfolioService.listarTecnologias()
            .stream()
            .map(TecnologiaResponse::fromEntity)
            .toList();
    return ResponseEntity.ok(tecnologias);
}
```

---

## Pós-execução obrigatória

Após implementar a PARTE B (backend), rodar no frontend:

```bash
npm run api:gen
```

Isso vai regenerar o SDK com o novo endpoint `GET /tecnologias` e o
`TechSelector` vai funcionar automaticamente.

---

## Comportamento esperado após execução

### Frontend — meta tags
```bash
# Inspecionar o HTML gerado
curl -s http://localhost:3000 | grep -E "og:|twitter:|description|title"
# Deve mostrar "Michael Trindade" em todos os campos, sem nenhuma referência a "Lovable"
```

### Backend — GET tecnologias
```bash
# Listar tecnologias (admin)
curl -u "admin:senha" http://localhost:8080/api/v1/admin/portfolio/tecnologias
# → 200 OK com array de tecnologias (pode ser vazio se nenhuma cadastrada)

# Criar uma tecnologia primeiro
curl -X POST http://localhost:8080/api/v1/admin/portfolio/tecnologias \
  -u "admin:senha" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: $(uuidgen)" \
  -d '{"nome": "Java", "urlIcone": "https://cdn.example.com/java.png"}'

# Listar novamente
curl -u "admin:senha" http://localhost:8080/api/v1/admin/portfolio/tecnologias
# → [{"id": "...", "nome": "Java", "urlIcone": "..."}]
```

### Frontend — TechSelector
- Abrir `/admin/projects/new`
- O campo "Tecnologias" deve exibir as tecnologias cadastradas para seleção
- Sem erro de rede no console do browser

## O que NÃO fazer
- Não alterar nenhum outro arquivo além dos listados
- Não remover a propriedade `og:image` mesmo sem ter a imagem ainda — deixar o placeholder
- Não modificar os hooks gerados pelo Orval manualmente — apenas rodar `api:gen`
- Não adicionar paginação no GET /tecnologias — lista simples por ora
- Não alterar `SecurityConfig.java` — isso é escopo da SPEC-007
