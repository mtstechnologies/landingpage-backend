package com.portfolio.backend.modulo_portfolio.application.service;

import com.portfolio.backend.modulo_portfolio.domain.Perfil;
import com.portfolio.backend.modulo_portfolio.domain.Projeto;
import com.portfolio.backend.modulo_portfolio.domain.Tecnologia;
import com.portfolio.backend.modulo_portfolio.domain.exception.PerfilNaoEncontradoException;
import com.portfolio.backend.modulo_portfolio.domain.exception.ProjetoNaoEncontradoException;
import com.portfolio.backend.modulo_portfolio.domain.exception.RecursoNaoEncontradoException;
import com.portfolio.backend.modulo_portfolio.domain.exception.TecnologiaDuplicadaException;
import com.portfolio.backend.modulo_portfolio.infrastructure.repository.PerfilRepository;
import com.portfolio.backend.modulo_portfolio.infrastructure.repository.ProjetoRepository;
import com.portfolio.backend.modulo_portfolio.infrastructure.repository.TecnologiaRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GestaoPortfolioService {

    private final PerfilRepository perfilRepository;
    private final ProjetoRepository projetoRepository;
    private final TecnologiaRepository tecnologiaRepository;

    public GestaoPortfolioService(
            PerfilRepository perfilRepository,
            ProjetoRepository projetoRepository,
            TecnologiaRepository tecnologiaRepository) {
        this.perfilRepository = perfilRepository;
        this.projetoRepository = projetoRepository;
        this.tecnologiaRepository = tecnologiaRepository;
    }

    @Transactional(readOnly = true)
    public Perfil obterPerfil() {
        return perfilRepository.findFirstByOrderByNomeAsc()
                .orElseThrow(() -> new PerfilNaoEncontradoException("Perfil não configurado no sistema."));
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Perfil criarPerfil(Perfil perfil) {
        return perfilRepository.save(perfil);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Perfil atualizarPerfil(UUID id, Perfil perfilDados) {
        Perfil perfil = perfilRepository.findById(id)
                .orElseThrow(() -> new PerfilNaoEncontradoException(id));
        perfil.setNome(perfilDados.getNome());
        perfil.setTitulo(perfilDados.getTitulo());
        perfil.setBio(perfilDados.getBio());
        perfil.setLinkLinkedin(perfilDados.getLinkLinkedin());
        perfil.setLinkGithub(perfilDados.getLinkGithub());
        perfil.setUrlFoto(perfilDados.getUrlFoto());
        if (perfilDados.getUsuarioId() != null) {
            perfil.setUsuarioId(perfilDados.getUsuarioId());
        }
        return perfilRepository.save(perfil);
    }

    @Transactional(readOnly = true)
    public List<Projeto> listarProjetos() {
        return projetoRepository.findAllWithTecnologiasOrderByDataDesenvolvimentoDesc();
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Projeto criarProjeto(Projeto projeto, Set<UUID> tecnologiaIds) {
        if (tecnologiaIds != null && !tecnologiaIds.isEmpty()) {
            Set<Tecnologia> tecnologias = new HashSet<>();
            for (UUID techId : tecnologiaIds) {
                Tecnologia tech = tecnologiaRepository.findById(techId)
                        .orElseThrow(() -> new RecursoNaoEncontradoException("Tecnologia não encontrada com o ID: " + techId));
                tecnologias.add(tech);
            }
            projeto.setTecnologias(tecnologias);
        }
        return projetoRepository.save(projeto);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Projeto atualizarProjeto(UUID id, Projeto dadosProjeto, Set<UUID> tecnologiaIds) {
        Projeto projeto = projetoRepository.findById(id)
                .orElseThrow(() -> new ProjetoNaoEncontradoException(id));
        projeto.setTitulo(dadosProjeto.getTitulo());
        projeto.setDescricao(dadosProjeto.getDescricao());
        projeto.setUrlCapa(dadosProjeto.getUrlCapa());
        projeto.setLinkProducao(dadosProjeto.getLinkProducao());
        projeto.setLinkRepositorio(dadosProjeto.getLinkRepositorio());
        projeto.setDataDesenvolvimento(dadosProjeto.getDataDesenvolvimento());

        if (tecnologiaIds != null) {
            Set<Tecnologia> tecnologias = new HashSet<>();
            for (UUID techId : tecnologiaIds) {
                Tecnologia tech = tecnologiaRepository.findById(techId)
                        .orElseThrow(() -> new RecursoNaoEncontradoException("Tecnologia não encontrada com o ID: " + techId));
                tecnologias.add(tech);
            }
            projeto.setTecnologias(tecnologias);
        }
        return projetoRepository.save(projeto);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void excluirProjeto(UUID id) {
        if (!projetoRepository.existsById(id)) {
            throw new ProjetoNaoEncontradoException(id);
        }
        projetoRepository.deleteById(id);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Tecnologia criarTecnologia(Tecnologia tecnologia) {
        if (tecnologiaRepository.existsByNome(tecnologia.getNome())) {
            throw new TecnologiaDuplicadaException(tecnologia.getNome());
        }
        return tecnologiaRepository.save(tecnologia);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void associarTecnologia(UUID projetoId, UUID tecnologiaId) {
        Projeto projeto = projetoRepository.findById(projetoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Projeto não encontrado com o ID: " + projetoId));
        Tecnologia tecnologia = tecnologiaRepository.findById(tecnologiaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tecnologia não encontrada com o ID: " + tecnologiaId));
        projeto.getTecnologias().add(tecnologia);
        projetoRepository.save(projeto);
    }
}
