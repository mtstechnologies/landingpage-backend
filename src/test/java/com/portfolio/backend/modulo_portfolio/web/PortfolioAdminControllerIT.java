package com.portfolio.backend.modulo_portfolio.web;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.backend.modulo_portfolio.domain.Projeto;
import com.portfolio.backend.modulo_portfolio.infrastructure.repository.ProjetoRepository;
import com.portfolio.backend.modulo_portfolio.web.dto.ProjetoRequest;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class PortfolioAdminControllerIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // Disable Flyway clean-on-validation to prevent exceptions on test restart
        registry.add("spring.flyway.clean-disabled", () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProjetoRepository projetoRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    void setUp() {
        projetoRepository.deleteAll();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @WithMockUser(authorities = "PORTFOLIO_ESCRITA")
    void testFluxoCriacaoProjetoComIdempotencia() throws Exception {
        // Arrange
        String idempotencyKey = "test-key-123";
        ProjetoRequest request = new ProjetoRequest(
                "Projeto Monólito",
                "projeto-monolito",
                "Descrição detalhada",
                "http://foto.jpg",
                "http://prod.com",
                "http://repo.com",
                LocalDate.now(),
                Set.of()
        );

        // First POST mock: setIfAbsent returns true (allowed)
        when(valueOperations.setIfAbsent(eq("idempotency:" + idempotencyKey), eq("PROCESSING"), any(Duration.class)))
                .thenReturn(true);

        // Act - First Request (Creates the project)
        mockMvc.perform(post("/api/v1/admin/portfolio/projetos")
                        .header("Idempotency-Key", idempotencyKey)
                        .header("X-Trace-Id", "trace-id-999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(header().string("X-Trace-Id", "trace-id-999"))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.titulo", is("Projeto Monólito")))
                .andExpect(jsonPath("$.versao", is(0)));

        // Verify that the record was actually inserted into PostgreSQL (efêmero)
        List<Projeto> projetosNoBanco = projetoRepository.findAll();
        assertEquals(1, projetosNoBanco.size());
        assertEquals("Projeto Monólito", projetosNoBanco.get(0).getTitulo());

        // Second POST mock: setIfAbsent returns false (duplicate!)
        when(valueOperations.setIfAbsent(eq("idempotency:" + idempotencyKey), eq("PROCESSING"), any(Duration.class)))
                .thenReturn(false);

        // Act - Second Request (Blocked by Idempotency Key)
        mockMvc.perform(post("/api/v1/admin/portfolio/projetos")
                        .header("Idempotency-Key", idempotencyKey)
                        .header("X-Trace-Id", "trace-id-999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title", is("Conflito de Idempotência")))
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.detail", is("Esta requisição já foi processada ou está em processamento.")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));

        // Verify that no second project was inserted
        assertEquals(1, projetoRepository.count());
    }

    @Test
    @WithMockUser(authorities = "PORTFOLIO_ESCRITA")
    void testCriacaoProjetoSemIdempotencyKey_RetornaBadRequest() throws Exception {
        // Arrange
        ProjetoRequest request = new ProjetoRequest(
                "Projeto Sem Chave",
                "projeto-sem-chave",
                "Descrição",
                "http://foto.jpg",
                "http://prod.com",
                "http://repo.com",
                LocalDate.now(),
                Set.of()
        );

        // Act & Assert
        mockMvc.perform(post("/api/v1/admin/portfolio/projetos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Cabeçalho Idempotency-Key Ausente")))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.detail", is("O cabeçalho 'Idempotency-Key' é obrigatório para esta operação.")));

        // Verify no project was inserted
        assertTrue(projetoRepository.findAll().isEmpty());
    }

// ============================================================
// Testes do endpoint GET /portfolio/projetos/{slug} (público)
// ============================================================

    @Test
    @DisplayName("GET /portfolio/projetos/{slug} — deve retornar projeto quando slug existe")
    void deveBuscarProjetoPorSlugExistente() throws Exception {
        when(valueOperations.setIfAbsent(any(), any(), any())).thenReturn(true);
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
        when(valueOperations.setIfAbsent(any(), any(), any())).thenReturn(true);
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
        when(valueOperations.setIfAbsent(any(), any(), any())).thenReturn(true);
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
        when(valueOperations.setIfAbsent(any(), any(), any())).thenReturn(true);
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
}
