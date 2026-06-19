# ADR-004 — Monolito Modular com Clean Architecture

**Status:** Aceito
**Data:** 2026-06
**Autor:** Michael Trindade da Silva

## Contexto

Para um portfólio pessoal, a escolha da arquitetura precisa balancear
complexidade operacional (microsserviços são caros de operar sozinho)
com qualidade de código (código monolítico puro é difícil de evoluir).

## Decisão

Adotar **Monolito Modular** com separação estrita de camadas inspirada em
Clean Architecture e DDD estratégico:

```
config/           → Cross-cutting concerns (segurança, idempotência, MDC)
modulo_portfolio/ → Bounded Context isolado
  domain/         → Entidades JPA + exceções de domínio
  application/    → Service com regras de negócio
  infrastructure/ → Repositories (Spring Data JPA)
  web/            → Controllers + DTOs
```

Referências lógicas entre bounded contexts (ex: `usuario_id` em `Perfil`)
são UUIDs simples — sem FK física — preservando o isolamento.

## Alternativas consideradas

| Alternativa | Motivo da rejeição |
|-------------|-------------------|
| Microsserviços | Overhead operacional inviável para um dev solo |
| Monolito em camadas simples | Difícil de escalar para novos módulos sem acoplamento |
| Serverless (AWS Lambda) | Cold start e vendor lock-in desnecessários |

## Consequências

**Positivas:**
- Deploy simples: um único JAR + Docker Compose
- Facilmente evoluível para microsserviços — módulos já são isolados
- Testabilidade alta — cada camada é independente

**Negativas:**
- Um único processo; falha total afeta tudo
- Escala vertical apenas (por ora suficiente para portfólio)
