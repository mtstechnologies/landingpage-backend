# ADR-002 — Idempotência por Redis no POST

**Status:** Aceito
**Data:** 2026-06
**Autor:** Michael Trindade da Silva

## Contexto

Operações HTTP `POST` não são nativamente idempotentes. Em ambientes com retry
automático (timeouts de rede, bugs no frontend), a mesma requisição de criação
pode ser enviada múltiplas vezes, resultando em dados duplicados. Para um CMS
de portfólio onde o admin cria projetos e tecnologias, duplicatas são um problema
real de integridade de dados.

## Decisão

Implementar idempotência via **Redis** com o padrão de `Idempotency-Key`:

- O cliente envia um UUID único no header `Idempotency-Key` em todo `POST`
- O `IdempotencyInterceptor` verifica o Redis antes de processar
- Se a chave já existe: retorna `409 Conflict`
- Se não existe: processa e armazena a chave com TTL de 10 minutos
- Requisições sem o header recebem `400 Bad Request`

## Alternativas consideradas

| Alternativa | Motivo da rejeição |
|-------------|-------------------|
| Constraint UNIQUE no banco | Cobre apenas duplicatas de dados, não de requisições |
| Idempotência no banco de dados | Requer lógica complexa de upsert em cada entidade |
| Sem idempotência | Risco de dados duplicados em retry |
| Idempotência em memória (sem Redis) | Não sobrevive a reinicializações da aplicação |

## Consequências

**Positivas:**
- Exatamente-uma-vez semântica para criações (`exactly-once semantics`)
- Proteção contra bugs de duplo-clique no frontend
- Redis já está no stack para cache — sem dependência nova

**Negativas:**
- Todos os clientes precisam gerar e enviar `Idempotency-Key`
- Adiciona latência de rede para consulta ao Redis por requisição
- TTL de 10 minutos significa que após esse período a mesma chave pode ser reprocessada
