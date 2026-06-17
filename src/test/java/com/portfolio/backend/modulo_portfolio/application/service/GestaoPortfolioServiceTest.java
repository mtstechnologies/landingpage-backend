package com.portfolio.backend.modulo_portfolio.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.portfolio.backend.modulo_portfolio.domain.Perfil;
import com.portfolio.backend.modulo_portfolio.domain.Projeto;
import com.portfolio.backend.modulo_portfolio.domain.exception.RecursoNaoEncontradoException;
import com.portfolio.backend.modulo_portfolio.infrastructure.repository.PerfilRepository;
import com.portfolio.backend.modulo_portfolio.infrastructure.repository.ProjetoRepository;
import com.portfolio.backend.modulo_portfolio.infrastructure.repository.TecnologiaRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GestaoPortfolioServiceTest {

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private ProjetoRepository projetoRepository;

    @Mock
    private TecnologiaRepository tecnologiaRepository;

    @InjectMocks
    private GestaoPortfolioService gestaoPortfolioService;

    private UUID projetoId;
    private UUID tecnologiaId;
    private UUID perfilId;

    @BeforeEach
    void setUp() {
        projetoId = UUID.randomUUID();
        tecnologiaId = UUID.randomUUID();
        perfilId = UUID.randomUUID();
    }

    @Test
    void testAssociarTecnologiaInexistenteProjeto_LancaRecursoNaoEncontradoException() {
        // Arrange
        Projeto projeto = Projeto.builder().id(projetoId).titulo("Projeto Teste").build();
        when(projetoRepository.findById(projetoId)).thenReturn(Optional.of(projeto));
        when(tecnologiaRepository.findById(tecnologiaId)).thenReturn(Optional.empty());

        // Act & Assert
        RecursoNaoEncontradoException exception = assertThrows(
                RecursoNaoEncontradoException.class,
                () -> gestaoPortfolioService.associarTecnologia(projetoId, tecnologiaId)
        );

        assertEquals("Tecnologia não encontrada com o ID: " + tecnologiaId, exception.getMessage());
        verify(projetoRepository, times(1)).findById(projetoId);
        verify(tecnologiaRepository, times(1)).findById(tecnologiaId);
        verify(projetoRepository, times(0)).save(any(Projeto.class));
    }

    @Test
    void testAtualizarPerfilComSucesso_QuandoNaoHaViolacaoDeLock() {
        // Arrange
        Perfil perfilExistente = Perfil.builder()
                .id(perfilId)
                .nome("Nome Antigo")
                .titulo("Titulo Antigo")
                .bio("Bio Antiga")
                .versao(1)
                .build();

        Perfil dadosAtualizados = Perfil.builder()
                .nome("Nome Novo")
                .titulo("Titulo Novo")
                .bio("Bio Nova")
                .linkLinkedin("linkedin.com/novo")
                .linkGithub("github.com/novo")
                .urlFoto("foto.jpg")
                .build();

        when(perfilRepository.findById(perfilId)).thenReturn(Optional.of(perfilExistente));
        when(perfilRepository.save(any(Perfil.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Perfil perfilResultado = gestaoPortfolioService.atualizarPerfil(perfilId, dadosAtualizados);

        // Assert
        assertNotNull(perfilResultado);
        assertEquals("Nome Novo", perfilResultado.getNome());
        assertEquals("Titulo Novo", perfilResultado.getTitulo());
        assertEquals("Bio Nova", perfilResultado.getBio());
        assertEquals("linkedin.com/novo", perfilResultado.getLinkLinkedin());
        assertEquals("github.com/novo", perfilResultado.getLinkGithub());
        assertEquals("foto.jpg", perfilResultado.getUrlFoto());
        assertEquals(1, perfilResultado.getVersao());

        verify(perfilRepository, times(1)).findById(perfilId);
        verify(perfilRepository, times(1)).save(perfilExistente);
    }
}
