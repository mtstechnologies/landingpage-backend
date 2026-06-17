package com.portfolio.backend.modulo_portfolio.web.dto;

import com.portfolio.backend.modulo_portfolio.domain.Perfil;
import java.util.UUID;

public record PerfilResponse(
        UUID id,
        String nome,
        String titulo,
        String bio,
        String linkLinkedin,
        String linkGithub,
        String urlFoto,
        UUID usuarioId,
        Integer versao
) {
    public static PerfilResponse fromEntity(Perfil perfil) {
        return new PerfilResponse(
                perfil.getId(),
                perfil.getNome(),
                perfil.getTitulo(),
                perfil.getBio(),
                perfil.getLinkLinkedin(),
                perfil.getLinkGithub(),
                perfil.getUrlFoto(),
                perfil.getUsuarioId(),
                perfil.getVersao()
        );
    }
}
