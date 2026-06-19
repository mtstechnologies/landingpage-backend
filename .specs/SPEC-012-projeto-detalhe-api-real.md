# SPEC-012 — Página de detalhe do projeto: API real + layout admin

## Contexto
Análise do código identificou três problemas bloqueadores:

1. **`projects.$slug.tsx`** usa `mockProjects.find()` em vez da API real.
   O visitante vê projetos fictícios de "Alex Carvalho" ao clicar em qualquer projeto.

2. **`slug` ausente no `ProjetoResponse` do SDK** — o `openapi.yaml` em `.specs/`
   não tem o campo `slug`, então o Orval não o gerou no modelo. Isso significa que
   o backend exporta slug mas o frontend não o recebe tipado.

3. **Sem arquivo `src/routes/admin.tsx`** (layout pai do TanStack Router) —
   as rotas `/admin/**` não têm um layout pai, então o `AdminLayout` com a sidebar
   nunca é renderizado. Cada página admin aparece sem navegação lateral.

## Pré-requisito
SPEC-011 executada. Backend rodando localmente.

---

## PARTE A — Backend: adicionar `slug` ao `ProjetoResponse` e ao `openapi.yaml`

### 1. MODIFICAR — `ProjetoResponse.java`

**Arquivo:** `src/main/java/com/portfolio/backend/modulo_portfolio/web/dto/ProjetoResponse.java`

Verificar se o campo `slug` já está no record/classe. Se não estiver, adicionar:

```java
// Se for um record:
public record ProjetoResponse(
    UUID id,
    String titulo,
    String slug,        // ← adicionar esta linha
    String descricao,
    String urlCapa,
    String linkProducao,
    String linkRepositorio,
    LocalDate dataDesenvolvimento,
    List<TecnologiaResponse> tecnologias,
    Integer versao
) {
    public static ProjetoResponse fromEntity(Projeto projeto) {
        return new ProjetoResponse(
            projeto.getId(),
            projeto.getTitulo(),
            projeto.getSlug(),   // ← adicionar esta linha
            projeto.getDescricao(),
            projeto.getUrlCapa(),
            projeto.getLinkProducao(),
            projeto.getLinkRepositorio(),
            projeto.getDataDesenvolvimento(),
            projeto.getTecnologias().stream()
                .map(TecnologiaResponse::fromEntity)
                .toList(),
            projeto.getVersao()
        );
    }
}
```

> Se for uma classe com getters/setters (Lombok), adicionar campo e getter:
> ```java
> private String slug;
> ```

### 2. Regenerar `openapi.yaml`
Após adicionar o `slug` ao DTO, subir o backend e executar:
```bash
curl http://localhost:8080/v3/api-docs.yaml -o .specs/openapi.yaml
```
O campo `slug` vai aparecer no `ProjetoResponse` do YAML.

---

## PARTE B — Frontend

### Passo 0 — Regenerar SDK (obrigatório antes de qualquer código)

```bash
npm run api:gen
```

Confirmar que `src/shared/api/model/projetoResponse.ts` agora tem:
```ts
export interface ProjetoResponse {
  id?: string;
  titulo?: string;
  slug?: string;       // ← deve aparecer
  descricao?: string;
  // ...
}
```

---

### 1. CRIAR — `src/routes/admin.tsx` (layout pai das rotas admin)

Este arquivo é o que o TanStack Router usa como layout pai para todas as rotas
que começam com `/admin`. Sem ele, `AdminLayout` nunca é renderizado.

```tsx
import { createFileRoute } from '@tanstack/react-router'
import { AdminLayout } from '../features/admin/components/AdminLayout'

export const Route = createFileRoute('/admin')({
  component: AdminLayout,
})
```

> **Por que funciona:** No TanStack Router com file-based routing, `admin.tsx`
> é o layout pai de `admin.index.tsx`, `admin.profile.tsx`, etc. O `AdminLayout`
> já tem `<Outlet />` do TanStack Router — ele vai renderizar cada rota filha
> dentro da sidebar corretamente.

---

### 2. MODIFICAR — `src/routes/projects.$slug.tsx`

Substituir o arquivo inteiro:

