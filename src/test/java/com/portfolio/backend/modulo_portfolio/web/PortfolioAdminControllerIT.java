package com.portfolio.backend.modulo_portfolio.web;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
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
import org.junit.jupiter.api.BeforeEach;
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
}
