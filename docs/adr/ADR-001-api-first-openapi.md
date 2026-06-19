# ADR-001 — API-First com OpenAPI e SpringDoc

**Status:** Aceito
**Data:** 2026-06
**Autor:** Michael Trindade da Silva

## Contexto

O projeto possui dois consumidores distintos da API: a landing page pública
(leitura de dados) e o painel administrativo (escrita). O frontend é desenvolvido
separadamente e precisa de um contrato estável para gerar código automaticamente.
Sem um contrato formal, mudanças no backend quebram silenciosamente o frontend.

## Decisão

Adotar a abordagem **API-First**: o contrato OpenAPI é a fonte de verdade.

- O SpringDoc gera o `openapi.yaml` automaticamente a partir das anotações do Spring
- O arquivo é exportado automaticamente pelo CI e commitado em `.specs/openapi.yaml`
- O frontend consome o YAML via **Orval** para gerar hooks React Query e tipos TypeScript
- Qualquer divergência entre backend e frontend quebra o build antes de chegar em produção

## Alternativas consideradas

| Alternativa | Motivo da rejeição |
|-------------|-------------------|
| Contract-first (escrever YAML à mão) | Overhead de manutenção; YAML e código ficam dessincronizados |
| GraphQL | Overkill para um portfólio com modelo de dados simples |
| tRPC | Requer Node.js no backend; incompatível com Spring Boot |
| Sem contrato formal | Acoplamento frágil; erros só aparecem em runtime |

## Consequências

**Positivas:**
- Frontend sempre tipado com os tipos exatos do backend
- Mudanças no contrato são detectadas no CI antes do merge
- Documentação Swagger UI gratuita em `/swagger-ui.html`
- Onboarding de novos desenvolvedores mais rápido

**Negativas:**
- Adiciona um step de geração de SDK ao fluxo de desenvolvimento
- `api:gen` precisa ser rodado após cada mudança no backend
