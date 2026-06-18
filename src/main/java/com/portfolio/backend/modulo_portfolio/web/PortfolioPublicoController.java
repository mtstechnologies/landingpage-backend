package com.portfolio.backend.modulo_portfolio.web;

import com.portfolio.backend.modulo_portfolio.application.service.GestaoPortfolioService;
import com.portfolio.backend.modulo_portfolio.web.dto.PerfilResponse;
import com.portfolio.backend.modulo_portfolio.web.dto.ProjetoResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/portfolio")
public class PortfolioPublicoController {

    private final GestaoPortfolioService gestaoPortfolioService;

    public PortfolioPublicoController(GestaoPortfolioService gestaoPortfolioService) {
        this.gestaoPortfolioService = gestaoPortfolioService;
    }

    @GetMapping("/perfil")
    public ResponseEntity<PerfilResponse> obterPerfil() {
        return ResponseEntity.ok(PerfilResponse.fromEntity(gestaoPortfolioService.obterPerfil()));
    }

    @GetMapping("/projetos")
    public ResponseEntity<List<ProjetoResponse>> listarProjetos() {
        List<ProjetoResponse> projetos = gestaoPortfolioService.listarProjetos().stream()
                .map(ProjetoResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(projetos);
    }

    @GetMapping("/projetos/{slug}")
    public ResponseEntity<ProjetoResponse> buscarProjetoPorSlug(
            @PathVariable String slug,
            HttpServletResponse response) {
        ProjetoResponse projeto = gestaoPortfolioService.buscarProjetoPorSlug(slug);
        response.setHeader("Cache-Control", "public, max-age=300");
        return ResponseEntity.ok(projeto);
    }
}