```tsx
import { createFileRoute, notFound } from '@tanstack/react-router'
import { useGetApiV1PortfolioProjetosBySlug } from '../shared/api/generated/default/default'
import { ProjectDetailsPage } from '@/pages/portfolio/ProjectDetailsPage'
import { Skeleton } from '@/components/ui/skeleton'

export const Route = createFileRoute('/projects/$slug')({
  head: ({ params }) => ({
    meta: [
      { title: `Projeto — Michael Trindade` },
      { name: 'description', content: 'Detalhes do projeto no portfólio de Michael Trindade.' },
    ],
  }),
  notFoundComponent: () => (
    <div className="flex min-h-screen items-center justify-center px-4 text-center">
      <div>
        <h1 className="text-2xl font-semibold text-foreground">Projeto não encontrado</h1>
        <p className="mt-2 text-sm text-muted-foreground">
          O projeto que você procura não existe ou foi removido.
        </p>
      </div>
    </div>
  ),
  component: ProjectDetailRoute,
})

function ProjectDetailRoute() {
  const { slug } = Route.useParams()
  const { data: response, isLoading, isError } = useGetApiV1PortfolioProjetosBySlug(slug)
  const projeto = response?.data

  if (isLoading) {
    return (
      <div className="min-h-screen bg-background">
        <div className="mx-auto max-w-3xl px-6 py-16 flex flex-col gap-6">
          <Skeleton className="h-6 w-32" />
          <Skeleton className="h-12 w-3/4" />
          <Skeleton className="h-6 w-1/2" />
          <Skeleton className="aspect-video w-full rounded-lg" />
        </div>
      </div>
    )
  }

  if (isError || !projeto) {
    throw notFound()
  }

  return <ProjectDetailsPage projeto={projeto} />
}
```

> **Nota sobre o hook:** O nome exato depende do que o Orval gerou.
> Verificar em `src/shared/api/generated/default/default.ts` o hook para
> `GET /api/v1/portfolio/projetos/{slug}`. Deve ser algo como:
> `useGetApiV1PortfolioProjetosBySlug` ou `useGetApiV1PortfolioProjetosSlug`.
> Usar o nome correto conforme o gerado.

---

### 3. MODIFICAR — `src/pages/portfolio/ProjectDetailsPage.tsx`

Substituir o arquivo inteiro para usar `ProjetoResponse` da API em vez do tipo local `Project`:

```tsx
import { Link } from '@tanstack/react-router'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { ExternalLink, Github, ArrowLeft } from 'lucide-react'
import type { ProjetoResponse } from '@/shared/api/model'

interface ProjectDetailsPageProps {
  projeto: ProjetoResponse
}

function formatDate(iso?: string) {
  if (!iso) return null
  try {
    return new Date(iso).toLocaleDateString('pt-BR', {
      day: '2-digit',
      month: 'long',
      year: 'numeric',
    })
  } catch {
    return null
  }
}

export function ProjectDetailsPage({ projeto }: ProjectDetailsPageProps) {
  const data = formatDate(projeto.dataDesenvolvimento)

  return (
    <div className="min-h-screen bg-background">
      {/* Nav */}
      <nav className="mx-auto max-w-5xl px-6 pt-8">
        <Link
          to="/"
          className="inline-flex items-center gap-2 text-sm font-medium text-muted-foreground transition-colors hover:text-foreground"
        >
          <ArrowLeft className="w-4 h-4" />
          Voltar ao portfólio
        </Link>
      </nav>

      {/* Hero */}
      <header className="border-b border-border/60 bg-gradient-to-b from-muted/30 to-background mt-6">
        <div className="mx-auto flex max-w-3xl flex-col gap-6 px-6 py-16 sm:py-20">
          <div className="flex flex-col gap-3">
            <span className="text-xs font-medium uppercase tracking-[0.2em] text-muted-foreground">
              Projeto
            </span>
            <h1 className="text-3xl font-bold tracking-tight text-foreground sm:text-4xl md:text-5xl">
              {projeto.titulo}
            </h1>
            {projeto.descricao && (
              <p className="text-lg text-muted-foreground sm:text-xl">{projeto.descricao}</p>
            )}
          </div>

          <div className="flex flex-wrap items-center gap-x-4 gap-y-2 text-sm text-muted-foreground">
            {data && <time dateTime={projeto.dataDesenvolvimento}>{data}</time>}
            {data && projeto.tecnologias && projeto.tecnologias.length > 0 && (
              <span aria-hidden className="h-1 w-1 rounded-full bg-muted-foreground/50" />
            )}
            <div className="flex flex-wrap gap-2">
              {projeto.tecnologias?.map((tec) => (
                <Badge key={tec.id} variant="secondary">
                  {tec.nome}
                </Badge>
              ))}
            </div>
          </div>
        </div>

        {/* Capa */}
        {projeto.urlCapa && (
          <div className="mx-auto max-w-5xl px-6 pb-12">
            <div className="aspect-video w-full overflow-hidden rounded-lg border border-border/60 bg-muted">
              <img
                src={projeto.urlCapa}
                alt={`Capa do projeto ${projeto.titulo}`}
                className="h-full w-full object-cover"
                loading="lazy"
              />
            </div>
          </div>
        )}
      </header>

      {/* Descrição expandida (caso queira usar descricao como conteúdo principal) */}
      <article className="mx-auto max-w-2xl px-6 py-16">
        {projeto.descricao ? (
          <div className="flex flex-col gap-4">
            <h2 className="text-2xl font-semibold tracking-tight text-foreground">
              Sobre o projeto
            </h2>
            <p className="text-base leading-relaxed text-muted-foreground">
              {projeto.descricao}
            </p>
          </div>
        ) : (
          <p className="text-base text-muted-foreground">
            O conteúdo detalhado deste projeto será publicado em breve.
          </p>
        )}
      </article>

      {/* Links */}
      {(projeto.linkProducao || projeto.linkRepositorio) && (
        <section className="mx-auto max-w-2xl px-6 pb-20">
          <div className="flex flex-wrap gap-3 border-t border-border/60 pt-8">
            {projeto.linkProducao && (
              <Button asChild>
                <a href={projeto.linkProducao} target="_blank" rel="noreferrer">
                  <ExternalLink className="w-4 h-4 mr-2" />
                  Ver projeto ao vivo
                </a>
              </Button>
            )}
            {projeto.linkRepositorio && (
              <Button asChild variant="outline">
                <a href={projeto.linkRepositorio} target="_blank" rel="noreferrer">
                  <Github className="w-4 h-4 mr-2" />
                  Ver repositório
                </a>
              </Button>
            )}
          </div>
        </section>
      )}

      {/* Footer */}
      <footer className="border-t border-border/60 py-8 text-center text-xs text-muted-foreground">
        © {new Date().getFullYear()} Michael Trindade da Silva. Todos os direitos reservados.
      </footer>
    </div>
  )
}
```

