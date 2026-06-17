package com.portfolio.backend.modulo_portfolio.web;

import com.portfolio.backend.modulo_portfolio.application.service.GestaoPortfolioService;
import com.portfolio.backend.modulo_portfolio.domain.Perfil;
import com.portfolio.backend.modulo_portfolio.domain.Projeto;
import com.portfolio.backend.modulo_portfolio.domain.Tecnologia;
import com.portfolio.backend.modulo_portfolio.web.dto.PerfilResponse;
import com.portfolio.backend.modulo_portfolio.web.dto.ProjetoRequest;
import com.portfolio.backend.modulo_portfolio.web.dto.ProjetoResponse;
import com.portfolio.backend.modulo_portfolio.web.dto.TecnologiaRequest;
import com.portfolio.backend.modulo_portfolio.web.dto.TecnologiaResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/admin/portfolio")
@PreAuthorize("hasAuthority('PORTFOLIO_ESCRITA')")
public class PortfolioAdminController {

    private final GestaoPortfolioService gestaoPortfolioService;

    public PortfolioAdminController(GestaoPortfolioService gestaoPortfolioService) {
        this.gestaoPortfolioService = gestaoPortfolioService;
    }

    @PostMapping("/perfil")
    public ResponseEntity<PerfilResponse> criarPerfil(@Valid @RequestBody Perfil perfil) {
        Perfil novoPerfil = gestaoPortfolioService.criarPerfil(perfil);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(novoPerfil.getId())
                .toUri();
        return ResponseEntity.created(uri).body(PerfilResponse.fromEntity(novoPerfil));
    }

    @PutMapping("/perfil/{id}")
    public ResponseEntity<PerfilResponse> atualizarPerfil(@PathVariable UUID id, @Valid @RequestBody Perfil perfil) {
        Perfil perfilAtualizado = gestaoPortfolioService.atualizarPerfil(id, perfil);
        return ResponseEntity.ok(PerfilResponse.fromEntity(perfilAtualizado));
    }

    @PostMapping("/projetos")
    public ResponseEntity<ProjetoResponse> criarProjeto(@Valid @RequestBody ProjetoRequest request) {
        Projeto projeto = Projeto.builder()
                .titulo(request.titulo())
                .descricao(request.descricao())
                .urlCapa(request.urlCapa())
                .linkProducao(request.linkProducao())
                .linkRepositorio(request.linkRepositorio())
                .dataDesenvolvimento(request.dataDesenvolvimento())
                .build();
        Projeto novoProjeto = gestaoPortfolioService.criarProjeto(projeto, request.tecnologiaIds());
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(novoProjeto.getId())
                .toUri();
        return ResponseEntity.created(uri).body(ProjetoResponse.fromEntity(novoProjeto));
    }

    @PutMapping("/projetos/{id}")
    public ResponseEntity<ProjetoResponse> atualizarProjeto(@PathVariable UUID id, @Valid @RequestBody ProjetoRequest request) {
        Projeto projeto = Projeto.builder()
                .titulo(request.titulo())
                .descricao(request.descricao())
                .urlCapa(request.urlCapa())
                .linkProducao(request.linkProducao())
                .linkRepositorio(request.linkRepositorio())
                .dataDesenvolvimento(request.dataDesenvolvimento())
                .build();
        Projeto projetoAtualizado = gestaoPortfolioService.atualizarProjeto(id, projeto, request.tecnologiaIds());
        return ResponseEntity.ok(ProjetoResponse.fromEntity(projetoAtualizado));
    }

    @DeleteMapping("/projetos/{id}")
    public ResponseEntity<Void> excluirProjeto(@PathVariable UUID id) {
        gestaoPortfolioService.excluirProjeto(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/tecnologias")
    public ResponseEntity<TecnologiaResponse> criarTecnologia(@Valid @RequestBody TecnologiaRequest request) {
        Tecnologia tecnologia = Tecnologia.builder()
                .nome(request.nome())
                .urlIcone(request.urlIcone())
                .build();
        Tecnologia novaTecnologia = gestaoPortfolioService.criarTecnologia(tecnologia);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(novaTecnologia.getId())
                .toUri();
        return ResponseEntity.created(uri).body(TecnologiaResponse.fromEntity(novaTecnologia));
    }

    @PostMapping("/projetos/{projetoId}/tecnologias/{tecnologiaId}")
    public ResponseEntity<Void> associarTecnologia(@PathVariable UUID projetoId, @PathVariable UUID tecnologiaId) {
        gestaoPortfolioService.associarTecnologia(projetoId, tecnologiaId);
        return ResponseEntity.noContent().build();
    }
}
