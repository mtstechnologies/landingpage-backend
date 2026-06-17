# Spec 001_BE: Setup do Domínio Core e Gestão de Portfólio (Back-end)

## 1. Objetivo e Escopo
- **Propósito:** Implementar a fundação de dados e a API do CMS da landing page, permitindo o cadastro, atualização, remoção e listagem do perfil profissional, dos projetos desenvolvidos e das tecnologias utilizadas. (Conformidade ISO 29148:2018).

## 2. Desenho Arquitetural (Monólito Modular)
- **Bounded Context Alvo:** `modulo_portfolio`

### Camada Domain (`domain/`)
- **Entidades JPA:**
  - `Perfil`: campos `id` (UUID), `nome` (String), `titulo` (String), `bio` (Text), `link_linkedin` (String), `link_github` (String), `url_foto` (String), `versao` (int).
  - `Projeto`: campos `id` (UUID), `titulo` (String), `descricao` (Text), `url_capa` (String), `link_producao` (String), `link_repositorio` (String), `data_desenvolvimento` (LocalDate), `versao` (int).
  - `Tecnologia`: campos `id` (UUID), `nome` (String, Unique), `url_icone` (String), `versao` (int).
- **Controle de Concorrência:** Adicionar obrigatoriamente a anotação `@Version` na coluna `versao` de todas as entidades para mitigar conflitos de escrita simultânea no painel administrativo.
- **Relacionamento:** `Projeto` e `Tecnologia` possuem relacionamento Many-to-Many. A entidade `Projeto` será a dona do relacionamento.
- **Exceções de Domínio:** `PerfilNaoEncontradoException`, `ProjetoNaoEncontradoException`, `TecnologiaDuplicadaException`.

### Camada Application (`application/service/`)
- Criar `GestaoPortfolioService`. Esta classe deve orquestrar todas as regras de negócio de mutação e associação.
- **Isolamento de Transação:** Todos os métodos de persistência devem utilizar `@Transactional(propagation = Propagation.REQUIRED)`. É terminantemente proibido realizar qualquer chamada de rede externa (como uploads para buckets S3 ou validações de links HTTP) dentro do escopo de métodos anotados com `@Transactional`.
- A vinculação de uma `Tecnologia` existente a um `Projeto` deve validar a existência prévia de ambos os IDs antes de atualizar a tabela associativa.

### Camada Infrastructure (`infrastructure/`)
- **Persistência:** Criar a migração SQL isolada utilizando Flyway em `src/main/resources/db/migration/portfolio/V1_1__portfolio_criar_tabelas_iniciais.sql`.
- **Regra de Isolamento de Banco:** As tabelas do `modulo_portfolio` não devem possuir restrições físicas de chave estrangeira (`FOREIGN KEY`) apontando para tabelas de outros contextos (como uma tabela global de usuários/autenticação). O vínculo deve ser estritamente lógico através do campo `usuario_id` (UUID) indexado.

### Camada Web / Controller (`web/`)
- **`PortfolioPublicoController`**: Expor rotas de leitura pública (sem necessidade de token JWT):
  - `GET /api/v1/portfolio/perfil`
  - `GET /api/v1/portfolio/projetos`
- **`PortfolioAdminController`**: Expor rotas de escrita (protegidas por segurança de borda):
  - `POST /api/v1/admin/portfolio/projetos`
  - `PUT /api/v1/admin/portfolio/projetos/{id}`
  - `DELETE /api/v1/admin/portfolio/projetos/{id}`
  - O acesso deve ser bloqueado com a anotação `@PreAuthorize("hasAuthority('PORTFOLIO_ESCRITA')")`.
- **Idempotência:** As rotas de criação (`POST`) no painel administrativo devem validar obrigatoriamente o cabeçalho `Idempotency-Key` via interceptador Redis antes de processar a camada de aplicação.

## 3. Contrato OpenAPI
O arquivo `src/main/resources/openapi/openapi.yaml` deve ser gerado/atualizado contendo os schemas estritos para os payloads de Request e Response DTOs.
- **Path:** `/api/v1/portfolio/projetos`
- **Responses Obrigatórias:** - `200 OK` para listagens.
  - `201 Created` para criações com o header `Location`.
  - `400 Bad Request` retornando o formato de erro estruturado RFC 7807 (Problem Details).
  - `409 Conflict` caso a `Idempotency-Key` já tenha sido processada ou esteja em andamento.

## 4. Suíte de Testes Requerida

### Testes Unitários (`GestaoPortfolioServiceTest`)
- Validar comportamento ao tentar associar uma tecnologia inexistente a um projeto, garantindo o lançamento de `RecursoNaoEncontradoException`.
- Validar que o método de atualização de perfil atualiza os campos corretamente quando o otimisto locking (`@Version`) não é violado.

### Testes de Integração (`PortfolioAdminControllerIT`)
- Utilizar **Testcontainers** para subir uma instância real e efêmera do PostgreSQL corporativo durante a execução da suíte.
- Validar o fluxo de ponta a ponta: efetuar um disparo `POST` simulando o painel administrativo, inspecionar se o registro foi corretamente inserido no banco efêmero e verificar se o cabeçalho de idempotência bloqueou uma requisição idêntica subsequente.

## 5. Test Harness (Critérios de Validação do Back-end)
O subagente de IA deve garantir autonomamente:
- [ ] O projeto compila com 0 erros via comando de build (`mvn clean compile`).
- [ ] 100% dos testes da suíte unitária e de integração passam com sucesso.
- [ ] O interceptador de Logs Estruturados em formato JSON propaga o `X-Trace-Id` por todas as camadas do `modulo_portfolio`.
- [ ] O encerramento de cada thread limpa o contexto de segurança e MDC de forma limpa para evitar vazamentos de memória.