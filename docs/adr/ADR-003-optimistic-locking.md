# ADR-003 — Controle de concorrência com Optimistic Locking

**Status:** Aceito
**Data:** 2026-06
**Autor:** Michael Trindade da Silva

## Contexto

O painel administrativo pode ser usado em múltiplas sessões simultaneamente
(ex: admin logado no celular e no desktop). Sem controle de concorrência,
a segunda gravação silenciosamente sobrescreve a primeira, causando perda de dados.

## Decisão

Usar **Optimistic Locking** via campo `@Version` do JPA/Hibernate em todas as entidades:
`Perfil`, `Projeto` e `Tecnologia`.

- Cada entidade tem um campo `versao: Integer` anotado com `@Version`
- O Hibernate verifica que a versão no banco corresponde à versão enviada antes do UPDATE
- Se houver conflito: `ObjectOptimisticLockingFailureException` → `GlobalExceptionHandler` → `409 Conflict`

## Alternativas consideradas

| Alternativa | Motivo da rejeição |
|-------------|-------------------|
| Pessimistic Locking (`SELECT FOR UPDATE`) | Degrada performance; bloqueia leituras |
| Last-write-wins (sem controle) | Perda silenciosa de dados |
| Timestamp-based optimistic locking | Menos confiável que versão inteira em ambientes distribuídos |

## Consequências

**Positivas:**
- Zero locks no banco de dados em leituras (performance máxima)
- Detecção determinística de conflitos
- Implementação trivial — uma anotação por entidade

**Negativas:**
- O frontend precisa enviar o campo `versao` ao fazer PUT
- Em conflito, o usuário precisa recarregar os dados e tentar novamente