---

### 4. MODIFICAR — `src/routes/projects.$slug.tsx` — atualizar head com dados reais

Após confirmar que `titulo` e `descricao` chegam do hook, atualizar o `head()`:

```tsx
// Dentro do Route, o head pode ser atualizado para usar dados do loader
// Por ora manter o título genérico — TanStack Start resolve dinamicamente via SSR
head: () => ({
  meta: [
    { title: 'Projeto — Michael Trindade' },
    { name: 'description', content: 'Detalhe de projeto no portfólio de Michael Trindade.' },
  ],
}),
```

---

### 5. VERIFICAR — `ProjectHero.tsx` e `ProjectContent.tsx`

Estes dois componentes usam o tipo local `Project`. Como o `ProjectDetailsPage`
foi reescrito sem usá-los diretamente, eles podem ser mantidos como estão (para
uso futuro) ou removidos. **Não remover nesta spec** — manter para evitar
quebrar imports em outros lugares. Apenas garantir que não são importados
pelo novo `ProjectDetailsPage`.

---

## Comportamento esperado após execução

```bash
# 1. Backend: slug no response
curl http://localhost:8080/api/v1/portfolio/projetos
# → cada projeto tem "slug": "meu-projeto"

# 2. Frontend: página de detalhe com dados reais
# Acessar http://localhost:3000/projects/meu-slug-real
# → Exibe título, descrição, tecnologias e links do banco — não "Alex Carvalho"

# 3. Footer correto
# → "© 2026 Michael Trindade da Silva. Todos os direitos reservados."

# 4. Admin com sidebar
# Acessar http://localhost:3000/admin
# → Sidebar com Dashboard, Novo Projeto, Perfil, Configurações aparece

# 5. Slug inexistente
# Acessar http://localhost:3000/projects/slug-falso
# → "Projeto não encontrado" (notFoundComponent)

# 6. Loading state
# Com rede lenta: skeletons aparecem antes do conteúdo
```

## O que NÃO fazer
- Não remover `ProjectHero.tsx` e `ProjectContent.tsx` — manter para futuro
- Não alterar `features/portfolio/mocks/data.ts` — deixar como está (usado pelos testes)
- Não criar loader SSR na rota — usar hook client-side (mais simples e suficiente)
- Não alterar nenhum outro arquivo de rota além de `projects.$slug.tsx` e o novo `admin.tsx`
- Não editar arquivos em `src/shared/api/generated/` manualmente
- Não alterar testes existentes
