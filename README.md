# Portfolio Back-end (Spring Boot)

API de suporte ao sistema de portfólio de engenharia de software de Michael Trindade. Este projeto implementa uma arquitetura modular baseada em Clean Architecture, utilizando Spring Boot e PostgreSQL.

## 🚀 Tecnologias e Arquitetura
- **Stack:** Java 21+ com Spring Boot 3.x.
- **Arquitetura:** Clean Architecture (DDD estratégico e modular).
- **Persistência:** Spring Data JPA com Hibernate e PostgreSQL.
- **API Documentation:** SpringDoc OpenAPI (geração automática do contrato).
- **Monitoramento/Testes:** Protocolos para testes de performance em microsserviços (foco de pesquisa de mestrado na UFRGS).

## 🛠 Como Iniciar

### Pré-requisitos
- JDK 21 instalado.
- Docker para levantar a instância do PostgreSQL:
  ```bash
  docker-compose up -d

  Instalação e Execução
Bash
./mvnw spring-boot:run

A API estará disponível em: http://localhost:8080

Contrato OpenAPI: O arquivo openapi.yaml é gerado automaticamente na inicialização. Você pode acessá-lo em http://localhost:8080/v3/api-docs.yaml e copiar para o front-end.

🏛 Convenções de Código

Clean Architecture: As camadas (domain, application, infrastructure, web) devem ser estritamente respeitadas.

Design Pattern: A comunicação entre as camadas é feita através de Interfaces (DIP).

Testes: Cada nova funcionalidade deve incluir testes unitários e de integração, garantindo o rigor científico e de engenharia.

🧬 Ciclo de Vida do Contrato (API-First)

Alteração na camada web (Controllers).

O SpringDoc atualiza o contrato (openapi.yaml).

O front-end consome o contrato para atualizar o SDK.

📑 Endpoints Principais

Endpoint: Descrição: Método:

/api/v1/projects: Listar todos os projetos: GET

/api/v1/projects/{id}: Buscar projeto por ID: GET

/api/v1/projects: Criar novo projeto: POST

/api/v1/projects/{id}: Atualizar projeto: PUT

/api/v1/projects/{id}: Deletar projeto: DELETE

Documentação: 
A documentação completa da API está disponível em http://localhost:8080/swagger-ui.html após iniciar o servidor.

✉️ Contato
Para mais informações sobre a API, projetos ou publicações acadêmicas, entre em contato com [michaeltrindadedasilva@gmail.com].