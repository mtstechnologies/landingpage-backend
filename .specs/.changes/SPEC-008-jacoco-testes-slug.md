# SPEC-008 — Jacoco no pom.xml + Testes do endpoint slug

## Contexto
O CI backend já tem o step `jacoco:check`, mas o plugin Jacoco não está
configurado no `pom.xml` — o step vai falhar em silêncio (continue-on-error: true).
Além disso, os testes de integração em `PortfolioAdminControllerIT` não cobrem
o campo `slug` introduzido na SPEC-002.

## Pré-requisito
SPEC-007 executada.

---

## Arquivos a modificar

### 1. `pom.xml` — Adicionar plugin Jacoco

Localizar o bloco `<build><plugins>` e adicionar dentro de `<plugins>`,
após o `maven-surefire-plugin`:

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.12</version>
    <executions>
        <execution>
            <id>prepare-agent</id>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>verify</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
        <execution>
            <id>check</id>
            <phase>verify</phase>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
                <rules>
                    <rule>
                        <element>BUNDLE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.70</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
                <excludes>
                    <!-- Excluir classes geradas e configuração -->
                    <exclude>com/portfolio/backend/BackendApplication.class</exclude>
                    <exclude>com/portfolio/backend/config/**</exclude>
                    <exclude>com/portfolio/backend/modulo_portfolio/domain/exception/**</exclude>
                    <exclude>com/portfolio/backend/modulo_portfolio/web/dto/**</exclude>
                    <exclude>com/portfolio/backend/modulo_portfolio/infrastructure/repository/**</exclude>
                </excludes>
            </configuration>
        </execution>
    </executions>
</plugin>
```

> O threshold de 70% cobre o que já temos. Aumentar para 80% quando os novos
> testes desta spec estiverem passando.

---

### 2. `src/test/java/com/portfolio/backend/modulo_portfolio/web/PortfolioAdminControllerIT.java`

Adicionar os seguintes métodos de teste à classe existente (não substituir — apenas adicionar):

```java
// ============================================================
// Testes do endpoint GET /portfolio/projetos/{slug} (público)
// ============================================================

@Test
@DisplayName("GET /portfolio/projetos/{slug} — deve retornar projeto quando slug existe")
void deveBuscarProjetoPorSlugExistente() throws Exception {
    // Arrange — criar projeto com slug
    String idempotencyKey = UUID.randomUUID().toString();
    String requestBody = """
        {
            "titulo": "Projeto Slug Test",
            "slug": "projeto-slug-test",
            "descricao": "Descrição do projeto para teste de slug.",
            "urlCapa": null,
            "linkProducao": null,
            "linkRepositorio": null,
            "dataDesenvolvimento": "2026-01-15",
            "tecnologiaIds": []
        }
        """;

    mockMvc.perform(post("/api/v1/admin/portfolio/projetos")
            .with(httpBasic("admin", "troque-em-producao"))
            .header("Idempotency-Key", idempotencyKey)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isCreated());

    // Act & Assert — buscar por slug
    mockMvc.perform(get("/api/v1/portfolio/projetos/projeto-slug-test"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.slug").value("projeto-slug-test"))
        .andExpect(jsonPath("$.titulo").value("Projeto Slug Test"));
}

@Test
@DisplayName("GET /portfolio/projetos/{slug} — deve retornar 404 quando slug não existe")
void deveRetornar404ParaSlugInexistente() throws Exception {
    mockMvc.perform(get("/api/v1/portfolio/projetos/slug-que-nao-existe"))
        .andExpect(status().isNotFound());
}

@Test
@DisplayName("POST /admin/portfolio/projetos — deve rejeitar slug com formato inválido")
void deveRejeitarSlugComFormatoInvalido() throws Exception {
    String idempotencyKey = UUID.randomUUID().toString();
    String requestBody = """
        {
            "titulo": "Projeto Inválido",
            "slug": "Slug Com Espacos E MAIUSCULAS",
            "descricao": "Descrição.",
            "urlCapa": null,
            "linkProducao": null,
            "linkRepositorio": null,
            "dataDesenvolvimento": "2026-01-15",
            "tecnologiaIds": []
        }
        """;

    mockMvc.perform(post("/api/v1/admin/portfolio/projetos")
            .with(httpBasic("admin", "troque-em-producao"))
            .header("Idempotency-Key", idempotencyKey)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isBadRequest());
}

@Test
@DisplayName("POST /admin/portfolio/projetos — deve retornar 409 para slug duplicado")
void deveRetornar409ParaSlugDuplicado() throws Exception {
    // Criar primeiro projeto
    String key1 = UUID.randomUUID().toString();
    String body = """
        {
            "titulo": "Projeto Original",
            "slug": "projeto-duplicado",
            "descricao": "Primeiro projeto.",
            "urlCapa": null,
            "linkProducao": null,
            "linkRepositorio": null,
            "dataDesenvolvimento": "2026-01-15",
            "tecnologiaIds": []
        }
        """;
    mockMvc.perform(post("/api/v1/admin/portfolio/projetos")
            .with(httpBasic("admin", "troque-em-producao"))
            .header("Idempotency-Key", key1)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isCreated());

    // Tentar criar segundo com mesmo slug (nova Idempotency-Key)
    String key2 = UUID.randomUUID().toString();
    String bodyDuplicado = """
        {
            "titulo": "Projeto Cópia",
            "slug": "projeto-duplicado",
            "descricao": "Segundo projeto com mesmo slug.",
            "urlCapa": null,
            "linkProducao": null,
            "linkRepositorio": null,
            "dataDesenvolvimento": "2026-02-15",
            "tecnologiaIds": []
        }
        """;
    mockMvc.perform(post("/api/v1/admin/portfolio/projetos")
            .with(httpBasic("admin", "troque-em-producao"))
            .header("Idempotency-Key", key2)
            .contentType(MediaType.APPLICATION_JSON)
            .content(bodyDuplicado))
        .andExpect(status().isConflict());
}

// ============================================================
// Teste do GET /admin/portfolio/tecnologias (SPEC-006)
// ============================================================

@Test
@DisplayName("GET /admin/portfolio/tecnologias — deve listar tecnologias cadastradas")
void deveListarTecnologias() throws Exception {
    // Criar tecnologia primeiro
    String key = UUID.randomUUID().toString();
    mockMvc.perform(post("/api/v1/admin/portfolio/tecnologias")
            .with(httpBasic("admin", "troque-em-producao"))
            .header("Idempotency-Key", key)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"nome\": \"Java\", \"urlIcone\": null}"))
        .andExpect(status().isCreated());

    // Listar
    mockMvc.perform(get("/api/v1/admin/portfolio/tecnologias")
            .with(httpBasic("admin", "troque-em-producao")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].nome").value("Java"));
}
```

> Verificar os imports necessários no topo da classe de teste.
> Os que já existem (`MockMvc`, `post`, `get`, `status`, `jsonPath`,
> `httpBasic`, `MediaType`, `UUID`) provavelmente já estão importados.
> Adicionar apenas os que faltarem.

---

### 3. CI backend — remover `continue-on-error`

**Arquivo:** `.github/workflows/ci.yml`

Localizar:
```yaml
      - name: Verificar cobertura (falha se < 60%)
        run: ./mvnw jacoco:check -B
        continue-on-error: true  # remover quando cobertura estiver estável
```

Substituir por:
```yaml
      - name: Verificar cobertura (falha se < 70%)
        run: ./mvnw jacoco:check -B
```

---

## Comportamento esperado após execução

```bash
# Rodar todos os testes
./mvnw test

# Deve passar incluindo os 5 novos testes:
# ✅ deveBuscarProjetoPorSlugExistente
# ✅ deveRetornar404ParaSlugInexistente
# ✅ deveRejeitarSlugComFormatoInvalido
# ✅ deveRetornar409ParaSlugDuplicado
# ✅ deveListarTecnologias

# Verificar cobertura
./mvnw verify
# Deve passar o check do Jacoco (70% de linhas cobertas)
# Relatório HTML gerado em: target/site/jacoco/index.html
```

## O que NÃO fazer
- Não alterar os testes existentes — apenas adicionar os novos
- Não aumentar o threshold além de 70% nesta spec
- Não mockar o banco de dados — usar Testcontainers como os testes existentes
- Não criar novos arquivos de teste — adicionar na classe existente
- Não alterar `GestaoPortfolioServiceTest.java`
